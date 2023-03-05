package pt.tecnico.distledger.adminclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc.*;

public class AdminService {

    private final ManagedChannel channel;

    private final AdminServiceBlockingStub stub;

    public AdminService(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        stub = AdminServiceGrpc.newBlockingStub(channel);
    }

    public void activate(String server) {
        // TODO: server is useful from phase 2 onwards
        stub.activate(ActivateRequest.newBuilder().build());
    }

    public void deactivate(String server) {
        // TODO: server is useful from phase 2 onwards
        stub.deactivate(DeactivateRequest.newBuilder().build());
    }

    public void gossip() {
        stub.gossip(GossipRequest.newBuilder().build());
    }

    public void getLedgerState(String server) {
        // TODO: server is useful from phase 2 onwards
        final GetLedgerStateResponse response = stub.getLedgerState(GetLedgerStateRequest.newBuilder().build());

        System.out.println(response);
    }
}
