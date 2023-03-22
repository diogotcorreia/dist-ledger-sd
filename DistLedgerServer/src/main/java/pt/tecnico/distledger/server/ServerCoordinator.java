package pt.tecnico.distledger.server;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.grpc.CrossServerService;
import pt.tecnico.distledger.server.grpc.NamingServerService;
import pt.tecnico.distledger.server.visitor.ConvertOperationsToGrpcVisitor;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.ServerInfo;

import java.util.concurrent.TimeUnit;

public class ServerCoordinator {

    private static final String PRIMARY_SERVER = "A";

    private final int port;
    private final String qualifier;

    @Getter
    private final ServerState serverState;


    private final Cache<ServerInfo, CrossServerService> peersCache = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES)
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
        if (peersCache.size() == 0) {
            namingServerService.getServerList()
                    .stream()
                    .filter(serverInfo -> !serverInfo.getQualifier().equals(qualifier))
                    .forEach(serverInfo -> peersCache.put(serverInfo, new CrossServerService(serverInfo)));
        }

        ConvertOperationsToGrpcVisitor visitor = new ConvertOperationsToGrpcVisitor();
        serverState.operateOverLedger(visitor);
        pendingOperation.accept(visitor);

        // TODO handle errors?
        // TODO retry again
        long successfulCount = peersCache.asMap()
                .entrySet()
                .stream()
                .map(service -> {
                    try {
                        service.getValue().sendLedger(visitor.getLedger());
                        return true;
                    } catch (Exception e) {
                        // TODO log?
                        e.printStackTrace();
                        peersCache.invalidate(service.getKey());
                        return false;
                    }
                })
                .filter(Boolean::booleanValue)
                .count();

        if (successfulCount < 1) {
            // TODO change exception
            throw new RuntimeException("can't propagate stuff");
        }
    }

}
