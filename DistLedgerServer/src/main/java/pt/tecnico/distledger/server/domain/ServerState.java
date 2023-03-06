package pt.tecnico.distledger.server.domain;

import lombok.Getter;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.server.exceptions.AccountAlreadyExistsException;
import pt.tecnico.distledger.server.exceptions.AccountNotFoundException;
import pt.tecnico.distledger.server.exceptions.CannotRemoveProtectedAccountException;
import pt.tecnico.distledger.server.exceptions.InsufficientFundsException;
import pt.tecnico.distledger.server.exceptions.ServerUnavailableException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

@Getter
public class ServerState {

    private static final String BROKER_ID = "broker";
    private final List<Operation> ledger;
    private final Map<String, Account> accounts;
    private boolean active;

    public ServerState() {
        this.ledger = new CopyOnWriteArrayList<>();
        this.accounts = new ConcurrentHashMap<>();
        this.active = true;
        createBroker();
    }

    public int getBalance(String userId) throws AccountNotFoundException, ServerUnavailableException {
        ensureServerIsActive();
        return getAccount(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId))
                .getBalance();
    }

    public void createAccount(String userId) throws AccountAlreadyExistsException, ServerUnavailableException {
        ensureServerIsActive();
        if (accounts.containsKey(userId)) {
            throw new AccountAlreadyExistsException(userId);
        }
        accounts.put(userId, new Account(userId));
        ledger.add(new CreateOp(userId));
    }

    public void deleteAccount(String userId) throws AccountNotFoundException, CannotRemoveProtectedAccountException, ServerUnavailableException {
        ensureServerIsActive();
        if (accounts.remove(userId) == null) {
            throw new AccountNotFoundException(userId);
        }
        if (userId.equals(BROKER_ID)) {
            throw new CannotRemoveProtectedAccountException(userId);
        }

        ledger.add(new DeleteOp(userId));
    }

    public void transferTo(
            String fromUserId,
            String toUserId,
            int amount
    ) throws AccountNotFoundException, InsufficientFundsException, ServerUnavailableException {
        ensureServerIsActive();
        final Account fromAccount = getAccount(fromUserId).orElseThrow(() -> new AccountNotFoundException(fromUserId));
        final Account toAccount = getAccount(toUserId).orElseThrow(() -> new AccountNotFoundException(toUserId));

        if (fromAccount.getBalance() < amount) {
            throw new InsufficientFundsException(fromUserId, amount, fromAccount.getBalance());
        }
        fromAccount.decreaseBalance(amount);
        toAccount.increaseBalance(amount);
        ledger.add(new TransferOp(fromUserId, toUserId, amount));
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void gossip() {
        // TODO
    }

    public Stream<Operation> getLedgerStream() {
        return ledger.stream();
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

    private void createBroker() {
        Account broker = new Account(BROKER_ID);
        broker.increaseBalance(1000);
        accounts.put(BROKER_ID, broker);
    }

    private void ensureServerIsActive() throws ServerUnavailableException {
        if (!active) {
            throw new ServerUnavailableException("Server is not active");
        }
    }
}
