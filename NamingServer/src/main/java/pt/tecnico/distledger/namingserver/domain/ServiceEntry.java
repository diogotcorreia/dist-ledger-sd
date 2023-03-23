package pt.tecnico.distledger.namingserver.domain;

import lombok.Getter;
import pt.tecnico.distledger.namingserver.exceptions.ServerDoesNotExistException;
import pt.tecnico.distledger.namingserver.exceptions.ServerEntryAlreadyExistsException;

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
    ) throws ServerEntryAlreadyExistsException {
        if (servers.stream().anyMatch(serverEntry -> serverEntry.address().equals(serverAddress))) {
            throw new ServerEntryAlreadyExistsException(serverQualifier);
        }
        servers.add(new ServerEntry(serverAddress, serverQualifier));
    }


    public synchronized List<ServerEntry> getServerEntriesWithQualifier(String qualifier) {
        return qualifier == null || qualifier.isEmpty()
                ? servers
                : servers
                        .stream()
                        .filter(serverEntry -> serverEntry.qualifier().equals(qualifier))
                        .toList();
    }

    public synchronized void removeServerEntry(ServerAddress serverAddress) throws ServerDoesNotExistException {
        if (!servers.removeIf(serverEntry -> serverEntry.address().equals(serverAddress))) {
            throw new ServerDoesNotExistException(serviceName);
        }
    }

}
