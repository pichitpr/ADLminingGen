package adl_2daa.gen.generator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import parsemis.extension.GraphPattern;
import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.gen.encoder.ADLSequenceDecoder;
import adl_2daa.gen.filter.ASTFilterOperator;
import adl_2daa.gen.filter.ASTNodeFilter;
import adl_2daa.gen.filter.ResultAgent;
import adl_2daa.gen.filter.ResultState;
import de.parsemis.graph.Edge;
import de.parsemis.graph.Node;

public class ParallelMerger {

	public static final ParallelMerger instance = new ParallelMerger();

	private List<List<ASTStatement>> decodedRelations;
	private List<ASTSequenceSelection> selections;
	private List<Action> transitions;
	
	public void merge(Root skel, GraphPattern<String,Integer> relation){
		decodeRelation(relation);
		select(skel);
		merge(skel);
	}
	
	private void decodeRelation(GraphPattern<String,Integer> relation){
		decodedRelations = new LinkedList<List<ASTStatement>>();
		
		//Find root
		Node<String,Integer> root = null;
		Iterator<Node<String,Integer>> nodeIt = relation.getGraph().nodeIterator();
		while(nodeIt.hasNext()){
			root = nodeIt.next();
			if(root.getInDegree() == 0){
				//System.out.println("ROOT");
				break;
			}
		}
		
		//Iterate each root edge
		Iterator<Edge<String,Integer>> edgeIt = root.outgoingEdgeIterator();
		Edge<String,Integer> edge;
		while(edgeIt.hasNext()){
			//System.out.println("=SEQ");
			//Get connected sequence node
			edge = edgeIt.next();
			Node<String,Integer> seqNode = edge.getOtherNode(root);
			//Iterate each edge of sequence node and create a sequence
			Iterator<Edge<String,Integer>> edgeIt2 = seqNode.outgoingEdgeIterator();
			List<String> seqNodeContent = new LinkedList<String>();
			while(edgeIt2.hasNext()){
				//System.out.println("===ACT");
				edge = edgeIt2.next();
				seqNodeContent.add(edge.getOtherNode(seqNode).getLabel());
			}
			ASTUtility.shuffle(seqNodeContent);
			decodedRelations.add(ADLSequenceDecoder.instance.decode(seqNodeContent));
		}
	}
	
	private void select(Root skel){
		selections = new LinkedList<ASTSequenceSelection>();
		transitions = new LinkedList<Action>();
		int requiredSequenceCount = decodedRelations.size();
		int transitionCount = 0;
		System.out.println(requiredSequenceCount+"  "+transitionCount);
		for(List<ASTStatement> seq : decodedRelations){
			Action transition = ASTUtility.removeAllEOBTransition(seq);
			if(transition != null) transitionCount++;
			transitions.add(transition);
			selections.add(null);
		}
		
		//Filter for agents that satisfy sequence count requirement first
		List<ResultAgent> validAgents = ASTFilterOperator.filterAgent(skel.getRelatedAgents(), 
				(agent, filteredState) -> !filteredState.isEmpty(), 
				(state, filteredSequence) -> filteredSequence.size() >= requiredSequenceCount, 
				null);
		if(validAgents.isEmpty()){
			validAgents = ASTFilterOperator.filterAgent(skel.getRelatedAgents(), 
					null,  null, null);
		}
		ResultAgent selectedAgent;
		ResultState selectedState;
		if(transitionCount > 0){
			selectedAgent = selectAndGrowBasedOnTransition(validAgents, transitionCount);
			selectedState = selectedAgent.getResultStates().get(0);
		}else{
			selectedAgent = ASTUtility.randomUniform(validAgents);
			selectedState = ASTUtility.randomUniform(selectedAgent.getResultStates());
		}
		
		//Fill the missing sequence (if any)
		while(selectedState.getResultSequences().size() < requiredSequenceCount){
			selectedState.getResultSequences().add(
					ASTUtility.createEmptySequence("seq"+selectedState.getResultSequences().size())
					);
		}
		
		//Select sequences for the remaining relations that do not contain transition
		ListIterator<ASTSequenceSelection> it = selections.listIterator();
		ASTSequenceSelection selection;
		while(it.hasNext()){
			selection = it.next();
			if(selection == null){
				Sequence selectedSequence = ASTUtility.randomUniform(selectedState.getResultSequences());
				it.set(new ASTSequenceSelection(selectedAgent.getActualAgent(), 
						selectedState.getActualState(), selectedSequence));
				selectedState.getResultSequences().remove(selectedSequence);
			}
		}
	}
	
	/**
	 * Select a state along with its sequences that can fit in provided relations 
	 * with transition as many as possible. Extra sequences are grew to fit the 
	 * remaining transitions. This method returns a new ResultAgent which is the 
	 * selected agent containing only the selected state and the remaining sequences. 
	 */
	private ResultAgent selectAndGrowBasedOnTransition(List<ResultAgent> agentList, 
			int transitionCount){		
		List<Action> transitionList = new ArrayList<Action>();
		int[] transitionMap = new int[transitionCount];
		boolean[] eosOnly = new boolean[transitionCount];
		int actualIndex=0,index = 0;
		for(Action transition : transitions){
			if(transition != null){
				transitionList.add(transition);
				transitionMap[index] = actualIndex;
				eosOnly[index] = true;
				index++;
			}
			actualIndex++;
		}
		
		//Select existing sequences for as many transitions as possible.
		List<ResultAgent> validAgents = ASTFilterOperator.filterDistinctEOBTransitionFitting(
				agentList, transitionList, eosOnly, true);
		ResultAgent agent;
		ResultState state;
		ResultAgent returnedAgent = null; //A copied,trimmed version of agent to be returned
		if(!validAgents.isEmpty()){
			//At least 1 transition can be fitted, randomly select 1 agent/state
			agent = ASTUtility.randomUniform(validAgents);
			state = ASTUtility.randomUniform(agent.getResultStates());
		}else{
			//No transition can be fitted, randomly select any
			agent = ASTUtility.randomUniform(agentList);
			state = ASTUtility.randomUniform(agent.getResultStates());
			//Then change to a copy version with result sequences becoming all null
			state = new ResultState(state.getActualState(), 
					new LinkedList<Sequence>());
			for(int i=1; i<transitionCount; i++){
				state.getResultSequences().add(null);
			}
		}
		
		//Create a copy of the selected state from agentList
		//Used sequences will be removed from its sequence list
		List<Sequence> copiedSequences = new LinkedList<Sequence>();
		for(ResultAgent a : agentList){
			if(a.getActualAgent() == agent.getActualAgent()){
				for(ResultState st : a.getResultStates()){
					if(st.getActualState() == state.getActualState()){
						for(Sequence seq : st.getResultSequences()){
							copiedSequences.add(seq);
						}
						break;
					}
				}
			}
			if(!copiedSequences.isEmpty()) break;
		}
		assert(!copiedSequences.isEmpty());
		List<ResultState> singletonStateList = new LinkedList<ResultState>();
		singletonStateList.add(new ResultState(state.getActualState(), copiedSequences));
		returnedAgent = new ResultAgent(agent.getActualAgent(), singletonStateList);
		
		//Modify selection list, also remove selected sequences
		//Any transition without selection will be mapped to the newly grew sequence instead
		int transitionIndex = 0;
		for(Sequence seq : state.getResultSequences()){
			ASTSequenceSelection selection;
			if(seq != null){
				selection = new ASTSequenceSelection(agent.getActualAgent(), 
						state.getActualState(), seq);
				copiedSequences.remove(seq);
			}else{
				Sequence emptySequence = ASTUtility.createEmptySequence("seq"+
						state.getActualState().getSequences().size());
				state.getActualState().getSequences().add(emptySequence);
				selection = new ASTSequenceSelection(agent.getActualAgent(), 
						state.getActualState(), emptySequence);
			}
			int replacingIndex = transitionMap[transitionIndex];
			selections.remove(replacingIndex);
			selections.add(replacingIndex, selection);
			transitionIndex++;
		}
		
		return returnedAgent;
	}
	
	private void merge(Root skel){
		assert(selections.size() == decodedRelations.size());
		assert(selections.size() == transitions.size());
		List<ASTSequenceWrapper> wrappedSkels = new ArrayList<ASTSequenceWrapper>();
		List<ASTSequenceWrapper> wrappedRels = new ArrayList<ASTSequenceWrapper>();
		for(ASTSequenceSelection selection : selections){
			wrappedSkels.add(new ASTSequenceWrapper(selection.sequence.getStatements()));
		}
		for(List<ASTStatement> decodedRelation : decodedRelations){
			wrappedRels.add(new ASTSequenceWrapper(decodedRelation));
		}
		
		for(int i=0; i<wrappedSkels.size(); i++){
			int[] transitionSlot = new int[]{-1};
			boolean requireTransitionInsertion = false;
			ASTSequenceWrapper wrappedSkel = wrappedSkels.get(i);
			ASTSequenceWrapper wrappedRel = wrappedRels.get(i);
			Action transition = transitions.get(i);
			
			if(transition != null){
				if(!ASTMergeOperator.matchAndMergeEOBTransition(wrappedSkel, transition, true,
						transitionSlot)){
					requireTransitionInsertion = true;
				}
			}else{
				List<Integer> existingEOS = wrappedSkel.getValidSlots(
						ASTNodeFilter.existingTransition(true)
						);
				if(!existingEOS.isEmpty()){
					transitionSlot[0] = existingEOS.get(existingEOS.size()-1);
				}
			}
			if(wrappedRel.getActionCount() > 0){
				ASTMergeOperator.queueSequenceInsertion(wrappedSkel, wrappedRel, transitionSlot[0]);
			}
			if(requireTransitionInsertion){
				wrappedSkel.queueInsertion(transitionSlot[0], transition);
			}
			wrappedSkel.finalizeWrapper();
		}
	}
	
}
