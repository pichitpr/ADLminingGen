package adl_2daa.gen.generator;

import java.util.List;

import adl_2daa.ast.ASTExpression;
import adl_2daa.ast.expression.ASTUnary;
import adl_2daa.ast.expression.And;
import adl_2daa.ast.expression.Arithmetic;
import adl_2daa.ast.expression.Bitwise;
import adl_2daa.ast.expression.BooleanConstant;
import adl_2daa.ast.expression.Comparison;
import adl_2daa.ast.expression.FloatConstant;
import adl_2daa.ast.expression.Function;
import adl_2daa.ast.expression.Identifier;
import adl_2daa.ast.expression.IntConstant;
import adl_2daa.ast.expression.Or;
import adl_2daa.ast.expression.StringConstant;
import adl_2daa.gen.signature.Datatype;
import adl_2daa.gen.signature.FunctionMainSignature;
import adl_2daa.gen.signature.GeneratorRegistry;
import adl_2daa.internal.Instruction;
import adl_2daa.tool.ADLCompiler;

public class ExpressionSkeleton extends ASTExpression{
	
	private Datatype type;
	
	public ExpressionSkeleton(Datatype type) {
		super();
		this.type = type;
	}

	public Datatype getType(){
		return this.type;
	}
	
	@Override
	public void compile(List<Instruction> ins, ADLCompiler compiler) {}

	@Override
	public void toScript(StringBuilder str, int indent) {
		str.append("#"+type.name());
	}
	
	//This method reflects SequenceInterpreter behavior
	public boolean isCompatibleWith(ASTExpression exp){		
		if(exp instanceof ASTUnary){
			//The interpreter is more forgiving but we will be more strict when generating
			//to create highest potential for reasonable behavior
			ASTUnary uop = (ASTUnary)exp;
			switch(uop.op){
			case NEG:
				return type == Datatype.INT || type == Datatype.DECIMAL;
			case NOT:
				return type == Datatype.BOOL;
			}
			return false; //Unknown unary operator (should not reach here!)
		}
		if(exp instanceof And || exp instanceof Or){
			return type == Datatype.BOOL;
		}
		if(exp instanceof Arithmetic || exp instanceof Bitwise){
			return type == Datatype.INT || type == Datatype.DECIMAL;
		}
		if(exp instanceof Comparison){
			return type == Datatype.BOOL;
		}
		if(exp instanceof BooleanConstant) 
			return type == Datatype.BOOL;
		if(exp instanceof IntConstant || exp instanceof FloatConstant)
			//It is possible to use DECIMAL type for INT param and get meaningful behavior
			return type == Datatype.INT || type == Datatype.DECIMAL;
		if(exp instanceof StringConstant)
			//These type must be typed in string in ADL -- actually IDENTIFIER can also
			//be string (interpreter knows) but we opt out for the sake of reasonable behavior
			return type == Datatype.CHOICE || type == Datatype.POSITION || 
					type == Datatype.DIRECTION;
		if(exp instanceof Identifier)
			return type == Datatype.IDENTIFIER;
		
		if(exp instanceof Function){
			Function func = (Function)exp;
			FunctionMainSignature sig = GeneratorRegistry.getFunctionSignature(func.getName());
			Datatype returnType = sig.getMainSignature().getReturnType();
			if(sig.hasChoice()){
				//Since choice is pseudo function name, it should always present
				assert(func.getParams().length >= 1);
				assert(func.getParams()[0] instanceof StringConstant);
				String choice = ((StringConstant)func.getParams()[0]).getValue();
				returnType = sig.getChoiceSignature(choice).getReturnType();
			}
			
			//Special case for ABSTRACT type (ONLY Random(..) use this type)
			if(type == Datatype.ABSTRACT_SET){
				return returnType == Datatype.DECIMAL_SET ||
						returnType == Datatype.DIRECTION_SET ||
						returnType == Datatype.DYNAMIC_SET;
			}
			if(returnType == Datatype.ABSTRACT){
				//All possible type returned from Random(..)
				return type == Datatype.DECIMAL || type == Datatype.DIRECTION ||
						type == Datatype.DYNAMIC;
			}
			
			return type == returnType;
		}
			
		//Should not reach here!
		return false;
	}
}
