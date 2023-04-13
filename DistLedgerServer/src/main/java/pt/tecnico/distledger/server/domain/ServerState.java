package pt.tecnico.distledger.server.domain;

import lombok.CustomLog;
import lombok.Getter;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import pt.tecnico.distledger.common.VectorClock;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.server.exceptions.AccountAlreadyExistsException;
import pt.tecnico.distledger.server.exceptions.AccountNotEmptyException;
import pt.tecnico.distledger.server.exceptions.AccountNotFoundException;
import pt.tecnico.distledger.server.exceptions.AccountProtectedException;
import pt.tecnico.distledger.server.exceptions.InsufficientFundsException;
import pt.tecnico.distledger.server.exceptions.InvalidAmountException;
import pt.tecnico.distledger.server.exceptions.PropagationException;
import pt.tecnico.distledger.server.exceptions.ReadOnlyException;
import pt.tecnico.distledger.server.exceptions.ServerUnavailableException;
import pt.tecnico.distledger.server.exceptions.TransferBetweenSameAccountException;
import pt.tecnico.distledger.server.observer.OperationManager;
import pt.tecnico.distledger.server.visitor.OperationVisitor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Getter
@CustomLog(topic = "Server State")
public class ServerState {

    private static final String BROKER_ID = "broker";
    private final List<Operation> ledger;
    private final Map<String, Account> accounts;
    private final AtomicBoolean active;
    private final String qualifier;
    private final OperationManager operationManager;

    private final VectorClock replicaTimestamp = new VectorClock();
    private final VectorClock valueTimestamp = new VectorClock();
    private final VectorClock gossipTimestamp = new VectorClock();

    @VisibleForTesting
    public ServerState() {
        this("TEST");
    }

    public ServerState(String qualifier) {
        this.ledger = new CopyOnWriteArrayList<>();
        this.accounts = new ConcurrentHashMap<>();
        this.active = new AtomicBoolean(true);
        createBroker();
        this.qualifier = qualifier;
        this.operationManager = new OperationManager(accounts, valueTimestamp, replicaTimestamp);
    }

    public OperationResult<Integer> getBalance(
            String userId,
            VectorClock prevTimestamp
    ) throws AccountNotFoundException, ServerUnavailableException {
        ensureServerIsActive();

        if (!valueTimestamp.isNewerThanOrEqualTo(prevTimestamp)) {
            // TODO: remove this exception and implement the correct behavior
            throw new ServerUnavailableException("TODO");
        }

        return new OperationResult<>(
                getAccount(userId)
                        .orElseThrow(() -> new AccountNotFoundException(userId))
                        .getBalance(),
                valueTimestamp
        );
    }

    public OperationResult<Void> createAccount(
            @NotNull String userId,
            VectorClock prevTimestamp
    ) throws AccountAlreadyExistsException, ServerUnavailableException, PropagationException, ReadOnlyException {
        ensureServerIsActive();

        synchronized (accounts) {
            // After the lock is granted, we need to re-check the server state
            ensureServerIsActive();

            if (accounts.containsKey(userId)) {
                throw new AccountAlreadyExistsException(userId);
            }

            replicaTimestamp.incrementClock(qualifier);
            VectorClock uniqueTimestamp = prevTimestamp.clone();
            uniqueTimestamp.setValue(qualifier, replicaTimestamp.getValue(qualifier));

            CreateOp pendingOperation = new CreateOp(userId, prevTimestamp, uniqueTimestamp, false);
            synchronized (ledger) {
                ledger.add(pendingOperation);
            }

            operationManager.registerObserver(pendingOperation);
            operationManager.notifyObservers();

            log.debug("Replica's current timestamp: %s", replicaTimestamp);
            return new OperationResult<>(null, uniqueTimestamp);
        }
    }

    public void deleteAccount(
            @NotNull String userId
    ) throws AccountNotEmptyException, AccountNotFoundException, AccountProtectedException, ServerUnavailableException, PropagationException, ReadOnlyException {
        ensureServerIsActive();

        Account account = null;
        try {
            account = getThreadSafeAccount(userId)
                    .orElseThrow(() -> new AccountNotFoundException(userId));

            // After the lock is granted, we need to re-check the server state
            ensureServerIsActive();

            if (userId.equals(BROKER_ID)) {
                throw new AccountProtectedException(userId);
            }

            final int balance = account.getBalance();
            if (balance != 0) {
                throw new AccountNotEmptyException(userId, balance);
            }

            DeleteOp pendingOperation = new DeleteOp(userId);
            synchronized (ledger) {
                ledger.add(pendingOperation);
            }

            accounts.remove(userId);
        } finally {
            if (account != null) {
                account.getLock().unlock();
            }
        }
    }

    public OperationResult<Void> transferTo(
            @NotNull String fromUserId,
            @NotNull String toUserId,
            int amount,
            VectorClock prevTimestamp
    ) throws AccountNotFoundException, InsufficientFundsException, ServerUnavailableException, InvalidAmountException, TransferBetweenSameAccountException, PropagationException, ReadOnlyException {
        ensureServerIsActive();

        if (fromUserId.equals(toUserId)) {
            throw new TransferBetweenSameAccountException(fromUserId, toUserId);
        }

        /* In order to avoid deadlocks, such as the case below, a standardized order must be defined for lock retrievals.
         *
         * Thread A: Transfer Request from dtc to dmg
         * Thread B: Transfer Request from dmg to dtc
         *
         * This leads to a deadlock (in naive implementations), as A would get dtc's lock, B would get dmg's lock, and then
         * none of them would be able to get the other's lock.
         *
         * The chosen order is rather simple: the locks are retrieved in ascending order of the user ids.
         */

        List<String> users = Stream.of(fromUserId, toUserId).sorted().toList();

        Account fromAccount = null;
        Account toAccount = null;

        VectorClock uniqueTimestamp;
        try {
            final String firstUser = users.get(0);
            final String secondUser = users.get(1);
            final Account firstAccount = getThreadSafeAccount(firstUser)
                    .orElseThrow(() -> new AccountNotFoundException(firstUser));
            final Account secondAccount = getThreadSafeAccount(secondUser)
                    .orElseThrow(() -> new AccountNotFoundException(secondUser));

            if (firstUser.equals(fromUserId)) {
                fromAccount = firstAccount;
                toAccount = secondAccount;
            } else {
                fromAccount = secondAccount;
                toAccount = firstAccount;
            }

            // After the locks are granted, we need to re-check the server state
            ensureServerIsActive();

            if (amount <= 0) {
                throw new InvalidAmountException(amount);
            }

            if (fromAccount.getBalance() < amount) {
                throw new InsufficientFundsException(fromUserId, amount, fromAccount.getBalance());
            }

            replicaTimestamp.incrementClock(qualifier);
            uniqueTimestamp = prevTimestamp.clone();
            uniqueTimestamp.setValue(qualifier, replicaTimestamp.getValue(qualifier));

            TransferOp pendingOperation =
                    new TransferOp(fromUserId, toUserId, amount, prevTimestamp, uniqueTimestamp, false);
            synchronized (ledger) {
                ledger.add(pendingOperation);
            }

            operationManager.registerObserver(pendingOperation);
            operationManager.notifyObservers();
        } finally {
            if (fromAccount != null) {
                fromAccount.getLock().unlock();
            }
            if (toAccount != null) {
                toAccount.getLock().unlock();
            }
        }
        log.debug("Replica's current timestamp: %s", replicaTimestamp);
        return new OperationResult<>(null, uniqueTimestamp);
    }

    public void activate() {
        this.active.set(true);
    }

    public void deactivate() {
        this.active.set(false);
    }

    public List<Operation> getOperations(String qualifier) {
        final int lastTimestamp = gossipTimestamp.getValue(qualifier);

        // TODO: check condition
        return ledger.stream()
                .filter(operation -> operation.getUniqueTimestamp().getValue(qualifier) >= lastTimestamp)
                .toList();
    }

    public void updateGossipTimestamp(String qualifier) {
        gossipTimestamp.setValue(qualifier, replicaTimestamp.getValue(qualifier));
    }

    public void operateOverLedger(OperationVisitor visitor) {
        ledger.forEach(operation -> operation.accept(visitor));
    }

    public synchronized void addToLedger(List<Operation> newOperations) throws ServerUnavailableException {
        ensureServerIsActive();
        newOperations.forEach(operation -> {
            if (!replicaTimestamp.isNewerThanOrEqualTo(operation.getUniqueTimestamp())) {
                replicaTimestamp.updateVectorClock(operation.getUniqueTimestamp());
                operationManager.registerObserver(operation);
                synchronized (ledger) {
                    ledger.add(operation);
                }
            }
        });
        operationManager.notifyObservers();
    }

    /**
     * Get an account by its ID.
     *
     * @param userId The ID of the account to get.
     * @return An optional with the Account, or an empty optional if the account cannot be found.
     */
    private Optional<Account> getAccount(String userId) {
        return Optional.ofNullable(accounts.get(userId));
    }

    /**
     * Same as {@link ServerState#getAccount(String)}, but locks the {@link Account} object.
     * <p>
     * IMPORTANT: The caller is responsible for releasing the lock.
     *
     * @param userId The ID of the account to get.
     * @return An optional with the Account, or an empty optional if the account cannot be found.
     * @see ServerState#getAccount(String)
     */
    private Optional<Account> getThreadSafeAccount(String userId) {
        val accountOpt = getAccount(userId);
        if (accountOpt.isPresent()) {
            // Obtain lock
            accountOpt.get().getLock().lock();

            // Re-check if the account still exists
            if (accounts.get(userId) != accountOpt.get()) {
                // If it doesn't, unlock it (so if any thread is also waiting, it can know the account has been deleted)
                accountOpt.get().getLock().unlock();
                return Optional.empty();
            }
        }
        return accountOpt;
    }

    private void createBroker() {
        Account broker = new Account(BROKER_ID);
        broker.increaseBalance(1000);
        accounts.put(BROKER_ID, broker);
    }

    private void ensureServerIsActive() throws ServerUnavailableException {
        if (!active.get()) {
            throw new ServerUnavailableException("Server is not active");
        }
    }
}
