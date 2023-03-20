package pt.tecnico.distledger.server.domain.operation;

import lombok.Getter;
import pt.tecnico.distledger.server.visitor.OperationVisitor;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;

@Getter
public abstract class Operation {
    private final String account;
    private final OperationType type;

    protected Operation(String fromAccount, OperationType type) {
        this.account = fromAccount;
        this.type = type;
    }

    public abstract void accept(OperationVisitor visitor);

}
