package adl_2daa.gen.encoder;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import adl_2daa.ast.ASTExpression;
import adl_2daa.ast.expression.ASTBinary;
import adl_2daa.ast.expression.ASTUnary;
import adl_2daa.ast.expression.ASTUnary.UnaryOp;
import adl_2daa.ast.expression.And;
import adl_2daa.ast.expression.Arithmetic;
import adl_2daa.ast.expression.Arithmetic.MathOp;
import adl_2daa.ast.expression.Comparison;
import adl_2daa.ast.expression.Comparison.Comp;
import adl_2daa.ast.expression.Function;
import adl_2daa.ast.expression.Or;
import adl_2daa.ast.expression.StringConstant;
import adl_2daa.gen.generator.LiteralSkeleton;
import adl_2daa.gen.signature.Datatype;
import adl_2daa.gen.signature.GeneratorRegistry;
import adl_2daa.gen.signature.Signature;

public class ADLExpressionDecoder {

	public static final ADLExpressionDecoder instance = new ADLExpressionDecoder();
	
	private byte[] buf;
	private int bufPointer;
	
	public ASTExpression decode(String exp, Datatype expectingType){
		buf = exp.getBytes(StandardCharsets.US_ASCII);
		bufPointer = -1;
		return decodeRecursively(expectingType);
	}
	
	private ASTExpression decodeRecursively(Datatype parentExpectingType){
		bufPointer++;
		switch(buf[bufPointer]){
		case EncodeTable.EXP_UNARY_NEG:
			return new ASTUnary(UnaryOp.NEG, decodeRecursively(Datatype.DECIMAL));
		case EncodeTable.EXP_UNARY_NOT:
			return new ASTUnary(UnaryOp.NOT, decodeRecursively(Datatype.BOOL));
		case EncodeTable.EXP_BINARY:
			return decodeBinaryExp();
		case EncodeTable.EXP_FUNCTION:
			return decodeFunction();
		case EncodeTable.EXP_LITERAL:
			return new LiteralSkeleton(parentExpectingType);
		}
		return null;
	}
	
	private ASTBinary decodeBinaryExp(){
		bufPointer++;
		byte binOp = buf[bufPointer];
		ASTExpression left = null;
		ASTExpression right = null;
		if(binOp == EncodeTable.EXP_BINARY_AND || binOp == EncodeTable.EXP_BINARY_OR){
			left = decodeRecursively(Datatype.BOOL);
			right = decodeRecursively(Datatype.BOOL);
		}else{
			left = decodeRecursively(Datatype.DECIMAL);
			right = decodeRecursively(Datatype.DECIMAL);
		}
		switch(binOp){
		case EncodeTable.EXP_BINARY_AND:
			return new And(left, right);
		case EncodeTable.EXP_BINARY_OR:
			return new Or(left, right);
		case EncodeTable.EXP_BINARY_COMP_EQ:
			return new Comparison(left, Comp.EQ, right);
		case EncodeTable.EXP_BINARY_COMP_NEQ:
			return new Comparison(left, Comp.NEQ, right);
		case EncodeTable.EXP_BINARY_COMP_GT:
			return new Comparison(left, Comp.GT, right);
		case EncodeTable.EXP_BINARY_COMP_LT:
			return new Comparison(left, Comp.LT, right);
		case EncodeTable.EXP_BINARY_COMP_GE:
			return new Comparison(left, Comp.GE, right);
		case EncodeTable.EXP_BINARY_COMP_LE:
			return new Comparison(left, Comp.LE, right);
		case EncodeTable.EXP_BINARY_ARITH_ADD:
			return new Arithmetic(left, MathOp.ADD, right);
		case EncodeTable.EXP_BINARY_ARITH_SUB:
			return new Arithmetic(left, MathOp.SUB, right);
		case EncodeTable.EXP_BINARY_ARITH_MUL:
			return new Arithmetic(left, MathOp.MUL, right);
		case EncodeTable.EXP_BINARY_ARITH_DIV:
			return new Arithmetic(left, MathOp.DIV, right);
		case EncodeTable.EXP_BINARY_ARITH_MOD:
			return new Arithmetic(left, MathOp.MOD, right);
		}
		return null;
	}
	
	private ASTExpression decodeFunction(){
		bufPointer++;
		byte id0 = buf[bufPointer];
		bufPointer++;
		byte id1 = buf[bufPointer];
		int functionID = EncodeTable.decodeSignatureID(id0, id1);
		boolean hasSingleQuery = functionID < 0;
		if(hasSingleQuery) functionID = -functionID;
		List<ASTExpression> paramList = new LinkedList<ASTExpression>();
		
		String functionName = GeneratorRegistry.getFunctionName(functionID);
		String choice = null;
		Signature sig = null;
		int choiceSymbolIndex = functionName.indexOf('#');
		if(choiceSymbolIndex > -1){
			choice = functionName.substring(choiceSymbolIndex+1);
			functionName = functionName.substring(0, choiceSymbolIndex);
			sig = GeneratorRegistry.getFunctionSignature(functionName).getChoiceSignature(choice);
			//First param is a choice
			paramList.add(new StringConstant(choice));
		}else{
			sig = GeneratorRegistry.getFunctionSignature(functionName).getMainSignature();
		}
		
		for(Datatype paramExpectingType : sig.getParamType()){
			paramList.add(decodeRecursively(paramExpectingType));
		}
		
		return new Function(functionName, paramList, hasSingleQuery);
	}
}
