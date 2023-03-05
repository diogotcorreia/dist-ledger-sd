package pt.tecnico.distledger.server.exceptions;

import lombok.Getter;

@Getter
public class DistLedgerExceptions extends RuntimeException {

    private final ErrorMessage errorMessage;

    public DistLedgerExceptions(ErrorMessage errorMessage) {
        this.errorMessage = errorMessage;
    }
}
