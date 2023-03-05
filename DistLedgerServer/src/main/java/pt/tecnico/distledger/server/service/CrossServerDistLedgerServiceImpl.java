package pt.tecnico.distledger.server.service;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;

public class CrossServerDistLedgerServiceImpl extends DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase {

    private final ServerState serverState;

    public CrossServerDistLedgerServiceImpl(ServerState serverState) {
        this.serverState = serverState;
    }

}
