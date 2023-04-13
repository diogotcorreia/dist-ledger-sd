package pt.tecnico.distledger.namingserver.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pt.tecnico.distledger.namingserver.exceptions.ServerDoesNotExistException;
import pt.tecnico.distledger.namingserver.exceptions.ServerEntryAlreadyExistsException;
import pt.tecnico.distledger.namingserver.exceptions.ServerWithInvalidParametersException;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@RequiredArgsConstructor
public class ServiceEntry {

    private final String serviceName;

    private final List<ServerEntry> servers = new CopyOnWriteArrayList<>();

    public void addServerEntry(
            ServerAddress serverAddress,
            String serverQualifier
    ) throws ServerWithInvalidParametersException, ServerEntryAlreadyExistsException {
        if (serverQualifier == null || serverQualifier.isEmpty()) {
            throw new ServerWithInvalidParametersException(serverQualifier);
        }
        synchronized (servers) {
            if (servers.stream()
                    .anyMatch(
                            serverEntry -> serverEntry.address().equals(serverAddress) || serverEntry.qualifier()
                                    .equals(serverQualifier)
                    )) {
                throw new ServerEntryAlreadyExistsException(serverAddress, serverQualifier, serviceName);
            }
            servers.add(new ServerEntry(serverAddress, serverQualifier));
        }
    }

    public List<ServerEntry> getServerEntriesWithQualifier(String qualifier) {
        if (qualifier == null || qualifier.isEmpty()) {
            return Collections.unmodifiableList(servers);
        }
        return servers
                .stream()
                .filter(serverEntry -> serverEntry.qualifier().equals(qualifier))
                .toList();
    }

    public void removeServerEntry(ServerAddress serverAddress) throws ServerDoesNotExistException {
        if (!servers.removeIf(serverEntry -> serverEntry.address().equals(serverAddress))) {
            throw new ServerDoesNotExistException(serverAddress, serviceName);
        }
    }

}
