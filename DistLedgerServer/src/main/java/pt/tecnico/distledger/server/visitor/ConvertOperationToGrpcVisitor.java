package pt.tecnico.distledger.server.visitor;

import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;

public class ConvertOperationToGrpcVisitor extends OperationVisitor<DistLedgerCommonDefinitions.Operation> {

    @Override
    public DistLedgerCommonDefinitions.Operation visit(CreateOp operation) {
        return DistLedgerCommonDefinitions.Operation.newBuilder()
                .setUserId(operation.getAccount())
                .setType(operation.getType())
                .build();
    }

    @Override
    public DistLedgerCommonDefinitions.Operation visit(DeleteOp operation) {
        return DistLedgerCommonDefinitions.Operation.newBuilder()
                .setUserId(operation.getAccount())
                .setType(operation.getType())
                .build();
    }

    @Override
    public DistLedgerCommonDefinitions.Operation visit(TransferOp operation) {
        return DistLedgerCommonDefinitions.Operation.newBuilder()
                .setUserId(operation.getAccount())
                .setType(operation.getType())
                .setDestUserId(operation.getDestAccount())
                .setAmount(operation.getAmount())
                .build();
    }
}
