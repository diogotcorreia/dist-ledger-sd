package pt.tecnico.distledger.server.domain.operation;

import lombok.EqualsAndHashCode;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;

@EqualsAndHashCode(callSuper = false)
public class DeleteOp extends Operation {

    public DeleteOp(String account) {
        super(account, OperationType.OP_DELETE_ACCOUNT);
    }

}
