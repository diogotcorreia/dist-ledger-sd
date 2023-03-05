package pt.tecnico.distledger.server.domain;

import lombok.Getter;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.server.exceptions.DistLedgerExceptions;
import pt.tecnico.distledger.server.exceptions.ErrorMessage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class ServerState {
    private List<Operation> ledger;
    private Map<String, Account> accounts;
    private boolean active;

    public ServerState() {
        this.ledger = new CopyOnWriteArrayList<>();
        this.accounts = new ConcurrentHashMap<>();
        this.active = true;
    }

    public int getBalance(String userId) {
        if (!accounts.containsKey(userId)) {
            throw new DistLedgerExceptions(ErrorMessage.ACCOUNT_NOT_FOUND);
        }
        return accounts.get(userId).getBalance();
    }

    public void createAccount(String userId) {
        if (accounts.containsKey(userId)) {
            throw new DistLedgerExceptions(ErrorMessage.ACCOUNT_ALREADY_EXISTS);
        }
        accounts.put(userId, new Account(userId));
        ledger.add(new CreateOp(userId));
    }

    public void deleteAccount(String userId) {
        if (!accounts.containsKey(userId)) {
            throw new DistLedgerExceptions(ErrorMessage.ACCOUNT_NOT_FOUND);
        }
        accounts.remove(userId);
        ledger.add(new DeleteOp(userId));
    }

    public void transferTo(String fromUserId, String toUserId, int amount) {
        if (!accounts.containsKey(fromUserId)) {
            throw new DistLedgerExceptions(ErrorMessage.ACCOUNT_NOT_FOUND);
        }
        if (!accounts.containsKey(toUserId)) {
            throw new DistLedgerExceptions(ErrorMessage.ACCOUNT_NOT_FOUND);
        }
        if (accounts.get(fromUserId).getBalance() < amount) {
            throw new DistLedgerExceptions(ErrorMessage.INSUFFICIENT_AMOUNT);
        }
        accounts.get(fromUserId).decreaseBalance(amount);
        accounts.get(toUserId).increaseBalance(amount);
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

}
