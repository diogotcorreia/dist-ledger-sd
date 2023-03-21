package pt.tecnico.distledger.namingserver.domain;

import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger;

public record ServerAddress(String host, int port) {

    public NamingServerDistLedger.ServerAddress convertAddressToGrpc() {
        return NamingServerDistLedger.ServerAddress.newBuilder()
                .setHost(this.host)
                .setPort(this.port)
                .build();
    }

}
