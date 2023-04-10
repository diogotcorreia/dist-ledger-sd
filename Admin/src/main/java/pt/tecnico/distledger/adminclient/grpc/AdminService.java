package pt.tecnico.distledger.adminclient.grpc;

import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import pt.tecnico.distledger.common.connection.ServerResolver;
import pt.tecnico.distledger.common.exceptions.ServerUnresolvableException;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.ActivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.GetLedgerStateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.GetLedgerStateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.GossipRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc.AdminServiceBlockingStub;

@CustomLog(topic = "Service")
@RequiredArgsConstructor
public class AdminService implements AutoCloseable {

    private final ServerResolver<AdminServiceBlockingStub> serverResolver;

    public void activate(String qualifier) throws ServerUnresolvableException {
        log.debug("[Server '%s'] Sending request to activate server", qualifier);
        // noinspection ResultOfMethodCallIgnored
        serverResolver.resolveStub(qualifier).activate(ActivateRequest.newBuilder().build());
        log.debug("[Server '%s'] Receiving response of server activation", qualifier);
    }

    public void deactivate(String qualifier) throws ServerUnresolvableException {
        log.debug("[Server '%s'] Sending request to deactivate server", qualifier);
        // noinspection ResultOfMethodCallIgnored
        serverResolver.resolveStub(qualifier).deactivate(DeactivateRequest.newBuilder().build());
        log.debug("[Server '%s'] Receiving response of server deactivation", qualifier);
    }

    public void gossip(String serverFrom, String serverTo) throws ServerUnresolvableException {
        log.debug("[Server '%s'] Sending gossip request", serverFrom);
        // noinspection ResultOfMethodCallIgnored
        serverResolver.resolveStub(serverFrom)
                .gossip(
                        GossipRequest.newBuilder()
                                .setQualifier(serverTo)
                                .build()
                );
        log.debug("[Server '%s'] Receiving gossip response", serverTo);
    }

    public GetLedgerStateResponse getLedgerState(String qualifier) throws ServerUnresolvableException {
        log.debug("[Server '%s'] Sending request for getting ledger state", qualifier);
        final GetLedgerStateResponse response = serverResolver.resolveStub(qualifier)
                .getLedgerState(GetLedgerStateRequest.newBuilder().build());
        log.debug("[Server '%s'] Receiving response for getting ledger state", qualifier);
        return response;
    }

    @Override
    public void close() {
        serverResolver.close();
    }
}
