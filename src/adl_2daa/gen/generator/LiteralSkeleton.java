package adl_2daa.gen.generator;

import java.util.List;

import adl_2daa.ast.ASTExpression;
import adl_2daa.gen.signature.Datatype;
import adl_2daa.internal.Instruction;
import adl_2daa.tool.ADLCompiler;

public class LiteralSkeleton extends ASTExpression{
	
	private Datatype type;
	
	public LiteralSkeleton(Datatype type) {
		super();
		this.type = type;
	}

	public Datatype getType(){
		return this.type;
	}
	
	@Override
	public void compile(List<Instruction> ins, ADLCompiler compiler) {}

	@Override
	public void toScript(StringBuilder str, int indent) {}
}
