package pt.tecnico.distledger.server.service;

import pt.tecnico.distledger.common.VectorClock;

public record OperationOutput<T> (T value, VectorClock updatedTS) {
}
