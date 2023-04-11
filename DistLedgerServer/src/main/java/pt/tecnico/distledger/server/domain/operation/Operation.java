package pt.tecnico.distledger.server.domain.operation;

import lombok.Getter;
import lombok.ToString;
import pt.tecnico.distledger.common.VectorClock;
import pt.tecnico.distledger.server.observer.Observer;
import pt.tecnico.distledger.server.visitor.OperationVisitor;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;

@Getter
@ToString
public abstract class Operation implements Observer {
    private final String account;
    private final OperationType type;
    private final VectorClock prevTimestamp;
    private final VectorClock uniqueTimestamp;

    protected Operation(
            String fromAccount,
            OperationType type,
            VectorClock prevTimestamp,
            VectorClock uniqueTimestamp
    ) {
        this.account = fromAccount;
        this.type = type;
        this.prevTimestamp = prevTimestamp;
        this.uniqueTimestamp = uniqueTimestamp;
    }

    public abstract void accept(OperationVisitor visitor);

    public boolean update(OperationVisitor visitor, VectorClock valueTimestamp) {
        if (valueTimestamp.isNewerThanOrEqualTo(this.getPrevTimestamp())) {
            accept(visitor);
            return true;
        }
        return false;
    }
}
