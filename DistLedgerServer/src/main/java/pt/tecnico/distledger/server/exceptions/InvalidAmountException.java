package pt.tecnico.distledger.server.exceptions;

import io.grpc.Status;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true)
@Getter
public class InvalidAmountException extends Exception implements GrpcSerializableException {

    private final int amount;

    public InvalidAmountException(int amount) {
        super(String.format("The given amount (%s) is a non-positive number", amount));
        this.amount = amount;
    }

    @Override
    public Status.Code getStatusCode() {
        return Status.Code.INVALID_ARGUMENT;
    }
}
