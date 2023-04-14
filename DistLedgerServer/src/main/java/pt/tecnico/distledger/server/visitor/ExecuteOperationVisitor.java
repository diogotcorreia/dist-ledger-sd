package pt.tecnico.distledger.server.visitor;

import pt.tecnico.distledger.server.domain.Account;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;

import java.util.Map;
import java.util.Objects;

public class ExecuteOperationVisitor extends OperationVisitor {

    private final Map<String, Account> accounts;

    public ExecuteOperationVisitor(Map<String, Account> accounts) {
        this.accounts = accounts;
    }

    @Override
    public void visit(CreateOp operation) {
        accounts.putIfAbsent(
                operation.getAccount(),
                new Account(operation.getAccount())
        );
    }

    @Override
    public void visit(TransferOp operation) {
        if (Objects.equals(operation.getAccount(), operation.getDestAccount())) {
            return;
        }

        Account from = accounts.get(operation.getAccount());
        Account to = accounts.get(operation.getDestAccount());

        if (from == null || to == null) {
            return;
        }

        if (operation.getAmount() <= 0 || from.getBalance() < operation.getAmount()) {
            return;
        }

        from.decreaseBalance(operation.getAmount());
        to.increaseBalance(operation.getAmount());
    }

}
