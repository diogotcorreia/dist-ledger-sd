package pt.tecnico.distledger.server.service;

import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.DistLedgerExceptions;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;

import static io.grpc.Status.INVALID_ARGUMENT;

public class AdminDistLedgerServiceImpl extends AdminServiceGrpc.AdminServiceImplBase {

    private final ServerState serverState;

    public AdminDistLedgerServiceImpl(ServerState serverState) {
        this.serverState = serverState;
    }

    @Override
    public void activate(
            ActivateRequest request,
            StreamObserver<ActivateResponse> responseObserver
    ) {
        try {
            serverState.activate();
            responseObserver.onCompleted();
        } catch (DistLedgerExceptions e) {
            responseObserver.onError(
                    INVALID_ARGUMENT.withDescription(e.getErrorMessage().message).asRuntimeException()
            );
        }
    }

    @Override
    public void deactivate(
            DeactivateRequest request,
            StreamObserver<DeactivateResponse> responseObserver
    ) {
        try {
            serverState.deactivate();
            responseObserver.onCompleted();
        } catch (DistLedgerExceptions e) {
            responseObserver.onError(
                    INVALID_ARGUMENT.withDescription(e.getErrorMessage().message).asRuntimeException()
            );
        }
    }

    @Override
    public void gossip(
            GossipRequest request,
            StreamObserver<GossipResponse> responseObserver
    ) {
        try {
            serverState.gossip();
            responseObserver.onCompleted();
        } catch (DistLedgerExceptions e) {
            responseObserver.onError(
                    INVALID_ARGUMENT.withDescription(e.getErrorMessage().message).asRuntimeException()
            );
        }
    }

    // TODO: add getLedgerState method

}
