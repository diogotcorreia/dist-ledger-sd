package pt.tecnico.distledger.server.domain.operation;

import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;

public class DeleteOp extends Operation {

    public DeleteOp(String account) {
        super(account, DistLedgerCommonDefinitions.OperationType.OP_DELETE_ACCOUNT);
    }

}
