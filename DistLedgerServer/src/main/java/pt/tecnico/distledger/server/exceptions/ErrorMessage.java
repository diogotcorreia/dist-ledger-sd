package pt.tecnico.distledger.server.exceptions;

public enum ErrorMessage {

    ACCOUNT_NOT_FOUND("Account not found"),
    ACCOUNT_ALREADY_EXISTS("Account already exists"),

    INSUFFICIENT_AMOUNT("Insufficient amount to transfer"),
    UNEXPECTED_ERROR("Unexpected error");

    public final String message;

    ErrorMessage(String message) {
        this.message = message;
    }
}
