package adl_2daa.gen.encoder;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import adl_2daa.ast.ASTExpression;
import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.expression.StringConstant;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.statement.Condition;
import adl_2daa.ast.statement.Loop;
import adl_2daa.gen.generator.ExpressionSkeleton;
import adl_2daa.gen.signature.Datatype;
import adl_2daa.gen.signature.GeneratorRegistry;
import adl_2daa.gen.signature.Signature;

public class ADLSequenceDecoder {

	public static final ADLSequenceDecoder instance = new ADLSequenceDecoder();
	
	private List<ASTStatement> result = new LinkedList<ASTStatement>();
	private byte[] buf;
	
	/**
	 * Decode given encoded actions as separated statement
	 */
	public List<ASTStatement> decode(List<String> seq){
		result.clear();
		for(String eAct : seq){
			result.add(decodeSingleAction(eAct));
		}
		return result;
	}
	
	private ASTStatement decodeSingleAction(String action){
		buf = action.getBytes(StandardCharsets.US_ASCII);
		List<ASTExpression> paramList = new LinkedList<ASTExpression>();
		
		int actionID = buf[0];
		String actionName = GeneratorRegistry.getActionName(actionID);
		String choice = null;
		Signature sig = null;
		int choiceSymbolIndex = actionName.indexOf('#');
		if(choiceSymbolIndex > -1){
			choice = actionName.substring(choiceSymbolIndex+1);
			actionName = actionName.substring(0, choiceSymbolIndex);
			sig = GeneratorRegistry.getActionSignature(actionName).getChoiceSignature(choice);
			//First param is a choice
			paramList.add(new StringConstant(choice));
		}else{
			sig = GeneratorRegistry.getActionSignature(actionName).getMainSignature();
		}
		
		for(Datatype expectingType : sig.getParamType()){
			paramList.add(new ExpressionSkeleton(expectingType));
		}
		Action astAction = new Action(actionName, paramList);
		
		return decodeStatement(1, astAction);
	}
	
	/**
	 * Decode a single statement stored in field "buf" starting from "startIndex"
	 */
	private ASTStatement decodeStatement(int startIndex, ASTStatement innermostContent){
		if(startIndex >= buf.length-1){
			return innermostContent;
		}

		//Find current block of encoded statement
		int endIndex = startIndex+1;
		while(endIndex < buf.length){
			if(buf[endIndex] == EncodeTable.LOOP || 
					buf[endIndex] == EncodeTable.COND_IF || 
					buf[endIndex] == EncodeTable.COND_IF_IFELSE ||
					buf[endIndex] == EncodeTable.COND_ELSE_IFELSE){
				break;
			}
			endIndex++;
		}
		
		//Decode inner content of current statement
		List<ASTStatement> list = new LinkedList<ASTStatement>();
		list.add(decodeStatement(endIndex, innermostContent));
		
		//Decode this statement and attach inner content as its child
		if(buf[startIndex] == EncodeTable.LOOP){
			return new Loop(decodeExpression(startIndex+1, endIndex, Datatype.INT), list);
		}else{
			ASTExpression exp = decodeExpression(startIndex+1, endIndex, Datatype.BOOL);
			switch(buf[startIndex]){
			case EncodeTable.COND_IF:
				return new Condition(exp, list, null);
			case EncodeTable.COND_IF_IFELSE:
				return new Condition(exp, list, new LinkedList<ASTStatement>());
			case EncodeTable.COND_ELSE_IFELSE:
				return new Condition(exp, new LinkedList<ASTStatement>(), list);
			}
		}
		return null;
	}
	
	private ASTExpression decodeExpression(int startIndex, int endIndex, 
			Datatype expectingType){
		byte[] data = new byte[endIndex-startIndex];
		for(int i=startIndex, j=0; i<endIndex; i++, j++){
			data[j] = buf[i];
		}
		return ADLExpressionDecoder.instance.decode(data, expectingType);
	}
}
