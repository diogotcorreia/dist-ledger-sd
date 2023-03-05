package pt.tecnico.distledger.server.domain.operation;

import lombok.EqualsAndHashCode;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;

@EqualsAndHashCode(callSuper = false)
public class CreateOp extends Operation {

    public CreateOp(String account) {
        super(account, OperationType.OP_CREATE_ACCOUNT);
    }

}
