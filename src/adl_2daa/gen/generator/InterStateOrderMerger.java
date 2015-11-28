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
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;
import adl_2daa.gen.encoder.ADLSequenceDecoder;
import adl_2daa.gen.filter.ASTFilter;
import adl_2daa.gen.filter.ASTFilterOperator;
import adl_2daa.gen.filter.ResultAgent;
import adl_2daa.gen.filter.ResultState;
import adl_2daa.gen.signature.Datatype;

public class InterStateOrderMerger {

	public static final InterStateOrderMerger instance = new InterStateOrderMerger();
	
	private ASTSequenceSelection startingSkelSelection, targetSkelSelection;
	private List<ASTStatement> startingDecodedRel, targetDecodedRel;
	private Action transition;
	
	public void merge(Root rootSkel, boolean desType, 
			JSPatternGen<String> relation, boolean useTag){
		decodeRelation(desType, relation);
		select(rootSkel, desType, useTag ? relation.getTag() : -1);
		merge(desType);
		ASTMergeOperator.fillIncompleteKeyAction(rootSkel, startingSkelSelection);
		ASTMergeOperator.fillIncompleteKeyAction(rootSkel, targetSkelSelection);
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
			eAct = iset.get(0).trim();
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
				new ASTFilter.EOBtransitionSlotFilter(transition, true));
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
				ResultAgent startingAgent = ASTUtility.randomUniform(validAgents);
				ResultState startingState = ASTUtility.randomUniform(startingAgent.getResultStates());
				//Grow new sequence
				Sequence startingSequence = ASTUtility.createEmptySequence(
						"seq"+startingState.getActualState().getSequences().size());
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
		}
		
		//Select target sequence based on starting sequence selection
		if(desType){
			targetSkelSelection = new ASTSequenceSelection(startingSkelSelection.agent, 
					null, startingSkelSelection.agent.getDes());
		}else{
			//No need to test for state count requirement here, above step already did this
			Action eobTransition = ASTUtility.removeAllEOBTransition(targetDecodedRel);
			if(eobTransition != null){
				List<ResultState> validStates = ASTFilterOperator.filterState(
						startingSkelSelection.agent.getStates(), 
						(state,filteredSequence) -> state != startingSkelSelection.state && 
							filteredSequence.size() > 0, 
						new ASTFilter.EOStransitionSlotFilter(eobTransition, true));
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
				ASTMergeOperator.mergeEOBTransition(targetSkelSelection, eobTransition, true, true);
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
			transition.getParams()[0] = new Identifier("."+targetSkelSelection.sequence.getIdentifier());
		}
		ASTMergeOperator.mergeEOBTransition(startingSkelSelection, transition, false, true);
		ASTMergeOperator.merge(startingSkelSelection, startingDecodedRel);
		ASTMergeOperator.merge(targetSkelSelection, targetDecodedRel);
	}
}
