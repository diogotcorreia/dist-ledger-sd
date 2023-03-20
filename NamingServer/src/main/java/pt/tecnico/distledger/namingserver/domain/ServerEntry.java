package pt.tecnico.distledger.namingserver.domain;

import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.ServerInfo;

public record ServerEntry(ServerAddress address, String qualifier) {
    public ServerInfo convertEntryToGrpc() {
        return ServerInfo
                .newBuilder()
                .setAddress(
                        address.convertAddressToGrpc()
                )
                .setQualifier(qualifier)
                .build();
    }
}
