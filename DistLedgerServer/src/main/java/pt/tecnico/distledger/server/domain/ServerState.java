package pt.tecnico.distledger.server.domain;

import lombok.Getter;
import lombok.val;
import org.jetbrains.annotations.VisibleForTesting;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Getter
public class ServerState {

    private static final String BROKER_ID = "broker";
    private final List<Operation> ledger;
    private final Map<String, Account> accounts;
    private final AtomicBoolean active;
    private final boolean isPrimary;
    private final Consumer<Operation> writeOperationCallback;

    @VisibleForTesting
    public ServerState() {
        this(true, op -> {
        });
    }

    public ServerState(boolean isPrimary, Consumer<Operation> writeOperationCallback) {
        this.ledger = new CopyOnWriteArrayList<>();
        this.accounts = new ConcurrentHashMap<>();
        this.active = new AtomicBoolean(true);
        createBroker();
        this.isPrimary = isPrimary;
        this.writeOperationCallback = writeOperationCallback;
    }

    public synchronized int getBalance(String userId) throws AccountNotFoundException, ServerUnavailableException {
        ensureServerIsActive();
        return getAccount(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId))
                .getBalance();
    }

    public synchronized void createAccount(
            String userId
    ) throws AccountAlreadyExistsException, ServerUnavailableException, PropagationException, ReadOnlyException {
        ensureServerIsActive();
        ensureServerIsPrimary();
        if (accounts.containsKey(userId)) {
            throw new AccountAlreadyExistsException(userId);
        }

        val pendingOperation = new CreateOp(userId);
        propagateOperation(pendingOperation);

        accounts.put(userId, new Account(userId));
        ledger.add(pendingOperation);
    }

    public synchronized void deleteAccount(
            String userId
    ) throws AccountNotEmptyException, AccountNotFoundException, AccountProtectedException, ServerUnavailableException, PropagationException, ReadOnlyException {
        ensureServerIsActive();
        ensureServerIsPrimary();
        final int balance = getAccount(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId))
                .getBalance();
        if (userId.equals(BROKER_ID)) {
            throw new AccountProtectedException(userId);
        }
        if (balance != 0) {
            throw new AccountNotEmptyException(userId, balance);
        }

        val pendingOperation = new DeleteOp(userId);
        propagateOperation(pendingOperation);

        accounts.remove(userId);
        ledger.add(pendingOperation);
    }

    public synchronized void transferTo(
            String fromUserId,
            String toUserId,
            int amount
    ) throws AccountNotFoundException, InsufficientFundsException, ServerUnavailableException, InvalidAmountException, TransferBetweenSameAccountException, PropagationException, ReadOnlyException {
        ensureServerIsActive();
        ensureServerIsPrimary();
        final Account fromAccount = getAccount(fromUserId).orElseThrow(() -> new AccountNotFoundException(fromUserId));
        final Account toAccount = getAccount(toUserId).orElseThrow(() -> new AccountNotFoundException(toUserId));

        if (fromAccount.equals(toAccount)) {
            throw new TransferBetweenSameAccountException(fromAccount.getUserId(), toAccount.getUserId());
        }

        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }

        if (fromAccount.getBalance() < amount) {
            throw new InsufficientFundsException(fromUserId, amount, fromAccount.getBalance());
        }
        val pendingOperation = new TransferOp(fromUserId, toUserId, amount);
        propagateOperation(pendingOperation);

        fromAccount.decreaseBalance(amount);
        toAccount.increaseBalance(amount);
        ledger.add(pendingOperation);
    }

    public void activate() {
        this.active.set(true);
    }

    public void deactivate() {
        this.active.set(false);
    }

    public void gossip() {
        // TODO
    }

    public synchronized void operateOverLedger(OperationVisitor visitor) {
        ledger.forEach(operation -> operation.accept(visitor));
    }

    public synchronized void setLedger(List<Operation> newOperations) throws ServerUnavailableException {
        ensureServerIsActive();
        this.ledger.addAll(newOperations);
        updateAccountsFromLedger(newOperations);
    }

    public synchronized void updateAccountsFromLedger(List<Operation> newOperations) {
        val visitor = new ExecuteOperationVisitor(this.accounts);
        newOperations.forEach(operation -> operation.accept(visitor));
    }

    /**
     * Get an account by its ID.
     *
     * @param userId The ID of the account to get.
     * @return An optional with the Account, or an empty optional if the account cannot be found.
     */
    private synchronized Optional<Account> getAccount(String userId) {
        return Optional.ofNullable(accounts.get(userId));
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

    public void ensureServerIsPrimary() throws ReadOnlyException {
        if (!isPrimary) {
            throw new ReadOnlyException();
        }
    }

    public void propagateOperation(Operation operation) throws PropagationException {
        try {
            writeOperationCallback.accept(operation); // may fail
        } catch (RuntimeException e) {
            if (e.getCause()instanceof PropagationException e2) {
                throw e2;
            }
            throw e;
        }

    }
}
