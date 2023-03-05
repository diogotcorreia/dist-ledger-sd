package pt.tecnico.distledger.server.domain;

import lombok.Getter;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.server.exceptions.AccountAlreadyExistsException;
import pt.tecnico.distledger.server.exceptions.AccountNotFoundException;
import pt.tecnico.distledger.server.exceptions.InsufficientFundsException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class ServerState {
    private final List<Operation> ledger;
    private final Map<String, Account> accounts;
    private boolean active;

    public ServerState() {
        this.ledger = new CopyOnWriteArrayList<>();
        this.accounts = new ConcurrentHashMap<>();
        this.active = true;
    }

    public int getBalance(String userId) throws AccountNotFoundException {
        return getAccount(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId))
                .getBalance();
    }

    public void createAccount(String userId) throws AccountAlreadyExistsException {
        if (accounts.containsKey(userId)) {
            throw new AccountAlreadyExistsException(userId);
        }
        accounts.put(userId, new Account(userId));
        ledger.add(new CreateOp(userId));
    }

    public void deleteAccount(String userId) throws AccountNotFoundException {
        if (accounts.remove(userId) == null) {
            throw new AccountNotFoundException(userId);
        }
        ledger.add(new DeleteOp(userId));
    }

    public void transferTo(
            String fromUserId,
            String toUserId,
            int amount
    ) throws AccountNotFoundException, InsufficientFundsException {
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

    /**
     * Get an account by its ID.
     *
     * @param userId The ID of the account to get.
     * @return An optional with the Account, or an empty optional if the account cannot be found.
     */
    private Optional<Account> getAccount(String userId) {
        return Optional.ofNullable(accounts.get(userId));
    }

}
