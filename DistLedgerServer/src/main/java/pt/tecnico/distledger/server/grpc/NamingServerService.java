package pt.tecnico.distledger.server.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.CustomLog;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.DeleteServerRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.LookupServerRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.LookupServerResponse;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.RegisterServerRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.ServerAddress;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.ServerInfo;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc.NamingServerServiceBlockingStub;

import java.util.List;

import static pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc.newBlockingStub;

@CustomLog(topic = "Service")
public class NamingServerService implements AutoCloseable {

    private static final String NAMING_SERVER_HOST = "localhost";

    private static final int NAMING_SERVER_PORT = 5001;

    private static final String SERVICE_NAME = "DistLedger";

    private final ManagedChannel channel;

    private final NamingServerServiceBlockingStub stub;

    public NamingServerService() {
        channel = ManagedChannelBuilder
                .forAddress(NAMING_SERVER_HOST, NAMING_SERVER_PORT)
                .usePlaintext()
                .build();
        stub = newBlockingStub(channel);
    }

    public void register(int port, String serverQualifier) {
        log.debug("Sending request to register server");

        // noinspection ResultOfMethodCallIgnored
        stub.registerServer(
                RegisterServerRequest.newBuilder()
                        .setServiceName(SERVICE_NAME)
                        .setAddress(
                                ServerAddress.newBuilder()
                                        .setHost(NAMING_SERVER_HOST)
                                        .setPort(port)
                                        .build()
                        )
                        .setQualifier(serverQualifier)
                        .build()
        );

        log.debug("Receiving response of server registration");
    }

    public List<ServerInfo> getServerList() {
        log.debug("Sending request to get server list");
        LookupServerResponse response = stub.lookupServer(
                LookupServerRequest.newBuilder()
                        .setServiceName(SERVICE_NAME)
                        .build()
        );
        log.debug("Receiving response of server list");
        return response.getServerInfoList();
    }

    public void removeServer(int port) {
        log.debug("Sending request to remove server");

        // noinspection ResultOfMethodCallIgnored
        stub.deleteServer(
                DeleteServerRequest.newBuilder()
                        .setServiceName(SERVICE_NAME)
                        .setAddress(
                                ServerAddress.newBuilder().setHost(NAMING_SERVER_HOST).setPort(port).build()
                        )
                        .build()
        );
        log.debug("Receiving response of server removal");
    }

    @Override
    public void close() {
        channel.shutdown();
    }
    
}
