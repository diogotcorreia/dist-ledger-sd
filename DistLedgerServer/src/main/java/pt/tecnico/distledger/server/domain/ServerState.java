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
import pt.tecnico.distledger.server.visitor.ExecuteOperationVisitor;
import pt.tecnico.distledger.server.visitor.OperationVisitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Getter
@CustomLog(topic = "Server State")
public class ServerState {

    private static final String BROKER_ID = "broker";
    private final Ledger ledger;
    private final Map<String, Account> accounts;
    private final AtomicBoolean active;
    private final String qualifier;

    private final VectorClock replicaTimestamp = new VectorClock();
    private final VectorClock valueTimestamp = new VectorClock();
    private final Map<String, VectorClock> gossipTimestampMap = new HashMap<>();

    @VisibleForTesting
    public ServerState() {
        this("TEST");
    }

    public ServerState(String qualifier) {
        this.accounts = new ConcurrentHashMap<>();
        this.ledger = new Ledger(this::canOperationBeStable, this::executeOperation);
        this.active = new AtomicBoolean(true);
        createBroker();
        this.qualifier = qualifier;
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
            ledger.addUnstable(pendingOperation);

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
            ledger.addUnstable(pendingOperation);

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
            ledger.addUnstable(pendingOperation);
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

    /**
     * Get operations to be sent to another replica with the given qualifier.
     *
     * @param visitor   The visitor to be called with every operation to be sent to the replica.
     * @param qualifier The qualifier of the replica to send operations to.
     */
    public void operateOverLedgerToPropagateToReplica(OperationVisitor visitor, String qualifier) {
        final VectorClock lastTimestamp = gossipTimestampMap.getOrDefault(qualifier, new VectorClock());

        ledger.operateOverLedger(
                visitor,
                operation -> !lastTimestamp.isNewerThanOrEqualTo(operation.getUniqueTimestamp())
        );
    }

    /**
     * Save the timestamp of the last operation propagated to this replica.
     *
     * @param qualifier The qualifier of the replica to save this timestamp of.
     * @param timestamp The timestamp to save.
     */
    public void updateGossipTimestamp(String qualifier, VectorClock timestamp) {
        gossipTimestampMap.put(qualifier, timestamp);
    }

    public void operateOverLedger(OperationVisitor visitor) {
        ledger.operateOverLedger(visitor);
    }

    public synchronized void addToLedger(List<Operation> newOperations) throws ServerUnavailableException {
        ensureServerIsActive();
        newOperations.forEach(operation -> replicaTimestamp.updateVectorClock(operation.getUniqueTimestamp()));
        ledger.addAllUnstable(newOperations);
    }

    private boolean canOperationBeStable(Operation operation) {
        return this.valueTimestamp.isNewerThanOrEqualTo(operation.getPrevTimestamp());
    }

    private void executeOperation(Operation operation) {
        operation.accept(new ExecuteOperationVisitor(this.accounts));
        this.valueTimestamp.updateVectorClock(operation.getUniqueTimestamp());
        log.debug("Value's timestamp: %s", valueTimestamp);
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
            throw new ServerUnavailableException(qualifier);
        }
    }
}
