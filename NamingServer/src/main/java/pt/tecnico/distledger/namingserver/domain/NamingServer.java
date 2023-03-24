package pt.tecnico.distledger.namingserver.domain;

import lombok.Getter;
import pt.tecnico.distledger.namingserver.exceptions.ServerDoesNotExistException;
import pt.tecnico.distledger.namingserver.exceptions.ServerEntryAlreadyExistsException;
import pt.tecnico.distledger.namingserver.exceptions.ServerWithInvalidParametersException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class NamingServer {

    private final Map<String, ServiceEntry> services = new ConcurrentHashMap<>();

    public void register(
            String serviceName,
            ServerAddress serverAddress,
            String serverQualifier
    ) throws ServerEntryAlreadyExistsException, ServerWithInvalidParametersException {
        services
                .computeIfAbsent(serviceName, ServiceEntry::new)
                .addServerEntry(serverAddress, serverQualifier);
    }

    public List<ServerEntry> lookup(String serviceName, String qualifier) {
        return Optional.ofNullable(services.get(serviceName))
                .map(service -> service.getServerEntriesWithQualifier(qualifier))
                .orElseGet(Collections::emptyList);
    }

    public void delete(
            String serviceName,
            ServerAddress serverAddress
    ) throws ServerDoesNotExistException {
        ServiceEntry service = services.get(serviceName);
        if (service == null) {
            throw new ServerDoesNotExistException(serverAddress, serviceName);
        }

        service.removeServerEntry(serverAddress);
    }

}
