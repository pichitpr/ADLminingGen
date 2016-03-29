package adl_2daa.gen.generator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import spmf.extension.patterns.itemset_list_generic.ItemsetGen;
import spmf.extension.prefixspan.JSPatternGen;
import adl_2daa.ast.ASTExpression;
import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.expression.Identifier;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;
import adl_2daa.gen.encoder.ADLSequenceDecoder;
import adl_2daa.gen.filter.ASTFilter;
import adl_2daa.gen.filter.ASTFilterOperator;
import adl_2daa.gen.filter.ASTNodeFilter;
import adl_2daa.gen.filter.ResultAgent;
import adl_2daa.gen.filter.ResultState;
import adl_2daa.gen.signature.Datatype;

public class InterStateOrderMerger {

	public static final InterStateOrderMerger instance = new InterStateOrderMerger();
	
	private ASTSequenceSelection startingSkelSelection, targetSkelSelection;
	private List<ASTStatement> startingDecodedRel, targetDecodedRel;
	private Action transition, transitionInTarget;
	
	public void merge(Root rootSkel, boolean desType, 
			JSPatternGen<String> relation, boolean useTag){
		decodeRelation(desType, relation);
		select(rootSkel, desType, useTag ? relation.getTag() : -1);
		merge(desType);
		ASTMergeOperator.fillIncompleteKeyAction(rootSkel, startingSkelSelection);
		ASTMergeOperator.fillIncompleteKeyAction(rootSkel, targetSkelSelection);
	}
	
	public void decodeAndDumpRelation(boolean desType, JSPatternGen<String> relation, StringBuilder strb){
		strb.append("Tag: ").append(relation.getTag()).append('\n');
		decodeRelation(desType, relation);
		(new Sequence("start",startingDecodedRel)).toScript(strb, 0);
		strb.append('\n').append("------------->").append('\n');
		(new Sequence("target",targetDecodedRel)).toScript(strb, 0);
	}
	
	private void decodeRelation(boolean desType, JSPatternGen<String> relation){
		List<String> startESeq = new LinkedList<String>();
		for(ItemsetGen<String> iset : relation.getLeftSide().getItemsets()){
			startESeq.add(iset.get(0));
		}
		startingDecodedRel = ADLSequenceDecoder.instance.decode(startESeq);
		
		List<String> targetESeq = new LinkedList<String>(); //Target can be empty
		String eAct;
		for(ItemsetGen<String> iset : relation.getRightSide().getItemsets()){
			eAct = iset.get(0);
			if(!eAct.isEmpty()) targetESeq.add(eAct);
		}
		targetDecodedRel = ADLSequenceDecoder.instance.decode(targetESeq);
		if(desType){
			ASTUtility.removeAllEOBTransition(targetDecodedRel);
		}
	}
	
	private void select(Root rootSkel, boolean desType, int tag){
		int requiredStateCount = desType ? 1 : 2;
		if(requiredStateCount < tag){
			requiredStateCount = tag;
		}
		
		List<ASTExpression> expList = new ArrayList<ASTExpression>();
		if(!desType) expList.add(new ExpressionSkeleton(Datatype.IDENTIFIER));
		transition = new Action(desType ? "Despawn" : "Goto", expList);
		
		//Select starting sequence, an agent with enough state to fit in target relation
		final int fStateCount = requiredStateCount;
		List<ResultAgent> validSequenceAgents = ASTFilterOperator.filterAgent(rootSkel.getRelatedAgents(),
				(agent,filteredState) -> filteredState.size() > 0, 
				(state,filteredSequence) -> filteredSequence.size() > 0, 
				new ASTFilter.EOBtransitionSlotFilter(transition, false, true));
		if(!validSequenceAgents.isEmpty()){
			List<ResultAgent> validAgents = ASTFilterOperator.filterAgentResult(
					validSequenceAgents, 
					(agentResult,filteredState) -> agentResult.getResultStates().size() >= fStateCount, 
					null, null);
			if(!validAgents.isEmpty()){
				//EOB-T slot, State count requirements met 
				ResultAgent startingAgent = ASTUtility.randomUniform(validAgents);
				ResultState startingState = ASTUtility.randomUniform(startingAgent.getResultStates());
				Sequence startingSequence = ASTUtility.randomUniform(startingState.getResultSequences());
				startingSkelSelection = new ASTSequenceSelection(startingAgent.getActualAgent(), 
						startingState.getActualState(), startingSequence);
			}else{
				//EOB-T slot requirement met
				ResultAgent startingAgent = ASTUtility.randomUniform(validSequenceAgents);
				ResultState startingState = ASTUtility.randomUniform(startingAgent.getResultStates());
				Sequence startingSequence = ASTUtility.randomUniform(startingState.getResultSequences());
				//Grow state 
				while(startingAgent.getActualAgent().getStates().size() < fStateCount){
					startingAgent.getActualAgent().getStates().add(
							ASTUtility.createEmptyState(
									"state"+startingAgent.getActualAgent().getStates().size()
									)
							);
				}
				startingSkelSelection = new ASTSequenceSelection(startingAgent.getActualAgent(), 
						startingState.getActualState(), startingSequence);
			}
		}else{
			List<ResultAgent> validAgents = ASTFilterOperator.filterAgent(
					rootSkel.getRelatedAgents(), 
					(agent,filteredState) -> agent.getStates().size() >= fStateCount, 
					null, null);
			if(!validAgents.isEmpty()){
				//State count requirement met
				ResultAgent startingAgent = ASTUtility.randomUniform(validAgents);
				ResultState startingState = ASTUtility.randomUniform(startingAgent.getResultStates());
				//Grow new sequence
				Sequence startingSequence = ASTUtility.createEmptySequence(
						"seq"+startingState.getActualState().getSequences().size());
				startingSkelSelection = new ASTSequenceSelection(startingAgent.getActualAgent(), 
						startingState.getActualState(), startingSequence);
			}else{
				//No requirement met
				Agent startingAgent = ASTUtility.randomUniformAgentOrCreate(rootSkel);
				State startingState = ASTUtility.randomUniformStateOrCreate(startingAgent);
				//Grow new sequence
				Sequence startingSequence = ASTUtility.randomUniformSequenceOrCreate(startingState);
				//Grow state 
				while(startingAgent.getStates().size() < fStateCount){
					startingAgent.getStates().add(
							ASTUtility.createEmptyState(
									"state"+startingAgent.getStates().size()
									)
							);
				}
				startingSkelSelection = new ASTSequenceSelection(startingAgent, 
						startingState, startingSequence);
			}
		}
		
		//Select target sequence based on starting sequence selection
		if(desType){
			targetSkelSelection = new ASTSequenceSelection(startingSkelSelection.agent, 
					null, startingSkelSelection.agent.getDes());
		}else{
			//No need to test for state count requirement here, above step already did this
			transitionInTarget = ASTUtility.removeAllEOBTransition(targetDecodedRel);
			if(transitionInTarget != null){
				List<ResultState> validStates = ASTFilterOperator.filterState(
						startingSkelSelection.agent.getStates(), 
						(state,filteredSequence) -> state != startingSkelSelection.state && 
							filteredSequence.size() > 0, 
						new ASTFilter.EOBtransitionSlotFilter(transitionInTarget, true, true));
				if(!validStates.isEmpty()){
					ResultState targetState = ASTUtility.randomUniform(validStates);
					Sequence targetSequence = ASTUtility.randomUniform(targetState.getResultSequences());
					targetSkelSelection = new ASTSequenceSelection(startingSkelSelection.agent, 
							targetState.getActualState(), targetSequence);
				}else{
					State targetState = startingSkelSelection.state;
					while(targetState == startingSkelSelection.state){
						targetState = ASTUtility.randomUniform(startingSkelSelection.agent.getStates());
					}
					targetSkelSelection = new ASTSequenceSelection(startingSkelSelection.agent, 
							targetState, ASTUtility.randomUniform(targetState.getSequences()));
				}
			}else{
				State targetState = startingSkelSelection.state;
				while(targetState == startingSkelSelection.state){
					targetState = ASTUtility.randomUniform(startingSkelSelection.agent.getStates());
				}
				targetSkelSelection = new ASTSequenceSelection(startingSkelSelection.agent, 
						targetState, ASTUtility.randomUniform(targetState.getSequences()));
			}
		}
	}
	
	private void merge(boolean desType){
		//Since miner use allFlowToTransition as input and discard the transition
		//The starting sequence relation MUST contain no transition
		assert(ASTUtility.removeAllEOBTransition(startingDecodedRel) == null);
		
		if(!desType){
			transition.getParams()[0] = new Identifier("."+
		targetSkelSelection.state.getIdentifier());
		}
		
		ASTSequenceWrapper wrappedStartingSeq = new ASTSequenceWrapper(
				startingSkelSelection.sequence.getStatements());
		ASTSequenceWrapper wrappedStartingRel = new ASTSequenceWrapper(
				startingDecodedRel);
		ASTSequenceWrapper wrappedTargetSeq = new ASTSequenceWrapper(
				targetSkelSelection.sequence.getStatements());
		ASTSequenceWrapper wrappedTargetRel = new ASTSequenceWrapper(
				targetDecodedRel);
		
		int[] transitionSlot = new int[]{-1};
		boolean requireTransitionInsertion = false;
		if(!ASTMergeOperator.matchAndMergeEOBTransition(wrappedStartingSeq, 
				transition, false, transitionSlot)){
			requireTransitionInsertion = true;
		}
		if(wrappedStartingRel.getActionCount() > 0){
			ASTMergeOperator.queueSequenceInsertion(wrappedStartingSeq, wrappedStartingRel, 
				transitionSlot[0]);
		}
		if(requireTransitionInsertion){
			wrappedStartingSeq.queueInsertion(transitionSlot[0], transition);
		}
		wrappedStartingSeq.finalizeWrapper();
		
		transitionSlot = new int[]{-1};
		requireTransitionInsertion = false;
		if(transitionInTarget != null){
			if(!ASTMergeOperator.matchAndMergeEOBTransition(wrappedTargetSeq, 
					transitionInTarget, true, transitionSlot)){
				requireTransitionInsertion = true;
			}
		}else{
			List<Integer> existingEOS = wrappedTargetSeq.getValidSlots(
					ASTNodeFilter.existingTransition(true)
					);
			if(!existingEOS.isEmpty()){
				transitionSlot[0] = existingEOS.get(existingEOS.size()-1);
			}
		}
		if(wrappedTargetRel.getActionCount() > 0){
			ASTMergeOperator.queueSequenceInsertion(wrappedTargetSeq, wrappedTargetRel, 
					transitionSlot[0]);
		}
		if(requireTransitionInsertion){
			wrappedTargetSeq.queueInsertion(transitionSlot[0], transitionInTarget);
		}
		wrappedTargetSeq.finalizeWrapper();
	}
}
