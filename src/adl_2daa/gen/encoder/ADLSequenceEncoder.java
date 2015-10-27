package adl_2daa.gen.encoder;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.expression.Identifier;
import adl_2daa.ast.expression.StringConstant;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.statement.Condition;
import adl_2daa.ast.statement.Loop;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.gen.signature.ActionMainSignature;
import adl_2daa.gen.signature.GeneratorRegistry;

public class ADLSequenceEncoder {

	public static final ADLSequenceEncoder instance = new ADLSequenceEncoder();
	public static final String dummyEncodedAction = new String(
			new byte[]{
					(byte)GeneratorRegistry.getDummyActionSignature().getMainSignature().getId()
					}, 
			StandardCharsets.US_ASCII);
	public static final List<String> dummyEncodedSequence;
	static{
		dummyEncodedSequence = new LinkedList<String>();
		dummyEncodedSequence.add(ADLSequenceEncoder.dummyEncodedAction);
	}
	
	private Stack<String> expressionBuf = new Stack<String>();
	private Stack<Byte> separatorBuf = new Stack<Byte>();
	private List<String> result;
	
	private List<String> allSpawnableAgent;
	
	private boolean analyzeFlow = false;
	private FlowAnalyzableTree analyzedNode;
	/**
	 * All possible flows to terminal (goto/despawn), the terminal action is discard
	 * from the sequence. Terminal type (goto/despawn) can be checked by examining sequence's
	 * identifier
	 */
	private List<ADLSequence> allPossibleFlowToTerminal;
	
	public void setAnalyzeFlow(boolean analyzeFlow) {
		this.analyzeFlow = analyzeFlow;
	}

	public ADLSequence encode(Sequence sequence){
		expressionBuf.clear();
		separatorBuf.clear();
		result = new LinkedList<String>();
		allSpawnableAgent = new LinkedList<String>();
		if(analyzeFlow){
			analyzedNode = new FlowAnalyzableTree();
			allPossibleFlowToTerminal = new LinkedList<ADLSequence>();
		}
		encodeRecursively(sequence.getStatements());
		
		ADLSequence resultSequence = new ADLSequence(sequence.getIdentifier(), result);
		resultSequence.allSpawnableAgent = allSpawnableAgent;
		if(analyzeFlow){
			resultSequence.allFlowToTerminal = allPossibleFlowToTerminal;
		}
		return resultSequence;
	}
	
	private void encodeRecursively(List<ASTStatement> statements){
		for(ASTStatement st : statements){
			if(st instanceof Action){
				encodeAction((Action)st);
			}else if(st instanceof Condition){
				if(analyzeFlow){
					encodeConditionAndAnalyzeFlow((Condition)st);
				}else{
					encodeCondition((Condition)st);
				}
			}else if(st instanceof Loop){
				encodeLoop((Loop)st);
			}
		}
	}
	
	private void encodeAction(Action action){
		ActionMainSignature sig = GeneratorRegistry.getActionSignature(action.getName());
		byte actionID = (byte)sig.getMainSignature().getId();
		if(sig.hasChoice()){
			String choice = ((StringConstant)action.getParams()[0]).getValue();
			actionID = (byte)sig.getChoiceSignature(choice).getId();
		}
		
		String enc = new String(new byte[]{actionID}, StandardCharsets.US_ASCII);
		for(int i=0; i<expressionBuf.size(); i++){
			enc += (char)separatorBuf.get(i).byteValue();
			enc += expressionBuf.get(i);
		}
		result.add(enc);
		
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
			List<String> eSeqtoTerminal = new LinkedList<String>();
			analyzedNode.createFlowAndTrim(eSeqtoTerminal);
			Collections.reverse(eSeqtoTerminal);
			allPossibleFlowToTerminal.add(new ADLSequence(targetState, eSeqtoTerminal));
		}else{
			analyzedNode.addAction(enc);
		}
	}
	
	private void encodeCondition(Condition cond){
		expressionBuf.push(ADLExpressionEncoder.instance.encode(cond.getCondition()));
		separatorBuf.push(cond.getElseblock() == null ? EncodeTable.COND_IF : 
				EncodeTable.COND_IF_IFELSE);
		encodeRecursively(cond.getIfblock());
		separatorBuf.pop();
		if(cond.getElseblock() != null){
			separatorBuf.push(EncodeTable.COND_ELSE_IFELSE);
			encodeRecursively(cond.getElseblock());
			separatorBuf.pop();
		}
		expressionBuf.pop();
	}
	
	private void encodeConditionAndAnalyzeFlow(Condition cond){
		expressionBuf.push(ADLExpressionEncoder.instance.encode(cond.getCondition()));
		
		FlowAnalyzableTree ifBlock,elseBlock = null;
		ifBlock = new FlowAnalyzableTree();
		if(cond.getElseblock() != null){
			elseBlock = new FlowAnalyzableTree();
		}
		analyzedNode.addBranching(ifBlock, elseBlock);
		FlowAnalyzableTree cachedLevel = analyzedNode;
		
		analyzedNode = ifBlock;

		separatorBuf.push(cond.getElseblock() == null ? EncodeTable.COND_IF : 
			EncodeTable.COND_IF_IFELSE);
		encodeRecursively(cond.getIfblock());
		separatorBuf.pop();
		
		if(cond.getElseblock() != null){
			analyzedNode = elseBlock;
			separatorBuf.push(EncodeTable.COND_ELSE_IFELSE);
			encodeRecursively(cond.getElseblock());
			separatorBuf.pop();
		}
		
		analyzedNode = cachedLevel;
		
		expressionBuf.pop();
	}
	
	private void encodeLoop(Loop loop){
		expressionBuf.push(ADLExpressionEncoder.instance.encode(loop.getLoopCount()));
		separatorBuf.push(EncodeTable.LOOP);
		encodeRecursively(loop.getContent());
		separatorBuf.pop();
		expressionBuf.pop();
	}
}
