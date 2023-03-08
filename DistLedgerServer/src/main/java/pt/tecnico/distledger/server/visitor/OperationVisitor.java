package pt.tecnico.distledger.server.visitor;

import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;

public abstract class OperationVisitor<T> {

    public abstract T visit(CreateOp operation);

    public abstract T visit(DeleteOp operation);

    public abstract T visit(TransferOp operation);

}
