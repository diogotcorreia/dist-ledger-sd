package pt.tecnico.distledger.server.exceptions;

import io.grpc.Status;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true)
@Getter
public class CannotRemoveNotEmptyAccountException extends Exception implements GrpcSerializableException {

    private final String accountId;
    private final int balance;

    public CannotRemoveNotEmptyAccountException(String accountId, int balance) {
        super(String.format("Account '%s' cannot be removed, as its balance (%d) is not zero.", accountId, balance));
        this.accountId = accountId;
        this.balance = balance;
    }

    @Override
    public Status.Code getStatusCode() {
        return Status.Code.FAILED_PRECONDITION;
    }
}
