package pt.tecnico.distledger.server.domain;

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

public class ServerState {
    private List<Operation> ledger;

    private Map<String, Account> accounts;

    public ServerState() {
        this.ledger = new CopyOnWriteArrayList<>();
        this.accounts = new ConcurrentHashMap<>();
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
        accounts.get(fromUserId).removeBalance(amount);
        accounts.get(toUserId).addBalance(amount);
        ledger.add(new TransferOp(fromUserId, toUserId, amount));
    }


}
