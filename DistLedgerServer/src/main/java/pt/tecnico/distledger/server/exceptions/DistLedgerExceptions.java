package pt.tecnico.distledger.server.exceptions;

public class DistLedgerExceptions extends RuntimeException {

    private final ErrorMessage errorMessage;

    public DistLedgerExceptions(ErrorMessage errorMessage) {
        this.errorMessage = errorMessage;
    }

    public ErrorMessage getErrorMessage() {
        return errorMessage;
    }
}
