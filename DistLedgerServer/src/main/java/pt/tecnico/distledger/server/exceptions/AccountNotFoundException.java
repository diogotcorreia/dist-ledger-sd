package pt.tecnico.distledger.server.exceptions;

import io.grpc.Status;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true)
@Getter
public class AccountNotFoundException extends Exception implements GrpcSerializableException {

    private final String accountId;

    public AccountNotFoundException(String accountId) {
        super(String.format("Account '%s' not found", accountId));
        this.accountId = accountId;
    }

    @Override
    public Status.Code getStatusCode() {
        return Status.Code.INVALID_ARGUMENT;
    }
}
