package adl_2daa.gen.encoder;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import adl_2daa.ast.ASTExpression;
import adl_2daa.ast.expression.ASTBinary;
import adl_2daa.ast.expression.ASTUnary;
import adl_2daa.ast.expression.Function;
import adl_2daa.ast.expression.StringConstant;
import adl_2daa.gen.FunctionMainSignature;
import adl_2daa.gen.GeneratorRegistry;
import adl_2daa.gen.Utility;


public class ADLExpressionEncoder {
	
	public static final ADLExpressionEncoder instance = new ADLExpressionEncoder();
	
	private List<Byte> buf = new LinkedList<Byte>();
	
	public String encode(ASTExpression astExp){
		buf.clear();
		encodeRecursively(astExp, 0);
		return new String(Utility.toByteArray(buf), StandardCharsets.US_ASCII);
	}
	
	private void encodeRecursively(ASTExpression astExp, int depth){
		buf.add((byte)depth);
		if(astExp instanceof ASTUnary){
			buf.add((byte)123);
			encodeRecursively(((ASTUnary)astExp).getNode(), depth+1);
		}else if(astExp instanceof ASTBinary){
			buf.add((byte)122);
			encodeRecursively(((ASTBinary)astExp).getLeft(), depth+1);
			encodeRecursively(((ASTBinary)astExp).getRight(), depth+1);
		}else if(astExp instanceof Function){
			encodeFunction((Function)astExp, depth);
		}else{
			buf.add((byte)120);
		}
	}
	
	private void encodeFunction(Function function, int depth){
		buf.add((byte)121);
		FunctionMainSignature sig = GeneratorRegistry.getFunctionSignature(function.getName());
		byte functionID = (byte)sig.getMainSignature().getId();
		if(sig.hasChoice()){
			String choice = ((StringConstant)function.getParams()[0]).getValue();
			functionID = (byte)sig.getChoiceSignature(choice).getId();
		}
		if(function.hasSingleQuery()){
			functionID = (byte)-functionID;
		}
		buf.add(functionID);
		byte branchingIndex = 65;
		for(ASTExpression astExp : function.getParams()){
			buf.add(branchingIndex);
			encodeRecursively(astExp, depth+1);
			branchingIndex++;
		}
	}
}
