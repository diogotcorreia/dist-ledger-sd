package pt.tecnico.distledger.namingserver.domain;

public record ServerAddress(String host, int port) {

    @Override
    public String toString() {
        return host + ":" + port;
    }
}
