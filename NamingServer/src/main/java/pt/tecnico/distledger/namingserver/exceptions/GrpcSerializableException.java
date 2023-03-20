package pt.tecnico.distledger.namingserver.exceptions;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public interface GrpcSerializableException {

    default StatusRuntimeException toGrpcRuntimeException() {
        return Status.fromCode(getStatusCode()).withDescription(getMessage()).asRuntimeException();
    }

    Status.Code getStatusCode();

    String getMessage();

}
