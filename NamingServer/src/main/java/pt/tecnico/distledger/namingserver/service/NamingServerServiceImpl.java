package pt.tecnico.distledger.namingserver.service;

import pt.tecnico.distledger.namingserver.domain.NamingServer;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc.NamingServerServiceImplBase;

public class NamingServerServiceImpl extends NamingServerServiceImplBase {

    private final NamingServer namingServer;

    public NamingServerServiceImpl(NamingServer namingServer) {
        this.namingServer = namingServer;
    }
}
