package pt.tecnico.distledger.server.domain.operation;

import lombok.Getter;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;

@Getter
public class TransferOp extends Operation {
    private String destAccount;
    private int amount;

    public TransferOp(String fromAccount, String destAccount, int amount) {
        super(fromAccount, DistLedgerCommonDefinitions.OperationType.OP_TRANSFER_TO);
        this.destAccount = destAccount;
        this.amount = amount;
    }

}
