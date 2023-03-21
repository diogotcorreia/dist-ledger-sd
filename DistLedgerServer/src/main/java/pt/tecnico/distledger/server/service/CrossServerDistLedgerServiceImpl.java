package pt.tecnico.distledger.server.service;

import io.grpc.stub.StreamObserver;
import lombok.CustomLog;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateRequest;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateResponse;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase;

@CustomLog(topic = "Cross Server Service")
public class CrossServerDistLedgerServiceImpl extends DistLedgerCrossServerServiceImplBase {

    private final ServerState serverState;

    public CrossServerDistLedgerServiceImpl(ServerState serverState) {
        this.serverState = serverState;
    }

    @Override
    public void propagateState(
            PropagateStateRequest request,
            StreamObserver<PropagateStateResponse> responseObserver
    ) {
        log.debug("Propagate state has been received");

        serverState.setLedger(
                request.getState()
                        .getLedgerList()
                        .stream()
                        .map(this::toOperation)
                        .toList()
        );

        log.debug("Propagate state response has been sent");
        responseObserver.onNext(PropagateStateResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    private Operation toOperation(DistLedgerCommonDefinitions.Operation operation) {
        return switch (operation.getType()) {
            case OP_CREATE_ACCOUNT -> new CreateOp(operation.getUserId());
            case OP_TRANSFER_TO -> new TransferOp(
                    operation.getDestUserId(),
                    operation.getDestUserId(),
                    operation.getAmount()
            );
            case OP_DELETE_ACCOUNT -> new DeleteOp(operation.getUserId());
            default -> throw new IllegalArgumentException("Invalid operation");
        };
    }
}
