package pt.tecnico.distledger.common.connection;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.AbstractBlockingStub;
import lombok.CustomLog;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import pt.tecnico.distledger.common.exceptions.ServerUnresolvableException;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.LookupServerRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.ServerAddress;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.ServerInfo;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc.NamingServerServiceBlockingStub;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;


/**
 * Uses the naming server to fetch and cache the addresses of a certain server qualifier. Then, creates and manages
 * connections and stubs to these servers.
 *
 * @param <T> The type of the stub.
 */
@CustomLog(topic = "CachedServerResolver")
public class CachedServerResolver<T extends AbstractBlockingStub<T>> implements ServerResolver<T> {

    private final static String NAMING_SERVER_HOST = "localhost";
    private final static int NAMING_SERVER_PORT = 5001;
    private static final String SERVER_SERVICE_NAME = "DistLedger";

    private final Function<ManagedChannel, T> stubCreator;

    private ManagedChannel namingServerChannel;
    private NamingServerServiceBlockingStub namingServerStub;

    private final Map<String, ChannelStubPair<T>> channelStubPairMap = new ConcurrentHashMap<>();

    public CachedServerResolver(Function<ManagedChannel, T> stubCreator) {
        this.stubCreator = stubCreator;

        createNamingServerStub();
    }

    /**
     * Create a channel and stub to the naming server, if they do not exist yet.
     * If they exist, nothing is executed.
     */
    private void createNamingServerStub() {
        if (namingServerChannel != null && !namingServerChannel.isShutdown()) {
            return;
        }
        this.namingServerChannel = ManagedChannelBuilder.forAddress(NAMING_SERVER_HOST, NAMING_SERVER_PORT)
                .usePlaintext()
                .build();
        this.namingServerStub = NamingServerServiceGrpc.newBlockingStub(namingServerChannel);
    }

    /**
     * Resolve the address of a server given its qualifier, through the naming server.
     *
     * @param qualifier The qualifier of the server to resolve.
     * @return The address of the resolved server.
     * @throws ServerUnresolvableException if the server cannot be resolved.
     */
    private @NotNull ServerAddress resolveAddress(@NotNull String qualifier) throws ServerUnresolvableException {
        createNamingServerStub(); // ensure connection to naming server

        val lookupResponse = this.namingServerStub.lookupServer(
                LookupServerRequest.newBuilder()
                        .setServiceName(SERVER_SERVICE_NAME)
                        .setQualifier(qualifier)
                        .build()
        );

        return lookupResponse.getServerInfoList()
                .stream()
                .findFirst()
                .map(ServerInfo::getAddress)
                .orElseThrow(() -> new ServerUnresolvableException(qualifier));
    }

    @Override
    public synchronized @NotNull T resolveStub(@NotNull String qualifier) throws ServerUnresolvableException {
        val cachedPair = this.channelStubPairMap.get(qualifier);
        if (cachedPair != null && !cachedPair.channel().isShutdown()) {
            return cachedPair.stub();
        }

        final ServerAddress address = resolveAddress(qualifier);
        final String host = address.getHost();
        final int port = address.getPort();

        log.debug("Connecting to server '%s' at %s:%d", qualifier, host, port);
        final ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        final T stub = this.stubCreator.apply(channel);
        log.debug("Connected to server '%s' at %s:%d", qualifier, host, port);

        this.channelStubPairMap.put(qualifier, new ChannelStubPair<>(channel, stub));
        return stub;
    }

    @Override
    public synchronized void closeAllChannels() {
        this.namingServerChannel.shutdown();
        this.namingServerChannel = null;
        this.namingServerStub = null;
        this.channelStubPairMap.forEach((k, v) -> v.channel().shutdown());
        this.channelStubPairMap.clear();
    }

    public record ChannelStubPair<T>(ManagedChannel channel, T stub) {
    }
}
