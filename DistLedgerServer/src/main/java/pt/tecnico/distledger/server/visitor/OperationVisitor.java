package pt.tecnico.distledger.server.visitor;

import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;

public abstract class OperationVisitor {

    public abstract void visit(CreateOp operation);

    public abstract void visit(TransferOp operation);

}
