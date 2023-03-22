package pt.tecnico.distledger.server.exceptions;

import io.grpc.Status;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true)
@Getter
public class PropagationException extends Exception implements GrpcSerializableException {

    public PropagationException() {
        super("Could not propagate state. Please try again later.");
    }

    @Override
    public Status.Code getStatusCode() {
        return Status.Code.UNAVAILABLE;
    }
}
