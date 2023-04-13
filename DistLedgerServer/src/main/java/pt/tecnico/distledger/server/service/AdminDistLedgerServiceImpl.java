package pt.tecnico.distledger.server.service;

import io.grpc.stub.StreamObserver;
import lombok.CustomLog;
import pt.tecnico.distledger.server.ServerCoordinator;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.ServerUnavailableException;
import pt.tecnico.distledger.server.visitor.ConvertOperationsToGrpcVisitor;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.ActivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.ActivateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.GetLedgerStateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.GetLedgerStateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.GossipRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.GossipResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;

@CustomLog(topic = "Admin Service")
public class AdminDistLedgerServiceImpl extends AdminServiceGrpc.AdminServiceImplBase {

    private final ServerState serverState;
    private final ServerCoordinator serverCoordinator;

    public AdminDistLedgerServiceImpl(ServerCoordinator serverCoordinator) {
        this.serverCoordinator = serverCoordinator;
        this.serverState = serverCoordinator.getServerState();
    }

    @Override
    public void activate(
            ActivateRequest request,
            StreamObserver<ActivateResponse> responseObserver
    ) {
        serverState.activate();
        log.debug("Server has been activated");
        responseObserver.onNext(ActivateResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void deactivate(
            DeactivateRequest request,
            StreamObserver<DeactivateResponse> responseObserver
    ) {
        serverState.deactivate();
        log.debug("Server has been deactivated");
        responseObserver.onNext(DeactivateResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void gossip(
            GossipRequest request,
            StreamObserver<GossipResponse> responseObserver
    ) {
        try {
            String serverTo = request.getQualifier();
            serverCoordinator.propagateUsingGossip(serverTo);
            responseObserver.onNext(GossipResponse.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (ServerUnavailableException e) {
            log.error("Server %s is currently unavailable", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getLedgerState(
            GetLedgerStateRequest request,
            StreamObserver<GetLedgerStateResponse> responseObserver
    ) {
        log.debug("Ledger state has been requested");
        final ConvertOperationsToGrpcVisitor visitor = new ConvertOperationsToGrpcVisitor();
        serverState.operateOverLedger(visitor);

        final LedgerState ledgerState = LedgerState.newBuilder()
                .addAllLedger(visitor.getLedger())
                .build();

        responseObserver.onNext(
                GetLedgerStateResponse.newBuilder()
                        .setLedgerState(ledgerState)
                        .build()
        );

        responseObserver.onCompleted();
    }
}
