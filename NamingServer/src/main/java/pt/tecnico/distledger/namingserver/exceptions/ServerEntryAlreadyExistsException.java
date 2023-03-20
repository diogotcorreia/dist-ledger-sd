package pt.tecnico.distledger.namingserver.exceptions;

import io.grpc.Status;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true)
@Getter
public class ServerEntryAlreadyExistsException extends Exception implements GrpcSerializableException {

    private final String serverName;

    public ServerEntryAlreadyExistsException(String serverName) {
        super(String.format("Not possible to register the server %s", serverName));
        this.serverName = serverName;
    }

    @Override
    public Status.Code getStatusCode() { return Status.Code.ALREADY_EXISTS; }
}
