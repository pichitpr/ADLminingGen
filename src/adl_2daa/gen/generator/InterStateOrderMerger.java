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
		
		/*
		final int fStateCount = requiredStateCount;
		ResultAgent startingAgent;
		boolean passStateRequirement = false;
		boolean passEOBtransitionSlotRequirement = false;
		List<ResultAgent> validSequenceAgents = ASTFilterOperator.filterAgent(rootSkel.getRelatedAgents(),
				(agent,filteredState) -> filteredState.size() > 0, 
				(state,filteredSequence) -> filteredSequence.size() > 0, 
				new ASTFilter.EOBtransitionSlotFilter(transition, true));
		if(!validSequenceAgents.isEmpty()){
			passEOBtransitionSlotRequirement = true;
			List<ResultAgent> validAgents = ASTFilterOperator.filterAgentResult(
					validSequenceAgents, 
					(agentResult,filteredState) -> agentResult.getResultStates().size() >= fStateCount, 
					null, null);
			if(!validAgents.isEmpty()){
				passStateRequirement = true;
				startingAgent = ASTUtility.randomUniform(validAgents);
			}else{
				startingAgent = ASTUtility.randomUniform(validSequenceAgents);
			}
		}else{
			List<ResultAgent> validAgents = ASTFilterOperator.filterAgent(
					rootSkel.getRelatedAgents(), 
					(agent,filteredState) -> agent.getStates().size() >= fStateCount, 
					null, null);
			if(!validAgents.isEmpty()){
				passStateRequirement = true;
				startingAgent = ASTUtility.randomUniform(validAgents);
			}else{
				startingAgent = ASTFilterOperator.createResultAgent(
						ASTUtility.randomUniformAgentOrCreate(rootSkel)
						);
			}
		}
		if(!passEOBtransitionSlotRequirement){
			ResultState startingState = ASTUtility.randomUniform(startingAgent.getResultStates());
			Sequence startingSequence = ASTUtility.createEmptySequence(
					"seq"+startingState.getActualState().getSequences().size());
			startingSkelSelection = new ASTSequenceSelection(startingAgent.getActualAgent(), 
					startingState.getActualState(), startingSequence); 
		}
		if(!passStateRequirement){
			
		}
		ASTMergeOperator.mergeEOBTransition(startingSkelSelection, transition, false, true);
		*/
		
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
		
		if(desType){
			targetSkelSelection = new ASTSequenceSelection(startingSkelSelection.agent, 
					null, startingSkelSelection.agent.getDes());
		}else{
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
		
		/*
		//Setup starting sequence
		List<Agent> validSequenceAgents = ASTUtility.filterAgent(rootSkel.getRelatedAgents(), 
				(agent,filteredState) -> filteredState.size() > 0, 
				(state,filteredSequence) -> filteredSequence.size() > 0,
				sequenceFilter);
		Agent validAgent = null;
		if(validSequenceAgents.size() > 0){
			//Agents with valid sequence (available slot for eob-transition)
			List<Agent> validStateCountAgents = ASTUtility.filterAgent(validSequenceAgents, 
					(agent,filteredState) -> agent.getStates().size() >= fRequiredStateCount, 
					null, null);
			if(validStateCountAgents.size() > 0){
				//Agents with required state count
				validAgent = ASTUtility.randomUniform(validStateCountAgents);
			}else{
				validAgent = ASTUtility.randomUniform(validSequenceAgents);
				while(validAgent.getStates().size() < fRequiredStateCount){
					validAgent.getStates().add(
							ASTUtility.createEmptyState(".state"+validAgent.getStates().size())
							);
				}
			}
			//Select sequences in valid agent
			State startingState = ASTUtility.randomUniform(
					ASTUtility.filterState(validAgent.getStates(), 
							(state, filteredSequence) -> filteredSequence.size() > 0, 
							sequenceFilter
							));
			Sequence validSequence = ASTUtility.randomUniform(
					ASTUtility.filterSequence(startingState.getSequences(), sequenceFilter)
					);
			startingSkelSelection = new ASTSequenceSelection(validAgent, startingState, validSequence);
		}else{
			//No agent with valid sequence, randomly pick one and give it a valid sequence
			validAgent = ASTUtility.randomUniformAgent(rootSkel);
			State startingState = ASTUtility.randomUniformState(validAgent);
			Sequence startingSequence = ASTUtility.createEmptySequence(
					".seq"+startingState.getSequences().size());
			startingState.getSequences().add(startingSequence);
			//Also grow state to meet requirement
			startingSkelSelection = new ASTSequenceSelection(validAgent, startingState, startingSequence);
			while(validAgent.getStates().size() < fRequiredStateCount){
				validAgent.getStates().add(
						ASTUtility.createEmptyState(".state"+validAgent.getStates().size())
						);
			}			
		}
		
		//Setup target sequence
		String startingStateIdentifier = startingSkelSelection.state.getIdentifier();
		if(desType){
			targetSkelSelection = new ASTSequenceSelection(validAgent, null, validAgent.getDes());
			transition = new Action("Despawn", new ArrayList<ASTExpression>());
		}else{
			State targetState;
			do{
				targetState = ASTUtility.randomUniform(validAgent.getStates());
			}while(startingStateIdentifier.equals(targetState.getIdentifier()));
			targetSkelSelection = new ASTSequenceSelection(validAgent, targetState,
					ASTUtility.randomUniform(targetState.getSequences())
					);
			List<ASTExpression> params = new ArrayList<ASTExpression>();
			params.add(new Identifier("."+targetSkelSelection.state.getIdentifier()));
			transition = new Action("Goto", params);
		}
		*/
		
		/*
		//First, try to find an agent with valid state count and valid sequence
		final int fRequiredStateCount = requiredStateCount;
		List<Agent> validAgents = ASTUtility.filterAgent(rootSkel, 
				(agent,filteredState) -> agent.getStates().size() >= fRequiredStateCount, 
				(state,filteredSequence) -> filteredSequence.size() > 0,
				sequenceFilter, false);
		if(validAgents.size() > 0){
			Agent validAgent = ASTUtility.randomUniform(validAgents);
			State startingState = ASTUtility.randomUniform(
					ASTUtility.filterState(validAgent, null, sequenceFilter, false)
					);
			Sequence validSequence = ASTUtility.randomUniform(
					ASTUtility.filterSequence(startingState, sequenceFilter)
					);
			startingSkelSelection = new ASTSequenceSelection(validAgent, startingState, validSequence);
			selectTarget(desType);
			return;
		}
		
		//No valid agent with specified condition, filter with sequence filter only
		//(this has higher priority)
		Agent validAgent = ASTUtility.randomAgentAndGenEmptyState(rootSkel, fRequiredStateCount);
		List<State> validStates = ASTUtility.filterState(validAgent, 
				(state,filteredSequence) -> filteredSequence.size() > 0, 
				sequenceFilter, false);
		if(validStates.size() > 0){
			State startingState = ASTUtility.randomUniform(validStates);
			Sequence validSequence = ASTUtility.randomUniform(
					ASTUtility.filterSequence(startingState, sequenceFilter)
					); 
			startingSkelSelection = new ASTSequenceSelection(validAgent, startingState, validSequence);
			selectTarget(desType);
			return;
		}
		*/
	}
	
	/*
	private void selectTarget(boolean desType){
		Agent validAgent = startingSkelSelection.agent;
		String startingStateIdentifier = startingSkelSelection.state.getIdentifier();
		if(desType){
			targetSkelSelection = new ASTSequenceSelection(validAgent, null, validAgent.getDes());
			transition = new Action("Despawn", new ArrayList<ASTExpression>());
		}else{
			State targetState;
			do{
				targetState = ASTUtility.randomUniform(validAgent.getStates());
			}while(startingStateIdentifier.equals(targetState.getIdentifier()));
			targetSkelSelection = new ASTSequenceSelection(validAgent, targetState,
					ASTUtility.randomUniform(targetState.getSequences())
					);
			List<ASTExpression> params = new ArrayList<ASTExpression>();
			params.add(new Identifier("."+targetSkelSelection.state.getIdentifier()));
			transition = new Action("Goto", params);
		}
	}
	*/
	
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
