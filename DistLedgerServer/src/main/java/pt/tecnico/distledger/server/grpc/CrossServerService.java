package pt.tecnico.distledger.server.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import lombok.CustomLog;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.Operation;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateRequest;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.ServerInfo;

import java.util.List;

import static pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc.newBlockingStub;

@CustomLog(topic = "Service")
public class CrossServerService implements AutoCloseable {

    private final ServerInfo serverInfo;

    private final ManagedChannel channel;

    private final DistLedgerCrossServerServiceBlockingStub stub;

    public CrossServerService(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
        channel = ManagedChannelBuilder
                .forAddress(serverInfo.getAddress().getHost(), serverInfo.getAddress().getPort())
                .usePlaintext()
                .build();
        stub = newBlockingStub(channel);
    }

    public void sendLedger(List<Operation> ledger) throws StatusRuntimeException {
        log.debug("Sending request to send ledger to server %s with %d operations", serverInfo, ledger.size());

        // noinspection ResultOfMethodCallIgnored
        stub.propagateState(
                PropagateStateRequest
                        .newBuilder()
                        .setState(
                                LedgerState
                                        .newBuilder()
                                        .addAllLedger(ledger)
                                        .build()
                        )
                        .build()
        );

        log.debug("Sent ledger to server");
    }

    @Override
    public void close() {
        channel.shutdown();
    }

}
