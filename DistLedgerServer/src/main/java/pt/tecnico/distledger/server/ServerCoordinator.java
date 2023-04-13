package pt.tecnico.distledger.server;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.CustomLog;
import lombok.Getter;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.PropagationException;
import pt.tecnico.distledger.server.grpc.CrossServerService;
import pt.tecnico.distledger.server.grpc.NamingServerService;
import pt.tecnico.distledger.server.visitor.ConvertOperationsToGrpcVisitor;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@CustomLog(topic = "Server Coordinator")
public class ServerCoordinator {

    private static final int TIMEOUT = 2;

    private static final int MAX_RETRIES = 3;

    private final int port;
    private final String qualifier;

    @Getter
    private final ServerState serverState;

    private final Cache<String, CrossServerService> peersCache = CacheBuilder.newBuilder()
            .expireAfterWrite(TIMEOUT, TimeUnit.MINUTES)
            // TODO fix typing to avoid casting?
            .removalListener(notification -> ((CrossServerService) notification.getValue()).close())
            .build();

    private final NamingServerService namingServerService = new NamingServerService();

    public ServerCoordinator(int port, String qualifier) {
        this.port = port;
        this.qualifier = qualifier;
        this.serverState = new ServerState(qualifier);
    }

    public void registerOnNamingServer() {
        namingServerService.register(port, qualifier);
    }

    public void unregisterFromNamingServer() {
        namingServerService.removeServer(port);
    }

    public void shutdown() {
        namingServerService.close();
    }

    public void propagateUsingGossip(String serverTo) {
        ConvertOperationsToGrpcVisitor visitor = new ConvertOperationsToGrpcVisitor();
        serverState.operateOverLedgerToPropagateToReplica(visitor, serverTo);
        propagateLedgerStateToServer(visitor, serverTo);
        serverState.updateGossipTimestamp(serverTo);
    }

    private void propagateLedgerStateToServer(ConvertOperationsToGrpcVisitor visitor, String serverTo) {
        long attempts = 0;
        do {
            if (peersCache.size() == 0) {
                populatePeersCache();
            }
            if (sendLedgerToServer(visitor, serverTo)) {
                return;
            }
            // TODO improve when this is fetched again
            populatePeersCache();
        } while (++attempts < MAX_RETRIES);

        throw new RuntimeException(new PropagationException());
    }

    /**
     * Sends the ledger to the server in the cache, invalidating the cache if it fails.
     *
     * @param visitor  The visitor containing the operations.
     * @param serverTo The qualifier of the replica to send to.
     * @return true if the ledger was sent successfully, false otherwise.
     */
    private boolean sendLedgerToServer(ConvertOperationsToGrpcVisitor visitor, String serverTo) {
        try {
            Optional.ofNullable(peersCache.getIfPresent(serverTo))
                    .orElseThrow(() -> new RuntimeException("Server not found"))
                    .sendLedger(
                            visitor.getLedger(),
                            serverState.getReplicaTimestamp()
                    );
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Failed to send ledger to server: %s", serverTo);
            peersCache.invalidate(serverTo);
        }
        return false;
    }

    private void populatePeersCache() {
        // TODO this can be improved to only fetch one server at a time on-demand
        namingServerService.getServerList()
                .forEach(serverInfo -> peersCache.put(serverInfo.getQualifier(), new CrossServerService(serverInfo)));
    }

}
