package pt.tecnico.distledger.server.exceptions;

import io.grpc.Status;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true)
@Getter
public class InsufficientFundsException extends Exception implements GrpcSerializableException {

    private final String accountId;
    private final int expectedFunds;
    private final int availableFunds;

    public InsufficientFundsException(String accountId, int expectedFunds, int availableFunds) {
        super(
                String.format(
                        "Account '%s' does not have enough funds (expected %d but only %d are available)",
                        accountId,
                        expectedFunds,
                        availableFunds
                )
        );
        this.accountId = accountId;
        this.expectedFunds = expectedFunds;
        this.availableFunds = availableFunds;
    }

    @Override
    public Status.Code getStatusCode() {
        return Status.Code.FAILED_PRECONDITION;
    }
}
