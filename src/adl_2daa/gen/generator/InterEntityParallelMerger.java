package adl_2daa.gen.generator;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import parsemis.extension.GraphPattern;
import adl_2daa.ast.ASTExpression;
import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.expression.Identifier;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.statement.Condition;
import adl_2daa.ast.statement.Loop;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;
import adl_2daa.gen.encoder.ADLSequenceDecoder;
import adl_2daa.gen.encoder.EncodeTable;
import adl_2daa.gen.filter.ASTFilterOperator;
import adl_2daa.gen.filter.ASTNodeFilter;
import adl_2daa.gen.filter.ResultAgent;
import adl_2daa.gen.filter.ResultState;
import adl_2daa.jacop.JaCopUtility;
import de.parsemis.graph.Edge;
import de.parsemis.graph.Node;

public class InterEntityParallelMerger {

	public static InterEntityParallelMerger instance = new InterEntityParallelMerger();
	
	private List<List<ASTStatement>> spawnerDecodedRel, childDecodedRel;
	private String childIdentifier; //No leading . since it is code syntax
	private int spawnerSequenceCount, childSequenceCount;
	/*
	 * Selection format:
	 * - selection list length == relation length (seq count)
	 * - selection[i] : a sequence that relation[i] is mapped to
	 * - relationType[i] : A type of mapping between selection[i] and relation[i]
	 */
	private List<ASTSequenceSelection> spawnerSelections, childSelections;
	private List<RelationType> spawnerSelectionRelation, childSelectionRelation;
	private enum RelationType{
		NORMAL, SPAWN_MATCH, EOBT_FIT, EOBT_NOFIT
	}
	
	public void merge(Root skel, GraphPattern<String,Integer> relation){
		spawnerDecodedRel = new LinkedList<List<ASTStatement>>();
		childDecodedRel = new LinkedList<List<ASTStatement>>();
		decodeRelation(relation);
		select(skel, childDecodedRel, childSequenceCount, true);
		assert(childSelections.size() > 0);
		editSpawnerSpawnTarget(childIdentifier);
		select(skel, spawnerDecodedRel, spawnerSequenceCount, false);
		merge(skel, true);
		merge(skel, false);
	}
	
	private void decodeRelation(GraphPattern<String,Integer> relation){
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
			//Get connected sequence/tag node
			edge = edgeIt.next();
			Node<String,Integer> seqNode = edge.getOtherNode(root);
			if(edge.getLabel() == EncodeTable.TAG_EDGE){
				childSequenceCount = seqNode.getLabel().getBytes(StandardCharsets.US_ASCII)[0];
				continue;
			}
			//Iterate each edge of sequence node and create a sequence
			Iterator<Edge<String,Integer>> edgeIt2 = seqNode.outgoingEdgeIterator();
			List<String> seqNodeContent = new LinkedList<String>();
			while(edgeIt2.hasNext()){
				//System.out.println("===ACT");
				edge = edgeIt2.next();
				seqNodeContent.add(edge.getOtherNode(seqNode).getLabel());
			}
			ASTUtility.shuffle(seqNodeContent);
			if(edge.getLabel() == EncodeTable.SEQUENCE_ACTION_EDGE){
				spawnerDecodedRel.add(ADLSequenceDecoder.instance.decode(seqNodeContent));
			}else{
				childDecodedRel.add(ADLSequenceDecoder.instance.decode(seqNodeContent));
			}
			
		}
		
		spawnerSequenceCount = spawnerDecodedRel.size();
		if(childDecodedRel.size() > childSequenceCount){
			childSequenceCount = childDecodedRel.size();
		}
	}
	
	private void select(Root skel, List<List<ASTStatement>> rel, int requiredSequenceCount,
			boolean isChild){
		//Filter agent by sequence count requirement
		List<ResultAgent> validAgents = ASTFilterOperator.filterAgent(skel.getRelatedAgents(), 
				(agent, filteredState) -> !filteredState.isEmpty(), 
				(state, filteredSequence) -> filteredSequence.size() >= requiredSequenceCount, 
				null);
		if(validAgents.isEmpty()){
			validAgents = ASTFilterOperator.filterAgent(skel.getRelatedAgents(), 
					null,  null, null);
		}
		
		if(isChild){
			//Remove Main Agent if selecting child agent
			String mainAgentIdentifier = skel.getRelatedAgents().get(0).getIdentifier();
			validAgents.removeIf(resultAgent -> 
				resultAgent.getActualAgent().getIdentifier().equals(mainAgentIdentifier));
			if(validAgents.isEmpty()){
				Agent newAgent = ASTUtility.createEmptyAgent("agent"+skel.getRelatedAgents().size());
				skel.getRelatedAgents().add(newAgent);
				validAgents.add(new ResultAgent(newAgent,
						ASTFilterOperator.filterState(newAgent.getStates(), null, null)
						));
			}
		}else{
			//Remove child if selecting spawner
			validAgents.removeIf(resultAgent -> 
				resultAgent.getActualAgent().getIdentifier().equals(childIdentifier));
		}
		
		//TODO: Child selection should consider only initial state

		/*
		 * Filter for highest spawn match, then select and grow based on EOB-T fitting.
		 * The filter is designed to work with ASTMergeOperator.queueInsertion
		 * (Merging Spawn() takes condition into consideration)
		 */
		List<ResultAgent> tmp = ASTFilterOperator.filterHighestSpawnMatch(
				validAgents, isChild ? childDecodedRel : spawnerDecodedRel);
		if(tmp.isEmpty()){
			//No spawn match -- change Result to no solution case.
			int relationLength = (isChild ? childDecodedRel : spawnerDecodedRel).size();
			for(ResultAgent agent : validAgents){
				for(ResultState state : agent.getResultStates()){
					state.getResultSequences().clear();
					for(int i=0; i<relationLength; i++){
						state.getResultSequences().add(null);
					}
				}
			}
		}else{
			validAgents = tmp;
		}
		selectAndGrow(validAgents, isChild);
	}
	
	private void editSpawnerSpawnTarget(String identifier){
		for(List<ASTStatement> seq : spawnerDecodedRel){
			editSpawnerSpawnTarget(seq, identifier);
		}
	}
	
	private void editSpawnerSpawnTarget(List<ASTStatement> seq, String identifier){
		for(int index=0; index<seq.size(); index++){
			ASTStatement st = seq.get(index);
			if(st instanceof Action){
				Action action = (Action)st;
				if(action.getName().equals("@Spawn")){
					List<ASTExpression> params = new LinkedList<ASTExpression>();
					params.add(new Identifier("."+identifier));
					for(int i=1; i<action.getParams().length; i++){
						params.add(action.getParams()[i]);
					}
					seq.set(index, new Action("Spawn", params));
				}
			}else if(st instanceof Condition){
				Condition cond = (Condition)st;
				editSpawnerSpawnTarget(cond.getIfblock(), identifier);
				if(cond.getElseblock() != null)
					editSpawnerSpawnTarget(cond.getElseblock(), identifier);
			}else if(st instanceof Loop){
				Loop loop = (Loop)st;
				editSpawnerSpawnTarget(loop.getContent(), identifier);
			}
		}
	}
	
	/**
	 * Filter the ResultAgent list with EOB-T fitting, select sequences for the relation
	 * and grow selected agent if needed. Input ResultAgent list MUST contains Spawn() matched
	 * solution (state.sequence[i] specify sequence that matched with relation[i]). 
	 * The solution can be "no solution" where all Sequences in ResultState are null.
	 */
	private void selectAndGrow(List<ResultAgent> agents, 
			boolean isChild){
		List<List<ASTStatement>> relation = isChild ? childDecodedRel : spawnerDecodedRel;
		
		/*
		 * Phase 1:
		 * Filter for highest EOB-T fit. Matched Spawn() pairs are excluded from testing
		 * sequence and relation before filtering. Therefore, each test will have different
		 * sequence and EOB-T list requiring special handle. Note that "no solution" case
		 * is acceptable since the filtering process also specify relation type for each
		 * relation sequence.
		 * The next phase accepts ResultAgent list that conform to below format. 
		 */
		/*
		 * ResultAgent can store any number of ResultState (including duplication) as usual
		 * ResultState stores matching/fitting information as below:
		 * - sequence list's length == relation's length + 1
		 * - sequence[sequence.length-1] (we'll call this lastSequence) is for additional info
		 * - Info and meaning
		 * - sequence[i] != null (== Sequence) , lastSequence[i] != null
		 * 		Transition fitted for EOB-T of relation[i] --> sequence[i] 
		 * 		{SPAWN_MATCH}
		 * - sequence[i] != null (== Sequence) , lastSequence[i] == null
		 * 		Spawn() matched between sequence[i] and relation[i] 
		 * 		{EOBT_FIT}
		 * - sequence[i] == null , lastSequence[i] != null
		 * 		EOB-T exist in relation[i] BUT CANNOT FIT in any non-Spawn() matched sequence
		 * 		{EOBT_NOFIT}
		 * - sequence[i] == null , lastSequence[i] == null
		 * 		No seq relation for relation[i]
		 *		{NORMAL} 	
		 */
		List<ResultAgent> filteredAgent = new LinkedList<ResultAgent>();
		
		int lowestCost = Integer.MAX_VALUE;
		for(ResultAgent agent : agents){
			List<ResultState> filteredState = new LinkedList<ResultState>();
			for(ResultState state : agent.getResultStates()){
				//ResultState comes from result of ASTFilterOperator.filterHighestSpawnMatch()
				assert(state.getResultSequences().size() == relation.size());
				
				//Clone ResultState/relation (since it will be changed from original state during
				//this process)
				List<Sequence> trimmedSequence = new LinkedList<Sequence>();
				trimmedSequence.addAll(state.getActualState().getSequences());
				List<List<ASTStatement>> dupRelation = new ArrayList<List<ASTStatement>>();
				for(List<ASTStatement> relSeq : relation){
					dupRelation.add(new LinkedList<ASTStatement>(relSeq));
				}
				
				/*
				 * Remove matched Spawn() pair from duplicated State (trimmedSequence) and relation (dupRelation),
				 * Create EOB-T list and map (relation each EOB-T is taken from),
				 * Create matched Spawn() pair map for easier access 
				 */
				List<Action> transitionList = new ArrayList<Action>();
				List<Integer> transitionMap = new ArrayList<Integer>(); //Point to relation
				//For matched spawn(), record the actual sequence in proper order instead of pair
				List<Sequence> matchedSpawn = new ArrayList<Sequence>();
				boolean[] isMatchedSpawn = new boolean[relation.size()]; //A flag
				int index = 0;
				int transitionCount = 0;
				//Loop through ASTFilterOperator.filterHighestSpawnMatch() result
				for(Sequence seq : state.getResultSequences()){
					if(seq == null){
						//relation[index] is not matched, check if the relation contains EOB-T
						Action transition = ASTUtility.removeAllEOBTransition(dupRelation.get(index));
						if(transition != null){
							transitionList.add(transition);
							transitionMap.add(index);
							transitionCount++;
						}
						//We can't decide relType until we finish transition fitting
						matchedSpawn.add(null);
						isMatchedSpawn[index] = false;
					}else{
						//Spawn() matched one is ignored during transition fitting
						assert(trimmedSequence.remove(seq));
						matchedSpawn.add(seq);
						isMatchedSpawn[index] = true;
					}
					index++;
				}
				boolean[] eosOnly = new boolean[transitionList.size()];
				for(int i=0; i<eosOnly.length; i++){
					eosOnly[i] = true;
				}
				
				//No transition (fitting cost treated as 0), record solution and continue
				if(transitionCount == 0){
					if(lowestCost > 0){
						lowestCost = 0;
						filteredState.clear();
						filteredAgent.clear();
					}
					List<Sequence> recordedSolution = new ArrayList<Sequence>();
					List<ASTStatement> matchingInfo = new ArrayList<ASTStatement>();
					for(int relIndex=0; relIndex<relation.size(); relIndex++){
						if(isMatchedSpawn[relIndex]){
							recordedSolution.add(matchedSpawn.get(relIndex));
							//Dummy for non-null object
							matchingInfo.add(new Action("", new ArrayList<ASTExpression>()));
						}else{
							recordedSolution.add(null);
							matchingInfo.add(null);
						}
					}
					recordedSolution.add(new Sequence("..", matchingInfo));
					filteredState.add(new ResultState(state.getActualState(), recordedSolution));
					continue;
				}
				 
				//Transition fitting + find fitting cost
				int fittingCost = ASTFilterOperator.enumerateDistinctEOBTransitionFitting(
						new ResultState(state.getActualState(), trimmedSequence), 
						transitionList, eosOnly, true);
				
				//Record if lowestCost, clear previous result if lower than last lowest
				//NOTE that this method also records even no EOB-T can be fitted
				if(fittingCost <= lowestCost){
					if(fittingCost < lowestCost){
						lowestCost = fittingCost;
						filteredState.clear();
						filteredAgent.clear();
					}
					
					//Record solution(s) -- this step also combine Spawn() matching and transition
					//fitting together
					for(int[] allocation : JaCopUtility.allPrecalculatedAssignments()){
						assert(transitionList.size() == transitionMap.size());
						assert(transitionList.size() == allocation.length);
						List<Sequence> recordedSolution = new ArrayList<Sequence>();
						List<ASTStatement> matchingInfo = new ArrayList<ASTStatement>();
						
						//Loop through each relation seq
						int transitionMapIndex = 0;
						for(int relIndex=0; relIndex<relation.size(); relIndex++){
							if(isMatchedSpawn[relIndex]){
								recordedSolution.add(matchedSpawn.get(relIndex));
								//Dummy for non-null object
								matchingInfo.add(new Action("", new ArrayList<ASTExpression>()));  
								continue;
							}
							if(transitionMapIndex < transitionMap.size() &&
									transitionMap.get(transitionMapIndex) == relIndex){
								//This relation seq participated in transition fitting
								int targetTrimmedSeqIndex = allocation[transitionMapIndex];
								if(targetTrimmedSeqIndex >= 0){
									//EOB-T can be fit
									recordedSolution.add(trimmedSequence.get(targetTrimmedSeqIndex));
									matchingInfo.add(null);
								}else{
									//EOB-T exist but cannot be fit
									recordedSolution.add(null);
									matchingInfo.add(new Action("", null)); //Dummy for non-null object
								}
								transitionMapIndex++;
							}else{
								//Not Spawn() matched/EOB-T
								recordedSolution.add(null);
								matchingInfo.add(null);
							}
						}
						
						//Append info and add to filtered ResultState
						recordedSolution.add(new Sequence("..", matchingInfo));
						filteredState.add(new ResultState(state.getActualState(), recordedSolution));
					}
				}
			}
			
			if(!filteredState.isEmpty())
				filteredAgent.add(new ResultAgent(agent.getActualAgent(), filteredState));
		}
		
		//Since no EOB-T solution case is included, empty result is not possible
		assert(!filteredAgent.isEmpty());
		
		/*
		 * Phase 2:
		 * Randomly select 1 agent > state, also construct relation type info
		 */
		ResultAgent rAgent = ASTUtility.randomUniform(filteredAgent);
		Agent selectedAgent = rAgent.getActualAgent();
		if(isChild)
			childIdentifier = selectedAgent.getIdentifier();
		ResultState rState = ASTUtility.randomUniform(rAgent.getResultStates());
		State selectedState = rState.getActualState();
		List<Sequence> seqList = rState.getResultSequences();
		List<ASTStatement> info = seqList.remove(seqList.size()-1).getStatements();
		//Create selection, NORMAL and EOBT_NOFIT has no selection at this point
		final List<ASTSequenceSelection> selection = new ArrayList<ASTSequenceSelection>();
		final List<RelationType> relType = new ArrayList<RelationType>();
		for(int i=0; i<seqList.size(); i++){
			if(seqList.get(i) != null){
				selection.add(new ASTSequenceSelection(selectedAgent, selectedState, 
						seqList.get(i)) );
				if(info.get(i) != null){
					relType.add(RelationType.SPAWN_MATCH);
				}else{
					relType.add(RelationType.EOBT_FIT);
				}
			}else{
				selection.add(null);
				if(info.get(i) != null){
					relType.add(RelationType.EOBT_NOFIT);
				}else{
					relType.add(RelationType.NORMAL);
				}
			}
		}
		
		/*
		 * Phase 3:
		 * Grow agent/sequence if needed
		 */
		//Grow new sequence to support EOBT_NOFIT, also change the flag to EOBT_FIT
		for(int i=0; i<relType.size(); i++){
			if(relType.get(i) == RelationType.EOBT_NOFIT){
				Sequence emptySequence = ASTUtility.createEmptySequence("seq"+
						selectedState.getSequences().size());
				selectedState.getSequences().add(emptySequence);
				selection.set(i, new ASTSequenceSelection(selectedAgent, selectedState, 
						emptySequence) );
				relType.set(i, RelationType.EOBT_FIT);
			}
		}
		
		//Grow new sequence according to sequence count
		int requiredSequenceCount = isChild ? childSequenceCount : spawnerSequenceCount;
		while(selectedState.getSequences().size() < requiredSequenceCount){
			Sequence emptySequence = ASTUtility.createEmptySequence("seq"+
					selectedState.getSequences().size());
			selectedState.getSequences().add(emptySequence);
		}
		
		//Randomly pick remaining sequence for NORMAL relation type
		List<Sequence> availableSequence = new ArrayList<Sequence>(selectedState.getSequences());
		for(ASTSequenceSelection sel : selection){
			//Remove all selected sequence first
			if(sel != null)
				assert(availableSequence.remove(sel.sequence));
		}
		for(int i=0; i<relType.size(); i++){
			if(relType.get(i) == RelationType.NORMAL){
				Sequence randomSeq = availableSequence.remove(
						ASTUtility.randomRange(0, availableSequence.size()-1)
						);
				selection.set(i, new ASTSequenceSelection(selectedAgent, selectedState, 
						randomSeq) );
			}
		}
		
		if(isChild){
			childSelections = new ArrayList<ASTSequenceSelection>(selection);
			childSelectionRelation = new ArrayList<RelationType>(relType);
		}else{
			spawnerSelections = new ArrayList<ASTSequenceSelection>(selection);
			spawnerSelectionRelation = new ArrayList<RelationType>(relType);
		}
	}
	
	private void merge(Root skel, boolean isChild){
		//NOTE:: fix code beyond transition --> Seems to disappear
		
		List<List<ASTStatement>> decodedRelations = isChild ? childDecodedRel : spawnerDecodedRel;
		List<ASTSequenceSelection> selections = isChild ? childSelections : spawnerSelections;
		List<RelationType> relType = isChild ? childSelectionRelation : spawnerSelectionRelation;
		int index = 0;
		/*
		 * Construct transition list and remove transition from relation
		 * Removed transition from EOBT_FIT (and EOBT_NOFIT) cases will be merged,
		 * while discarded for SPAWN_MATCH case (we want to merge this case regardless
		 * of transition -- Spawn() match takes priority)
		 */
		List<Action> transitionList = new ArrayList<Action>();
		for(List<ASTStatement> seq : decodedRelations){
			if(relType.get(index) == RelationType.EOBT_FIT){
				Action transition = ASTUtility.removeAllEOBTransition(seq);
				transitionList.add(transition);
			}else if(relType.get(index) == RelationType.SPAWN_MATCH){
				ASTUtility.removeAllEOBTransition(seq);
				transitionList.add(null);
			}else{
				transitionList.add(null);
			}
			index++;
		}
		
		assert(selections.size() == decodedRelations.size());
		assert(selections.size() == transitionList.size());
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
			Action transition = transitionList.get(i);
			
			if(transition != null){
				//Merging relation contains EOB-T, merge EOB-T first.
				//If insertion is required, code bound is set and flag is turned on
				if(!ASTMergeOperator.matchAndMergeEOBTransition(wrappedSkel, transition, true,
						transitionSlot)){
					requireTransitionInsertion = true;
				}
			}else{
				//No EOB-T, find code bound
				List<Integer> existingEOS = wrappedSkel.getValidSlots(
						ASTNodeFilter.existingTransition(true)
						);
				if(!existingEOS.isEmpty()){
					transitionSlot[0] = existingEOS.get(existingEOS.size()-1);
				}
			}
			
			//Queue relation (with key action matching) and finalize
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
