package pt.tecnico.distledger.server.domain.operation;

import lombok.Getter;
import pt.tecnico.distledger.common.VectorClock;
import pt.tecnico.distledger.server.visitor.OperationVisitor;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;

@Getter
public class TransferOp extends Operation {
    private final String destAccount;
    private final int amount;

    public TransferOp(
            String fromAccount,
            String destAccount,
            int amount,
            VectorClock prevTimestamp,
            VectorClock uniqueTimestamp,
            boolean stable
    ) {
        super(fromAccount, OperationType.OP_TRANSFER_TO, prevTimestamp, uniqueTimestamp, stable);
        this.destAccount = destAccount;
        this.amount = amount;
    }

    @Override
    public void accept(OperationVisitor visitor) {
        visitor.visit(this);
    }
}
