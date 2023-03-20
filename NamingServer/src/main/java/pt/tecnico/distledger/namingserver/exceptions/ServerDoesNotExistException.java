package pt.tecnico.distledger.namingserver.exceptions;

import io.grpc.Status;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true)
@Getter
public class ServerDoesNotExistException extends Exception implements GrpcSerializableException {

    private final String serverName;

    public ServerDoesNotExistException(String serverName) {
        super(String.format("Not possible to remove the server %s", serverName));
        this.serverName = serverName;
    }

    @Override
    public Status.Code getStatusCode() {
        return Status.Code.INVALID_ARGUMENT;
    }

}
