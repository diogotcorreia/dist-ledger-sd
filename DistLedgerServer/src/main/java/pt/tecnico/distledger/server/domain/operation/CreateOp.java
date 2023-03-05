package pt.tecnico.distledger.server.domain.operation;

import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.*;

public class CreateOp extends Operation {

    public CreateOp(String account) {
        super(account, OperationType.OP_CREATE_ACCOUNT);
    }

}
