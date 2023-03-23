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

import java.util.concurrent.TimeUnit;

@CustomLog(topic = "Server Coordinator")
public class ServerCoordinator {

    private static final String PRIMARY_SERVER = "A";

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
        this.serverState = new ServerState(qualifier.equals(PRIMARY_SERVER), this::propagateLedgerStateToAllServers);
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

    public void propagateLedgerStateToAllServers(Operation pendingOperation) {
        ConvertOperationsToGrpcVisitor visitor = new ConvertOperationsToGrpcVisitor();
        serverState.operateOverLedger(visitor);
        pendingOperation.accept(visitor);

        long attempts = 0;

        do {
            if (peersCache.size() == 0) {
                populatePeersCache();
            }
            long unsuccessfulCount = sendLedgerToServers(visitor);
            if (unsuccessfulCount == 0 && peersCache.size() > 0) {
                return;
            }
            peersCache.invalidateAll();
        } while (attempts++ < MAX_RETRIES);

        throw new RuntimeException(new PropagationException());
    }

    private void populatePeersCache() {
        namingServerService.getServerList()
                .stream()
                .filter(serverInfo -> !serverInfo.getQualifier().equals(qualifier))
                .forEach(serverInfo -> peersCache.put(serverInfo, new CrossServerService(serverInfo)));
    }

    /**
     * @param visitor visitor with ledger to send to servers
     * @return number of unsuccessful attempts
     */
    private long sendLedgerToServers(ConvertOperationsToGrpcVisitor visitor) {
        return peersCache.asMap()
                .entrySet()
                .parallelStream()
                .map(service -> {
                    try {
                        service.getValue().sendLedger(visitor.getLedger());
                        return false;
                    } catch (Exception e) {
                        log.error("Failed to send ledger to server: %s", service.getKey().getQualifier());
                        peersCache.invalidate(service.getKey());
                        return true;
                    }
                })
                .filter(Boolean::booleanValue)
                .count();
    }

}
