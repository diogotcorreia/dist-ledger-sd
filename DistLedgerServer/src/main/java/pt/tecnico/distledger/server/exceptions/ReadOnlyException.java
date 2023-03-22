package pt.tecnico.distledger.server.exceptions;

import io.grpc.Status;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true)
@Getter
public class ReadOnlyException extends Exception implements GrpcSerializableException {

    public ReadOnlyException() {
        super("Read-only server. Try again in Server A");
    }

    @Override
    public Status.Code getStatusCode() {
        return Status.Code.PERMISSION_DENIED;
    }
}
