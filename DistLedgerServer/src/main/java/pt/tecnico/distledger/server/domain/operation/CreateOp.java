package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.common.VectorClock;
import pt.tecnico.distledger.server.visitor.OperationVisitor;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;

public class CreateOp extends Operation {

    public CreateOp(String account, VectorClock prevTimestamp, VectorClock uniqueTimestamp, boolean stable) {
        super(account, OperationType.OP_CREATE_ACCOUNT, prevTimestamp, uniqueTimestamp, stable);
    }

    @Override
    public void accept(OperationVisitor visitor) {
        visitor.visit(this);
    }
}
