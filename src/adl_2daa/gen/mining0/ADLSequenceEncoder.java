package adl_2daa.gen.mining0;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import adl_2daa.ast.ASTExpression;
import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.expression.ASTBinary;
import adl_2daa.ast.expression.ASTUnary;
import adl_2daa.ast.expression.And;
import adl_2daa.ast.expression.Comparison;
import adl_2daa.ast.expression.Function;
import adl_2daa.ast.expression.Identifier;
import adl_2daa.ast.expression.Or;
import adl_2daa.ast.expression.StringConstant;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.statement.Condition;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.gen.signature.ActionMainSignature;
import adl_2daa.gen.signature.FunctionMainSignature;
import adl_2daa.gen.signature.GeneratorRegistry;
import adl_2daa.gen.signature.Utility;

public class ADLSequenceEncoder {
	
	protected static final ADLSequenceEncoder instance = new ADLSequenceEncoder();
	protected static final String impossibleAction = new String(new byte[]{0}, 
			StandardCharsets.US_ASCII); 
	
	//Buffer storing nesting conditions, will changes throughout parsing process
	private List<byte[]> nestingConditionsBuf = new ArrayList<byte[]>();
	
	private List<EncodedAction> eActList;
	private List<String> allSpawnableAgent;
	
	private boolean analyzeFlow = false;
	private FlowAnalyzableTree analyzedNode;
	private List<EncodedSequence> allPossibleFlows;
	
	/**
	 * Parse ASTSequence as an encoded sequence (the AST remains unchanged).
	 * The code also store all entities that can be spawned with this sequence.
	 * If performFlowAnalysis is true, all possible flows ended with state transition
	 * are also generated and stored in returned sequence.
	 * Generated flows are in EncodedSequence form with "TargetStateName" as an identifier.
	 * DO Note that all EncodedActions inside generated flows refers to 
	 * the same EncodedActions in the returning encoded sequence.  
	 */
	protected EncodedSequence parseAsEncodedSequence(Sequence astSequence, 
			boolean performFlowAnalysis){
		analyzeFlow = performFlowAnalysis;
		eActList = new ArrayList<EncodedAction>();
		allSpawnableAgent = new ArrayList<String>();
		if(analyzeFlow){
			analyzedNode = new FlowAnalyzableTree();
			allPossibleFlows = new ArrayList<EncodedSequence>();
		}
		for(ASTStatement st : astSequence.getStatements()){
			parseStatement(st, 0);
		}
		EncodedSequence eSeq = new EncodedSequence(astSequence.getIdentifier(), eActList);
		eSeq.allFlowToTransition = allPossibleFlows;
		eSeq.allSpawnableAgent = allSpawnableAgent;
		return eSeq;
	}
	
	private void parseStatement(ASTStatement st, int nestingLevel){
		if(st instanceof Action){
			parseAction((Action)st, nestingLevel);
		}else{
			parseConditionStruct((Condition)st, nestingLevel);
		}
	}
	
	private void parseAction(Action action, int nestingLevel){
		ActionMainSignature sig = GeneratorRegistry.getActionSignature(action.getName());
		byte actionID = (byte)sig.getMainSignature().getId();
		if(sig.hasChoice()){
			String choice = ((StringConstant)action.getParams()[0]).getValue();
			actionID = (byte)sig.getChoiceSignature(choice).getId();
		}
		
		/*
		int size = 1;
		int i;
		for(i=0; i<nestingLevel; i++){
			size += 1+nestingConditions.get(i).length;
		}
		
		byte[] encodedAction = new byte[size];
		encodedAction[0] = actionID;
		
		//Copy nesting conditions info
		int encodeIdx = 1;
		for(i=0; i<nestingLevel; i++){
			encodedAction[encodeIdx] = (byte)127;
			encodeIdx++;
			for(byte b : nestingConditions.get(i)){
				encodedAction[encodeIdx] = b;
				encodeIdx++;
			}
		}
		*/
		
		EncodedAction eAct = new EncodedAction(actionID, nestingConditionsBuf, 
				nestingLevel);
		eActList.add(eAct);
		
		if(action.getName().equals("Spawn")){
			String spawned = ((Identifier)action.getParams()[0]).getValue();
			allSpawnableAgent.add(spawned);
		}
		
		if(!analyzeFlow) return;
		if(action.getName().equals("Goto") || action.getName().equals("Despawn")){
			String targetState;
			if(action.getName().equals("Despawn")){
				targetState = "des";
			}else{
				targetState = ((Identifier)action.getParams()[0]).getValue();
			}
			List<EncodedAction> eSeqTrans = new LinkedList<EncodedAction>();
			eSeqTrans.add(eAct);
			//Add current flow and trim this branch from the tree
			//TODO: N dead transition beyond the first transition cause wrong N results
			analyzedNode.createFlowAndTrim(eSeqTrans);
			Collections.reverse(eSeqTrans);
			allPossibleFlows.add(new EncodedSequence(targetState, eSeqTrans));
		}else{
			analyzedNode.addAction(eAct);
		}
	}
	
	private void parseConditionStruct(Condition conditionStruct, int nestingLevel){
		byte[] condition = parseCondition(conditionStruct.getCondition());
		byte[] condInfo = new byte[condition.length+2];
		condInfo[1] = (byte)127;
		for(int i=0; i<condition.length; i++){
			condInfo[i+2] = condition[i];
		}
		if(nestingConditionsBuf.size() > nestingLevel){
			nestingConditionsBuf.set(nestingLevel, condInfo);
		}else{
			nestingConditionsBuf.add(condInfo);
		}
		
		if(!analyzeFlow){
			condInfo[0] = conditionStruct.getElseblock() == null ? (byte)70 : (byte)71;
			for(ASTStatement st : conditionStruct.getIfblock()){
				parseStatement(st, nestingLevel+1);
			}
			
			if(conditionStruct.getElseblock() != null){
				condInfo[0] = 72;
				for(ASTStatement st : conditionStruct.getElseblock()){
					parseStatement(st, nestingLevel+1);
				}
			}
		}else{
			FlowAnalyzableTree ifBlock,elseBlock = null;
			ifBlock = new FlowAnalyzableTree();
			if(conditionStruct.getElseblock() != null){
				elseBlock = new FlowAnalyzableTree();
			}
			analyzedNode.addBranching(ifBlock, elseBlock);
			FlowAnalyzableTree cachedLevel = analyzedNode;
			
			analyzedNode = ifBlock;
			condInfo[0] = conditionStruct.getElseblock() == null ? (byte)70 : (byte)71;
			for(ASTStatement st : conditionStruct.getIfblock()){
				parseStatement(st, nestingLevel+1);
			}
			
			if(conditionStruct.getElseblock() != null){
				analyzedNode = elseBlock;
				condInfo[0] = 72;
				for(ASTStatement st : conditionStruct.getElseblock()){
					parseStatement(st, nestingLevel+1);
				}
			}
			
			analyzedNode = cachedLevel;
		}
	}
	
	private byte[] parseCondition(ASTExpression condition){
		List<Byte> buf = new ArrayList<Byte>();
		parseExpression(condition, buf, 0);
		return Utility.toByteArray(buf);
	}
	
	private void parseExpression(ASTExpression exp, List<Byte> buf, int depth){
		buf.add((byte)(depth+101));
		if(exp instanceof Function){
			parseFunction((Function)exp, buf, depth);
		}else if(exp instanceof ASTBinary){
			parseBinary((ASTBinary)exp, buf, depth);
			/*
			if(exp instanceof Comparison){
				parseComparator((Comparison)exp, buf, depth);
			}else if((exp instanceof And) || (exp instanceof Or)){
				parseLogicOpExp((ASTBinary)exp, buf, depth);
			}else{
				parseMathOpExp((ASTBinary)exp, buf, depth);
			}
			*/
		}else if(exp instanceof ASTUnary){
			parseUnary((ASTUnary)exp, buf, depth);
		}else{ //Literal
			parseLiteral(exp, buf, depth);
		}
	}
	
	private void parseBinary(ASTBinary exp, List<Byte> buf, int depth){
		if(exp instanceof Comparison){
			buf.add((byte)85);
		}else if((exp instanceof And) || (exp instanceof Or)){
			buf.add((byte)73);
		}else{
			buf.add((byte)75);
		}
		buf.add((byte)121);
		parseExpression(exp.getLeft(), buf, depth+1);
		buf.add((byte)122);
		parseExpression(exp.getRight(), buf, depth+1);
	}
	
	/*
	private void parseComparator(Comparison compExp, List<Byte> buf, int depth){
		buf.add((byte)85);
		buf.add((byte)121);
		parseExpression(compExp.getLeft(), buf, depth+1);
		buf.add((byte)122);
		parseExpression(compExp.getRight(), buf, depth+1);
	}
	
	private void parseLogicOpExp(ASTBinary logicExp, List<Byte> buf, int depth){
		buf.add((byte)73);
		buf.add((byte)121);
		parseExpression(logicExp.getLeft(), buf, depth+1);
		buf.add((byte)122);
		parseExpression(logicExp.getRight(), buf, depth+1);
	}
	
	private void parseMathOpExp(ASTBinary mathExp, List<Byte> buf, int depth){
		buf.add((byte)75);
		buf.add((byte)121);
		parseExpression(mathExp.getLeft(), buf, depth+1);
		buf.add((byte)122);
		parseExpression(mathExp.getRight(), buf, depth+1);
	}
	*/
	
	private void parseUnary(ASTUnary unaryExp, List<Byte> buf, int depth){
		buf.add((byte)94);
		parseExpression(unaryExp.getNode(), buf, depth+1);
	}
	
	private void parseFunction(Function func, List<Byte> buf, int depth){
		FunctionMainSignature sig = GeneratorRegistry.getFunctionSignature(func.getName());
		byte functionID = (byte)sig.getMainSignature().getId();
		if(sig.hasChoice()){
			String choice = ((StringConstant)func.getParams()[0]).getValue();
			functionID = (byte)sig.getChoiceSignature(choice).getId();
		}
		buf.add(functionID);
		int i=121; //Starting branch index 0 = 121
		for(ASTExpression fExp : func.getParams()){
			buf.add((byte)i);
			parseExpression(fExp, buf, depth+1);
			i++;
		}
	}
	
	private void parseLiteral(ASTExpression litExp, List<Byte> buf, int depth){
		/*
		if(litExp instanceof Identifier){
			buf.add((byte)96);
			byte[] identifier = ((Identifier)litExp).getValue().
					getBytes(StandardCharsets.US_ASCII);
			buf.add((byte)identifier.length);
			for(byte b : identifier){
				buf.add(b);
			}
		}else{
			buf.add((byte)95);
		}
		*/
		buf.add((byte)95);
	}
}
