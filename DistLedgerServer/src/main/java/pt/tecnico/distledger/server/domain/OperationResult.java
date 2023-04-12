package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.common.VectorClock;

public record OperationResult<T>(T value, VectorClock vectorClock) {
}
