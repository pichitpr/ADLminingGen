package adl_2daa.gen.encoder;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import adl_2daa.ast.ASTExpression;
import adl_2daa.ast.expression.ASTBinary;
import adl_2daa.ast.expression.ASTUnary;
import adl_2daa.ast.expression.And;
import adl_2daa.ast.expression.Arithmetic;
import adl_2daa.ast.expression.Comparison;
import adl_2daa.ast.expression.Function;
import adl_2daa.ast.expression.Or;
import adl_2daa.ast.expression.StringConstant;
import adl_2daa.gen.Utility;
import adl_2daa.gen.signature.FunctionMainSignature;
import adl_2daa.gen.signature.GeneratorRegistry;


public class ADLExpressionEncoder {
	
	public static final ADLExpressionEncoder instance = new ADLExpressionEncoder();
	
	private List<Byte> buf = new LinkedList<Byte>();
	
	public String encode(ASTExpression astExp){
		buf.clear();
		encodeRecursively(astExp);
		return new String(Utility.toByteArray(buf), StandardCharsets.US_ASCII);
	}
	
	private void encodeRecursively(ASTExpression astExp){
		if(astExp instanceof ASTUnary){
			encodeUnary((ASTUnary)astExp);
		}else if(astExp instanceof ASTBinary){
			encodeBinary((ASTBinary)astExp);
		}else if(astExp instanceof Function){
			encodeFunction((Function)astExp);
		}else{
			buf.add(EncodeTable.EXP_LITERAL);
		}
	}
	
	private void encodeUnary(ASTUnary astUnary){
		if(astUnary.op == ASTUnary.UnaryOp.NOT){
			buf.add(EncodeTable.EXP_UNARY_NOT);
		}else{
			buf.add(EncodeTable.EXP_UNARY_NEG);
		}
		encodeRecursively(astUnary.getNode());
	}
	
	private void encodeBinary(ASTBinary astBinary){
		buf.add(EncodeTable.EXP_BINARY);
		if(astBinary instanceof And){
			buf.add(EncodeTable.EXP_BINARY_AND);
		}else if(astBinary instanceof Or){
			buf.add(EncodeTable.EXP_BINARY_OR);
		}else if(astBinary instanceof Comparison){
			switch(((Comparison)astBinary).getOp()){
			case EQ:
				buf.add(EncodeTable.EXP_BINARY_COMP_EQ);
				break;
			case NEQ:
				buf.add(EncodeTable.EXP_BINARY_COMP_NEQ);
				break;
			case GT:
				buf.add(EncodeTable.EXP_BINARY_COMP_GT);
				break;
			case GE:
				buf.add(EncodeTable.EXP_BINARY_COMP_GE);
				break;
			case LT:
				buf.add(EncodeTable.EXP_BINARY_COMP_LT);
				break;
			case LE:
				buf.add(EncodeTable.EXP_BINARY_COMP_LE);
				break;
			}
		}else if(astBinary instanceof Arithmetic){
			switch(((Arithmetic)astBinary).getOp()){
			case ADD:
				buf.add(EncodeTable.EXP_BINARY_ARITH_ADD);
				break;
			case SUB:
				buf.add(EncodeTable.EXP_BINARY_ARITH_SUB);
				break;
			case MUL:
				buf.add(EncodeTable.EXP_BINARY_ARITH_MUL);
				break;
			case DIV:
				buf.add(EncodeTable.EXP_BINARY_ARITH_DIV);
				break;
			case MOD:
				buf.add(EncodeTable.EXP_BINARY_ARITH_MOD);
				break;
			}
		}
		encodeRecursively(astBinary.getLeft());
		encodeRecursively(astBinary.getRight());
	}
	
	private void encodeFunction(Function function){
		buf.add(EncodeTable.EXP_FUNCTION);
		FunctionMainSignature sig = GeneratorRegistry.getFunctionSignature(function.getName());
		int functionID = (byte)sig.getMainSignature().getId();
		//byte paramsLength = (byte)sig.getMainSignature().getParamType().length;
		if(sig.hasChoice()){
			String choice = ((StringConstant)function.getParams()[0]).getValue();
			functionID = sig.getChoiceSignature(choice).getId();
			//paramsLength = (byte)sig.getChoiceSignature(choice).getParamType().length;
		}
		if(function.hasSingleQuery()){
			functionID = -functionID;
		}
		byte[] encodedFunction = EncodeTable.encodeSignatureID(functionID);
		for(byte b : encodedFunction){
			buf.add(b);
		}
		//buf.add(paramsLength); //TODO: later work, remove paramCount if not used
		for(int i=0; i<function.getParams().length; i++){
			if(i == 0 && sig.hasChoice()) continue;
			encodeRecursively(function.getParams()[i]);
		}
	}
}
