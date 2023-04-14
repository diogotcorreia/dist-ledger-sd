package pt.tecnico.distledger.server;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import io.grpc.StatusRuntimeException;
import lombok.CustomLog;
import lombok.Getter;
import lombok.val;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.ServerUnavailableException;
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
            .removalListener(
                    (RemovalNotification<String, CrossServerService> notification) -> notification.getValue().close()
            )
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

    public void propagateUsingGossip(String serverTo) throws ServerUnavailableException {
        if (!serverState.getActive().get()) {
            throw new ServerUnavailableException(qualifier);
        }
        ConvertOperationsToGrpcVisitor visitor = new ConvertOperationsToGrpcVisitor();
        // This "data race" isn't an issue because even if the timestamp isn't up-to-date
        // after iterating over the ledger, it won't affect the gossip propagation.
        // Worst case scenario, we will send a duplicate operation in further gossip propagations,
        // which would be ignored by the receiving replica.
        val timestamp = serverState.getReplicaTimestamp().clone();
        serverState.operateOverLedgerToPropagateToReplica(visitor, serverTo);
        propagateLedgerStateToServer(visitor, serverTo);
        serverState.updateGossipTimestamp(serverTo, timestamp);
    }

    private void propagateLedgerStateToServer(
            ConvertOperationsToGrpcVisitor visitor,
            String serverTo
    ) throws ServerUnavailableException {
        long attempts = 0;
        do {
            if (peersCache.size() == 0) {
                populatePeersCache();
            }
            if (sendLedgerToServer(visitor, serverTo)) {
                return;
            }
            populatePeersCache();
        } while (++attempts < MAX_RETRIES);

        throw new ServerUnavailableException(serverTo);
    }

    /**
     * Sends the ledger to the server in the cache, invalidating the cache if it fails.
     *
     * @param visitor  The visitor containing the operations.
     * @param serverTo The qualifier of the replica to send to.
     * @return true if the ledger was sent successfully, false otherwise.
     */
    private boolean sendLedgerToServer(
            ConvertOperationsToGrpcVisitor visitor,
            String serverTo
    ) throws ServerUnavailableException {
        try {
            if (!serverState.getActive().get()) {
                throw new ServerUnavailableException(qualifier);
            }
            Optional.ofNullable(peersCache.getIfPresent(serverTo))
                    .orElseThrow(() -> new RuntimeException("Server not found"))
                    .sendLedger(visitor.getLedger());
            return true;
        } catch (ServerUnavailableException e) {
            peersCache.invalidate(qualifier);
            throw e;
        } catch (StatusRuntimeException e) {
            throw new ServerUnavailableException(serverTo);
        } catch (Exception e) {
            e.printStackTrace();
            peersCache.invalidate(serverTo);
        }
        return false;
    }

    private void populatePeersCache() {
        namingServerService.getServerList()
                .forEach(serverInfo -> peersCache.put(serverInfo.getQualifier(), new CrossServerService(serverInfo)));
    }

}
