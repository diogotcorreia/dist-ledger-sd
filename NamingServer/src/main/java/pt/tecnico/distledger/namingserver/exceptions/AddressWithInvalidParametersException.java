package pt.tecnico.distledger.namingserver.exceptions;

import io.grpc.Status;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true)
@Getter
public class AddressWithInvalidParametersException extends Exception implements GrpcSerializableException {

    private final String host;
    private final int port;

    public AddressWithInvalidParametersException(String host, int port) {
        super(String.format("Server Address with invalid parameters: %s:%d", host, port));
        this.host = host;
        this.port = port;
    }

    @Override
    public Status.Code getStatusCode() {
        return Status.Code.INVALID_ARGUMENT;
    }

}
