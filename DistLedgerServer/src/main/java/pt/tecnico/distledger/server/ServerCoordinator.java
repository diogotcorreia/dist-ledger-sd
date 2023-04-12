package pt.tecnico.distledger.server;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.exceptions.PropagationException;
import pt.tecnico.distledger.server.grpc.CrossServerService;
import pt.tecnico.distledger.server.grpc.NamingServerService;
import pt.tecnico.distledger.server.visitor.ConvertOperationsToGrpcVisitor;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.ServerInfo;

import java.util.List;
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
        this.serverState = new ServerState(qualifier, this::propagateLedgerStateToAllServers);
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
        propagateLedgerStateToAllServers(serverState.gossip(serverTo), serverTo);
    }


    public void propagateLedgerStateToAllServers(List<Operation> pendingOperation, String serverTo) {
        ConvertOperationsToGrpcVisitor visitor = new ConvertOperationsToGrpcVisitor();
        for (Operation operation : pendingOperation) {
            operation.accept(visitor);
        }

        long attempts = 0;

        do {
            if (peersCache.size() == 0) {
                populatePeersCache();
            }
            long successfulCount = sendLedgerToServers(visitor, serverTo);
            if (successfulCount == peersCache.size() && peersCache.size() > 0) {
                return;
            }
            peersCache.invalidateAll();
        } while (++attempts < MAX_RETRIES);

        throw new RuntimeException(new PropagationException());
    }

    private void populatePeersCache() {
        namingServerService.getServerList()
                .stream()
                .forEach(serverInfo -> peersCache.put(serverInfo, new CrossServerService(serverInfo)));
    }

    /**
     * @param visitor visitor with ledger to send to servers
     * @return number of successful attempts
     */
    private long sendLedgerToServers(ConvertOperationsToGrpcVisitor visitor, String serverTo) {
        CrossServerService serverService =
                peersCache.asMap().keySet().stream().filter(serverInfo -> serverInfo.getQualifier().equals(serverTo))
                        .;


        return peersCache.asMap()
                .entrySet()
                .parallelStream()
                .map(service -> {
                    try {
                        service.getValue().sendLedger(visitor.getLedger());
                        return true;
                    } catch (Exception e) {
                        log.error("Failed to send ledger to server: %s", service.getKey().getQualifier());
                        peersCache.invalidate(service.getKey());
                        return false;
                    }
                })
                .filter(Boolean::booleanValue)
                .count();
    }

}
