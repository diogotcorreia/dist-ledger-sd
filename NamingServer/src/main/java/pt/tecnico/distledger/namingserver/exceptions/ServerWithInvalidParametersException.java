package pt.tecnico.distledger.namingserver.exceptions;

import io.grpc.Status;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true)
@Getter
public class ServerWithInvalidParametersException extends Exception implements GrpcSerializableException {

    private final String qualifier;

    public ServerWithInvalidParametersException(String qualifier) {
        super(String.format("Server with invalid qualifier: %s", qualifier));
        this.qualifier = qualifier;
    }

    @Override
    public Status.Code getStatusCode() {
        return Status.Code.INVALID_ARGUMENT;
    }

}
