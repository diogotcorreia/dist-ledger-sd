package pt.tecnico.distledger.namingserver.exceptions;

import io.grpc.Status;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import pt.tecnico.distledger.namingserver.domain.ServerAddress;

@EqualsAndHashCode(callSuper = true)
@Getter
public class ServerDoesNotExistException extends Exception implements GrpcSerializableException {

    public ServerDoesNotExistException(ServerAddress address, String serviceName) {
        super(String.format("Not possible to remove the server %s from service %s", address, serviceName));
    }

    @Override
    public Status.Code getStatusCode() {
        return Status.Code.INVALID_ARGUMENT;
    }

}
