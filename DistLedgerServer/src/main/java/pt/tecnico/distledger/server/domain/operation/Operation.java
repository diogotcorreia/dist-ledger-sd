package pt.tecnico.distledger.server.domain.operation;

import lombok.Getter;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;

@Getter
public class Operation {
    private final String account;
    private final OperationType type;

    public Operation(String fromAccount, OperationType type) {
        this.account = fromAccount;
        this.type = type;
    }

}
