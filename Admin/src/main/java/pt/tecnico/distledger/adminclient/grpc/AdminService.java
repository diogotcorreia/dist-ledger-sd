package pt.tecnico.distledger.adminclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc.*;

public class AdminService extends AdminServiceGrpc.AdminServiceImplBase implements AutoCloseable {

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
        // noInspection ResultOfMethodCallIgnored
        stub.activate(ActivateRequest.newBuilder().build());
    }

    public void deactivate(String server) {
        // TODO: server is useful from phase 2 onwards
        // noinspection ResultOfMethodCallIgnored
        stub.deactivate(DeactivateRequest.newBuilder().build());
    }

    public void gossip() {
        // noinspection ResultOfMethodCallIgnored
        stub.gossip(GossipRequest.newBuilder().build());
    }

    public LedgerState getLedgerState(String server) {
        return stub.getLedgerState(GetLedgerStateRequest.newBuilder().build()).getLedgerState();
    }

    @Override
    public void close() {
        channel.shutdown();
    }
}
