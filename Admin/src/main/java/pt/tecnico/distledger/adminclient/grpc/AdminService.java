package pt.tecnico.distledger.adminclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.CustomLog;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.ActivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.GetLedgerStateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.GetLedgerStateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.GossipRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc.AdminServiceBlockingStub;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc.*;

@CustomLog(topic = "Service")
public class AdminService implements AutoCloseable {

    private final static String HOST = "localhost";
    private final static int PORT = 5001;
    private final static String SERVER_SERVICE_NAME = "DistLedger";
    private final ManagedChannel namingChannel;
    private final NamingServerServiceBlockingStub namingServerStub;
    private ManagedChannel adminChannel;
    private AdminServiceBlockingStub adminStub;

    public AdminService() {
        namingChannel = ManagedChannelBuilder.forAddress(HOST, PORT)
                .usePlaintext()
                .build();
        namingServerStub = NamingServerServiceGrpc.newBlockingStub(namingChannel);
    }

    public void activate(String qualifier) {
        connectToServer(qualifier);
        log.debug("Sending request to activate server");
        // noinspection ResultOfMethodCallIgnored
        adminStub.activate(ActivateRequest.newBuilder().build());
        log.debug("Receiving response of server activation");
    }

    public void deactivate(String qualifier) {
        connectToServer(qualifier);
        log.debug("Sending request to deactivate server");
        // noinspection ResultOfMethodCallIgnored
        adminStub.deactivate(DeactivateRequest.newBuilder().build());
        log.debug("Receivign response of server deactivation");
    }

    public void gossip() {
        log.debug("Sending gossip request");
        // noinspection ResultOfMethodCallIgnored
        adminStub.gossip(GossipRequest.newBuilder().build());
        log.debug("Receiving gossip response");
    }

    public GetLedgerStateResponse getLedgerState(String qualifier) {
        connectToServer(qualifier);
        log.debug("Sending request for getting ledger state");
        final GetLedgerStateResponse response = adminStub.getLedgerState(GetLedgerStateRequest.newBuilder().build());
        log.debug("Receiving response for getting ledger state");
        return response;
    }

    private void connectToServer(String qualifier) {
        establishConnection(
                namingServerStub.lookupServer(
                        LookupServerRequest
                                .newBuilder()
                                .setQualifier(qualifier)
                                .setServiceName(SERVER_SERVICE_NAME)
                                .build()
                )
        );
    }

    private void establishConnection(LookupServerResponse response) {
        if (response.getServerInfoCount() == 0) {
            throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("No server found"));
        }

        final ServerInfo serverInfo = response.getServerInfo(0);
        final ServerAddress serverAddress = serverInfo.getAddress();
        final String host = serverAddress.getHost();
        final int port = serverAddress.getPort();

        adminChannel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        adminStub = AdminServiceGrpc.newBlockingStub(adminChannel);
    }

    @Override
    public void close() {
        adminChannel.shutdown();
    }
}
