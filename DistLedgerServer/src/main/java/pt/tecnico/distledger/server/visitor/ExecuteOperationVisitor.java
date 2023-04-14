package pt.tecnico.distledger.server.visitor;

import pt.tecnico.distledger.server.domain.Account;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;

import java.util.Map;

public class ExecuteOperationVisitor extends OperationVisitor {

    private final Map<String, Account> accounts;

    public ExecuteOperationVisitor(Map<String, Account> accounts) {
        this.accounts = accounts;
    }

    @Override
    public void visit(CreateOp operation) {
        accounts.put(
                operation.getAccount(),
                new Account(operation.getAccount())
        );
    }

    @Override
    public void visit(TransferOp operation) {
        Account from = accounts.get(operation.getAccount());
        Account to = accounts.get(operation.getDestAccount());
        from.decreaseBalance(operation.getAmount());
        to.increaseBalance(operation.getAmount());
    }

}
