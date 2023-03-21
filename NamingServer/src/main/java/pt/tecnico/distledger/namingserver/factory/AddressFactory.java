package pt.tecnico.distledger.namingserver.factory;

import pt.tecnico.distledger.namingserver.domain.ServerAddress;
import pt.tecnico.distledger.namingserver.exceptions.ServerWithInvalidParametersException;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger;

public class AddressFactory {

    public ServerAddress createAddressFromGrpc(
            NamingServerDistLedger.ServerAddress address
    ) throws ServerWithInvalidParametersException {

        final String host = address.getHost();
        final int port = address.getPort();

        if (host.isEmpty()) {
            throw new ServerWithInvalidParametersException(host, port);
        }
        if (port < 1024 || port > 65535) {
            throw new ServerWithInvalidParametersException(host, port);
        }
        return new ServerAddress(address.getHost(), port);
    }

}
