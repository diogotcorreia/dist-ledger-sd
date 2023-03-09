package pt.tecnico.distledger.adminclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.CustomLog;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc.*;

@CustomLog(topic = "Service")
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
        log.debug("Sending request to activate server");
        // noinspection ResultOfMethodCallIgnored
        stub.activate(ActivateRequest.newBuilder().build());
        log.debug("Receiving response of server activation");
    }

    public void deactivate(String server) {
        // TODO: server is useful from phase 2 onwards
        log.debug("Sending request to deactivate server");
        // noinspection ResultOfMethodCallIgnored
        stub.deactivate(DeactivateRequest.newBuilder().build());
        log.debug("Receivign response of server deactivation");
    }

    public void gossip() {
        log.debug("Sending gossip request");
        // noinspection ResultOfMethodCallIgnored
        stub.gossip(GossipRequest.newBuilder().build());
        log.debug("Receiving gossip response");
    }

    public GetLedgerStateResponse getLedgerState(String server) {
        log.debug("Sending request for getting ledger state");
        final GetLedgerStateResponse response = stub.getLedgerState(GetLedgerStateRequest.newBuilder().build());
        log.debug("Receiving response for getting ledger state");
        return response;
    }

    @Override
    public void close() {
        channel.shutdown();
    }
}
