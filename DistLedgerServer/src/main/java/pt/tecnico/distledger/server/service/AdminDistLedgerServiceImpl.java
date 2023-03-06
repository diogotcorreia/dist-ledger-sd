package pt.tecnico.distledger.server.service;

import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.Operation;
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
        serverState.activate();
        responseObserver.onNext(ActivateResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void deactivate(
            DeactivateRequest request,
            StreamObserver<DeactivateResponse> responseObserver
    ) {
        serverState.deactivate();
        responseObserver.onNext(DeactivateResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void gossip(
            GossipRequest request,
            StreamObserver<GossipResponse> responseObserver
    ) {
        serverState.gossip();
        responseObserver.onNext(GossipResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void getLedgerState(
            GetLedgerStateRequest request,
            StreamObserver<GetLedgerStateResponse> responseObserver
    ) {
        final LedgerState ledgerState = LedgerState.newBuilder()
                .addAllLedger(serverState.getLedgerStream().map(Operation::toProto).toList())
                .build();

        responseObserver.onNext(
                GetLedgerStateResponse.newBuilder()
                        .setLedgerState(ledgerState)
                        .build()
        );

        responseObserver.onCompleted();
    }
}
