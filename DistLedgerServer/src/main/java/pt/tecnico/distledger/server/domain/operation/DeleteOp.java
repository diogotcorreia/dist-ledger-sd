package pt.tecnico.distledger.server.domain.operation;

import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;

public class DeleteOp extends Operation {

    public DeleteOp(String account) {
        super(account, OperationType.OP_DELETE_ACCOUNT);
    }

    @Override
    public DistLedgerCommonDefinitions.Operation toProto() {
        return DistLedgerCommonDefinitions.Operation.newBuilder()
                .setUserId(getAccount())
                .setType(getType())
                .build();
    }

}
