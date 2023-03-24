package pt.tecnico.distledger.namingserver.exceptions;

import io.grpc.Status;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import pt.tecnico.distledger.namingserver.domain.ServerAddress;

@EqualsAndHashCode(callSuper = true)
@Getter
public class ServerEntryAlreadyExistsException extends Exception implements GrpcSerializableException {

    public ServerEntryAlreadyExistsException(ServerAddress address, String qualifier, String serviceName) {
        super(
                String.format(
                        "Not possible to register the server %s with qualifier %s on service %s",
                        address,
                        qualifier,
                        serviceName
                )
        );
    }

    @Override
    public Status.Code getStatusCode() {
        return Status.Code.ALREADY_EXISTS;
    }

}
