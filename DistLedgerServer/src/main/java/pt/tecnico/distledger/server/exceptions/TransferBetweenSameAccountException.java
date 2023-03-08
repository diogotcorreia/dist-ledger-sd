package pt.tecnico.distledger.server.exceptions;

import io.grpc.Status;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true)
@Getter
public class TransferBetweenSameAccountException extends Exception implements GrpcSerializableException {

    private final String fromAccountId;
    private final String toAccountId;

    public TransferBetweenSameAccountException(String fromAccountId, String toAccountId) {
        super(
                String.format(
                        "It is not possible to create a transfer between the same account (from '%s' to '%s').",
                        fromAccountId,
                        toAccountId
                )
        );
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
    }

    @Override
    public Status.Code getStatusCode() {
        return Status.Code.FAILED_PRECONDITION;
    }
}
