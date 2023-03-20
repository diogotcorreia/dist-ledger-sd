package pt.tecnico.distledger.namingserver.domain;

import lombok.Getter;
import pt.tecnico.distledger.namingserver.exceptions.ServerDoesNotExistException;
import pt.tecnico.distledger.namingserver.exceptions.ServerEntryAlreadyExistsException;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger;

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
            String host,
            int port,
            String serverQualifier
    ) throws ServerEntryAlreadyExistsException {
        services
                .computeIfAbsent(serviceName, ServiceEntry::new)
                .addServer(new ServerAddress(host, port), serverQualifier);
    }

    public List<ServerEntry> lookup(String serviceName, String qualifier) {
        return services.containsKey(serviceName)
                ? services.get(serviceName).getServers(qualifier)
                : Collections.emptyList();
    }

    public void delete(
            String serviceName,
            NamingServerDistLedger.ServerAddress serverAddress
    ) throws ServerDoesNotExistException {
        if (!services.containsKey(serviceName)) {
            throw new ServerDoesNotExistException(serviceName);
        }
        services.get(serviceName).delete(new ServerAddress(serverAddress.getHost(), serverAddress.getPort()));
    }
    
}
