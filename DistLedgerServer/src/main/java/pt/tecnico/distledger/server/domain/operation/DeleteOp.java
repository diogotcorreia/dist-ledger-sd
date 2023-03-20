package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.visitor.OperationVisitor;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;

public class DeleteOp extends Operation {

    public DeleteOp(String account) {
        super(account, OperationType.OP_DELETE_ACCOUNT);
    }

    @Override
    public void accept(OperationVisitor visitor) {
        visitor.visit(this);
    }
}
