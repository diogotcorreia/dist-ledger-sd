package pt.tecnico.distledger.server.exceptions;

import io.grpc.Status;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true)
@Getter
public class AccountAlreadyExistsException extends Exception implements GrpcSerializableException {

    private final String accountId;

    public AccountAlreadyExistsException(String accountId) {
        super(String.format("Account '%s' already exists", accountId));
        this.accountId = accountId;
    }

    @Override
    public Status.Code getStatusCode() {
        return Status.Code.FAILED_PRECONDITION;
    }
}
