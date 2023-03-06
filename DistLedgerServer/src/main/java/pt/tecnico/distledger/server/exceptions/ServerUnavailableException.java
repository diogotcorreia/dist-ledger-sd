package pt.tecnico.distledger.server.exceptions;

import io.grpc.Status;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true)
@Getter
public class ServerUnavailableException extends Exception implements GrpcSerializableException {

    private final String accountId;

    public ServerUnavailableException(String accountId) {
        super("The server is unavailable. Please try again later.");
        this.accountId = accountId;
    }

    @Override
    public Status.Code getStatusCode() {
        return Status.Code.FAILED_PRECONDITION;
    }
}
