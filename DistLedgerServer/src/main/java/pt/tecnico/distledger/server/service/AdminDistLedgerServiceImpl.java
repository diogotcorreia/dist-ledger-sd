package pt.tecnico.distledger.server.service;

import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.ActivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.ActivateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateResponse;
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
        responseObserver.onCompleted();
    }

    @Override
    public void deactivate(
            DeactivateRequest request,
            StreamObserver<DeactivateResponse> responseObserver
    ) {
        serverState.deactivate();
        responseObserver.onCompleted();
    }

    @Override
    public void gossip(
            GossipRequest request,
            StreamObserver<GossipResponse> responseObserver
    ) {
        serverState.gossip();
        responseObserver.onCompleted();
    }

    // TODO: add getLedgerState method

}
