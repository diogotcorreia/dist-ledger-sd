package pt.tecnico.distledger.server.domain.operation;

import lombok.Getter;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;

@Getter
public class Operation {
    private String account;
    private DistLedgerCommonDefinitions.OperationType type;

    public Operation(String fromAccount, DistLedgerCommonDefinitions.OperationType type) {
        this.account = fromAccount;
        this.type = type;
    }

}
