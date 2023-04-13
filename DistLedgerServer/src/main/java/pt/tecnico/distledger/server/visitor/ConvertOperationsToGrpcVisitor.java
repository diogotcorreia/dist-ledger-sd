package pt.tecnico.distledger.server.visitor;

import lombok.Getter;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;

import java.util.ArrayList;
import java.util.List;

public class ConvertOperationsToGrpcVisitor extends OperationVisitor {

    @Getter
    private final List<DistLedgerCommonDefinitions.Operation> ledger = new ArrayList<>();

    @Override
    public void visit(CreateOp operation) {
        ledger.add(
                DistLedgerCommonDefinitions.Operation.newBuilder()
                        .setUserId(operation.getAccount())
                        .setType(operation.getType())
                        .putAllPrevTimestamp(operation.getPrevTimestamp().getTimestamps())
                        .putAllUniqueTimestamp(operation.getUniqueTimestamp().getTimestamps())
                        .setStable(operation.isStable())
                        .build()
        );
    }

    @Override
    public void visit(DeleteOp operation) {
        ledger.add(
                DistLedgerCommonDefinitions.Operation.newBuilder()
                        .setUserId(operation.getAccount())
                        .setType(operation.getType())
                        .build()
        );
    }

    @Override
    public void visit(TransferOp operation) {
        ledger.add(
                DistLedgerCommonDefinitions.Operation.newBuilder()
                        .setUserId(operation.getAccount())
                        .setType(operation.getType())
                        .setDestUserId(operation.getDestAccount())
                        .setAmount(operation.getAmount())
                        .putAllPrevTimestamp(operation.getPrevTimestamp().getTimestamps())
                        .putAllUniqueTimestamp(operation.getUniqueTimestamp().getTimestamps())
                        .setStable(operation.isStable())
                        .build()
        );
    }
}
