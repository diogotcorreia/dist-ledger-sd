package pt.tecnico.distledger.common.exceptions;

public class ServerUnresolvableException extends Exception {

    public ServerUnresolvableException(String qualifier) {
        super(String.format("Cannot resolve server with qualifier '%s'", qualifier));
    }
}
