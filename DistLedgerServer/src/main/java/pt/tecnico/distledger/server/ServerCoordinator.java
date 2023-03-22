package pt.tecnico.distledger.server;

import lombok.Getter;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.grpc.CrossServerService;
import pt.tecnico.distledger.server.grpc.NamingServerService;
import pt.tecnico.distledger.server.visitor.ConvertOperationsToGrpcVisitor;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger;

import java.util.List;

public class ServerCoordinator {

    private static final String PRIMARY_SERVER = "A";

    private final int port;
    private final String qualifier;

    @Getter
    private final ServerState serverState;

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
        // TODO this uses gRPC objects. convert to domain?

        List<NamingServerDistLedger.ServerInfo> serverList;
        serverList = namingServerService.getServerList()
                .stream()
                .filter(serverInfo -> !serverInfo.getQualifier().equals("A"))
                .toList();

        ConvertOperationsToGrpcVisitor visitor = new ConvertOperationsToGrpcVisitor();
        serverState.operateOverLedger(visitor);
        pendingOperation.accept(visitor);

        // TODO cache them?
        for (NamingServerDistLedger.ServerInfo serverInfo : serverList) {
            try (var serverService = new CrossServerService(serverInfo)) {
                serverService.sendLedger(visitor.getLedger());
            }
        }
    }

}
