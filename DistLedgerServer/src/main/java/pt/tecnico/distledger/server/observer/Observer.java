package pt.tecnico.distledger.server.observer;

import pt.tecnico.distledger.common.VectorClock;
import pt.tecnico.distledger.server.visitor.OperationVisitor;

public interface Observer {
    boolean update(OperationVisitor visitor, VectorClock valueTimestamp);
}
