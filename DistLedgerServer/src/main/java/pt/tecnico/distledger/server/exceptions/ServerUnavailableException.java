package pt.tecnico.distledger.server.exceptions;

import io.grpc.Status;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true)
@Getter
public class ServerUnavailableException extends Exception implements GrpcSerializableException {

    private final String serverQualifier;

    public ServerUnavailableException(String serverQualifier) {
        super(String.format("The server %s is unavailable. Please try again later.", serverQualifier));
        this.serverQualifier = serverQualifier;
    }

    @Override
    public Status.Code getStatusCode() {
        return Status.Code.UNAVAILABLE;
    }
}
