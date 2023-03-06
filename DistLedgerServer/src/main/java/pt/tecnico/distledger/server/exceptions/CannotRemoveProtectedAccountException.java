package pt.tecnico.distledger.server.exceptions;

import io.grpc.Status;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true)
@Getter
public class CannotRemoveProtectedAccountException extends Exception implements GrpcSerializableException {

    private final String accountId;

    public CannotRemoveProtectedAccountException(String accountId) {
        super(String.format("Account '%s' is protected. It cannot be removed", accountId));
        this.accountId = accountId;
    }

    @Override
    public Status.Code getStatusCode() {
        return Status.Code.PERMISSION_DENIED;
    }
}
