package pt.tecnico.distledger.server.service;

import io.grpc.stub.StreamObserver;
import lombok.CustomLog;
import pt.tecnico.distledger.common.VectorClock;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.server.exceptions.ServerUnavailableException;
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
        try {
            serverState.addToLedger(
                    request.getState()
                            .getLedgerList()
                            .stream()
                            .map(this::toOperation)
                            .toList()
            );

            log.debug("Propagate state response has been sent");
            responseObserver.onNext(PropagateStateResponse.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (ServerUnavailableException e) {
            log.debug("Error: %s", e.getMessage());
            responseObserver.onError(e.toGrpcRuntimeException());
        }
    }

    private Operation toOperation(DistLedgerCommonDefinitions.Operation operation) {
        return switch (operation.getType()) {
            case OP_CREATE_ACCOUNT -> new CreateOp(
                    operation.getUserId(),
                    new VectorClock(operation.getPrevTimestampMap()),
                    new VectorClock(operation.getUniqueTimestampMap())
            );
            case OP_TRANSFER_TO -> new TransferOp(
                    operation.getUserId(),
                    operation.getDestUserId(),
                    operation.getAmount(),
                    new VectorClock(operation.getPrevTimestampMap()),
                    new VectorClock(operation.getUniqueTimestampMap())
            );
            case OP_DELETE_ACCOUNT -> new DeleteOp(operation.getUserId());
            default -> throw new IllegalArgumentException("Invalid operation");
        };
    }
}
