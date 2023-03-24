package pt.tecnico.distledger.namingserver.domain;

import lombok.Getter;
import pt.tecnico.distledger.namingserver.exceptions.ServerDoesNotExistException;
import pt.tecnico.distledger.namingserver.exceptions.ServerEntryAlreadyExistsException;
import pt.tecnico.distledger.namingserver.exceptions.ServerWithInvalidParametersException;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class ServiceEntry {

    private final String serviceName;

    private final List<ServerEntry> servers;

    public ServiceEntry(String serviceName) {
        this.serviceName = serviceName;
        servers = new CopyOnWriteArrayList<>();
    }

    public void addServerEntry(
            ServerAddress serverAddress,
            String serverQualifier
    ) throws ServerWithInvalidParametersException, ServerEntryAlreadyExistsException {
        // TODO: remove this for phase 3
        if (serverQualifier == null || serverQualifier.isEmpty() ||
                (!serverQualifier.equals("A") && !serverQualifier.equals("B"))) {
            throw new ServerWithInvalidParametersException(serverQualifier);
        }
        if (servers.stream()
                .anyMatch(
                        serverEntry -> serverEntry.address().equals(serverAddress) || serverEntry.qualifier()
                                .equals(serverQualifier)
                )) {
            throw new ServerEntryAlreadyExistsException(serverQualifier);
        }
        servers.add(new ServerEntry(serverAddress, serverQualifier));
    }

    public List<ServerEntry> getServerEntriesWithQualifier(String qualifier) {
        return qualifier == null || qualifier.isEmpty()
                ? servers
                : servers
                        .stream()
                        .filter(serverEntry -> serverEntry.qualifier().equals(qualifier))
                        .toList();
    }

    public void removeServerEntry(ServerAddress serverAddress) throws ServerDoesNotExistException {
        if (!servers.removeIf(serverEntry -> serverEntry.address().equals(serverAddress))) {
            throw new ServerDoesNotExistException(serviceName);
        }
    }

}
