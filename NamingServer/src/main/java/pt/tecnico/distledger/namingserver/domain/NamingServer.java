package pt.tecnico.distledger.namingserver.domain;

import lombok.Getter;
import pt.tecnico.distledger.namingserver.exceptions.ServerDoesNotExistException;
import pt.tecnico.distledger.namingserver.exceptions.ServerEntryAlreadyExistsException;
import pt.tecnico.distledger.namingserver.exceptions.ServerWithInvalidParametersException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class NamingServer {

    private final Map<String, ServiceEntry> services;

    public NamingServer() {
        services = new ConcurrentHashMap<>();
    }

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
        return services.containsKey(serviceName)
                ? services.get(serviceName).getServerEntriesWithQualifier(qualifier)
                : Collections.emptyList();
    }

    public void delete(
            String serviceName,
            ServerAddress serverAddress
    ) throws ServerDoesNotExistException {
        ServiceEntry service;
        synchronized (services) {
            service = services.get(serviceName);
        }
        if (service == null) {
            throw new ServerDoesNotExistException(serviceName);
        }

        service.removeServerEntry(serverAddress);
        if (service.getServers().isEmpty()) {
            synchronized (services) {
                services.remove(serviceName);
            }
        }
    }

}
