package pt.tecnico.distledger.server;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.CustomLog;
import lombok.Getter;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.exceptions.PropagationException;
import pt.tecnico.distledger.server.grpc.CrossServerService;
import pt.tecnico.distledger.server.grpc.NamingServerService;
import pt.tecnico.distledger.server.visitor.ConvertOperationsToGrpcVisitor;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.ServerInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@CustomLog(topic = "Server Coordinator")
public class ServerCoordinator {

    private static final int TIMEOUT = 2;

    private static final int MAX_RETRIES = 3;

    private final int port;
    private final String qualifier;

    @Getter
    private final ServerState serverState;

    private final Cache<ServerInfo, CrossServerService> peersCache = CacheBuilder.newBuilder()
            .expireAfterWrite(TIMEOUT, TimeUnit.MINUTES)
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

    public void gossip(String serverTo) {
        propagateLedgerStateToServer(serverState.getOperations(serverTo), serverTo);
        serverState.updateGossipTimestamp(serverTo);
    }


    public void propagateLedgerStateToServer(List<Operation> pendingOperation, String serverTo) {
        ConvertOperationsToGrpcVisitor visitor = new ConvertOperationsToGrpcVisitor();
        for (Operation operation : pendingOperation) {
            operation.accept(visitor);
        }

        long attempts = 0;

        do {
            if (peersCache.size() == 0) {
                populatePeersCache();
            }
            boolean successful = sendLedgerToServers(visitor, serverTo);
            if (successful) {
                return;
            }
            peersCache.invalidateAll();
        } while (++attempts < MAX_RETRIES);

        throw new RuntimeException(new PropagationException());
    }

    private void populatePeersCache() {
        namingServerService.getServerList()
                .forEach(serverInfo -> peersCache.put(serverInfo, new CrossServerService(serverInfo)));
    }

    /**
     * Sends the ledger to the server in the cache
     *
     * @param visitor
     * @param serverTo
     * @return true if the ledger was sent successfully, false otherwise
     */

    private boolean sendLedgerToServers(ConvertOperationsToGrpcVisitor visitor, String serverTo) {
        try {
            List<CrossServerService> crossServerService = peersCache.asMap()
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getKey().getQualifier().equals(serverTo))
                    .map(Map.Entry::getValue)
                    .toList();

            if (crossServerService.isEmpty()) {
                return false;
            }

            crossServerService.forEach(
                    crossServer -> crossServer.sendLedger(
                            visitor.getLedger(),
                            serverState.getReplicaTimestamp()
                    )
            );
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Failed to send ledger to server: %s", serverTo);
            peersCache.invalidate(serverTo);
        }
        return false;
    }

}
