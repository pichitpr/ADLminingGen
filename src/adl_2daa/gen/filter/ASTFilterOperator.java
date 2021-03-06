package adl_2daa.gen.filter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import lcs.LCSSequenceEmbedding;
import lcs.SimpleLCSEmbedding;

import org.jacop.core.IntDomain;
import org.jacop.core.IntervalDomain;

import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;
import adl_2daa.gen.generator.ASTNode;
import adl_2daa.gen.generator.ASTSequenceWrapper;
import adl_2daa.jacop.CSPTemplate;
import adl_2daa.jacop.CSPTemplate.BestEffortAssignmentVariedCost;
import adl_2daa.jacop.JaCopUtility;


public class ASTFilterOperator {
	
	public static List<ResultAgent> filterAgent(List<Agent> agentList,
			BiPredicate<Agent, List<ResultState>> agentFilter, 
			BiPredicate<State, List<Sequence>> stateFilter,
			Predicate<Sequence> sequenceFilter){
		List<ResultAgent> result = new LinkedList<ResultAgent>();
		for(Agent agent : agentList){
			List<ResultState> filteredState = filterState(agent.getStates(),
					stateFilter, sequenceFilter);
			if(agentFilter != null && !agentFilter.test(agent, filteredState)) continue;
			result.add(new ResultAgent(agent, filteredState));
		}
		return result;
	}
	
	public static List<ResultAgent> filterAgentResult(List<ResultAgent> agentList,
			BiPredicate<ResultAgent,List<ResultState>> agentFilter, 
			BiPredicate<ResultState, List<Sequence>> stateFilter,
			Predicate<Sequence> sequenceFilter){
		List<ResultAgent> result = new LinkedList<ResultAgent>();
		for(ResultAgent resultAgent : agentList){
			List<ResultState> filteredState = filterStateResult(
					resultAgent.getResultStates(), stateFilter, sequenceFilter);
			if(agentFilter != null && !agentFilter.test(resultAgent, filteredState)) 
				continue;
			result.add(new ResultAgent(resultAgent.getActualAgent(), filteredState));
		}
		return result;
	}
	
	public static List<ResultState> filterState(List<State> stateList, 
			BiPredicate<State, List<Sequence>> stateFilter, 
			Predicate<Sequence> sequenceFilter){
		List<ResultState> result = new LinkedList<ResultState>();
		for(State state : stateList){
			List<Sequence> filteredSequence = filterSequence(state.getSequences(),
					sequenceFilter);
			if(stateFilter != null && !stateFilter.test(state, filteredSequence)) continue;
			result.add(new ResultState(state, filteredSequence));
		}
		return result;
	}
	
	public static List<ResultState> filterStateResult(List<ResultState> stateList,
			BiPredicate<ResultState, List<Sequence>> stateFilter, 
			Predicate<Sequence> sequenceFilter){
		List<ResultState> result = new LinkedList<ResultState>();
		for(ResultState resultState : stateList){
			List<Sequence> filteredSequence = filterSequence(resultState.getResultSequences(),
					sequenceFilter);
			if(stateFilter != null && !stateFilter.test(resultState, filteredSequence)) 
				continue;
			result.add(new ResultState(resultState.getActualState(), filteredSequence));
		}
		return result;
	}
	
	public static List<Sequence> filterSequence(List<Sequence> sequenceList,
			Predicate<Sequence> sequenceFilter){
		List<Sequence> result = new LinkedList<Sequence>();
		for(Sequence seq : sequenceList){
			if(sequenceFilter == null || sequenceFilter.test(seq)) 
				result.add(seq);
		}
		return result;
	}
	
	/**
	 * Filter agent(s) that contain state(s) containing sequences that can fit
	 * all given transitions into each distinct sequence. If all transitions cannot be fitted,
	 * this method tries to fit as much as possible. This method does fit test using
	 * EOBtransitionSlotFilter which does not compare nesting condition.
	 * The sequences in ResultState are<br/>
	 * - Having their length equals to eobTransitions'<br/>
	 * - Sorted in order so that:<br/>
	 * --- If sequences[i] == null : it cannot fit in transition<br/>
	 * --- Else : it can fit in transition[i]<br/>
	 * A state with more than 1 solution will have duplicated ResultState with different
	 * containing sequences. A state with no solution (no transition can be fitted) will
	 * not be added to the result.<br/><br/>
	 * This method DO NOT modify provided agentList but create a new one for the result.
	 */
	public static List<ResultAgent> filterDistinctEOBTransitionFitting(
			List<ResultAgent> agentList, List<Action> eobTransitions, 
			boolean[] eosOnly, boolean tryMatching){
		assert(eobTransitions.size() == eosOnly.length);
		
		//Loop through every existing states and filter for states that can support highest
		//number of transition without growing
		List<ResultAgent> filteredAgent = new LinkedList<ResultAgent>();
		int lowestAllocationCost = Integer.MAX_VALUE;
		for(ResultAgent agent : agentList){
			List<ResultState> filteredState = new LinkedList<ResultState>();
			for(ResultState state : agent.getResultStates()){
				//Check for highest transition coverage for this state
				/*System.out.println("======== "+agent.getActualAgent().getIdentifier()+"."+
						state.getActualState().getIdentifier()+" =======");*/
				
				//Solve for all allocations, get coverage count and record solutions
				int allocationCost = enumerateDistinctEOBTransitionFitting(state, 
						eobTransitions, eosOnly, tryMatching);
				//A solution is valid only if it costs less than "cannot allocate" case
				if(allocationCost >= eobTransitions.size())
					continue;
				if(allocationCost <= lowestAllocationCost){
					//Valid solutions found, record all of them
					if(allocationCost < lowestAllocationCost){
						//New better coverage, wipe all solutions found so far
						filteredAgent.clear();
						filteredState.clear();
						lowestAllocationCost = allocationCost;
					}
					//Loop through all solutions and record them if valid
					for(int[] allocation : JaCopUtility.allPrecalculatedAssignments()){
						assert(eobTransitions.size() == allocation.length);
						List<Sequence> recordedSolution = new LinkedList<Sequence>();
						for(int targetSeqIndex : allocation){
							if(targetSeqIndex >= 0){
								recordedSolution.add(state.getResultSequences().get(targetSeqIndex));
							}else{
								recordedSolution.add(null);
							}
						}
						filteredState.add(new ResultState(state.getActualState(), recordedSolution));
					}
				}
			}
			if(!filteredState.isEmpty())
				filteredAgent.add(new ResultAgent(agent.getActualAgent(), filteredState));
		}
		
		return filteredAgent;
	}
	
	/**
	 * Enumerate all valid allocations of EOB-T with minimal cost using solver. The result
	 * is stored in JaCopUtility cache and minimal cost is returned. 
	 * [i] of each allocation indicates target sequence index that transition[i] should be 
	 * fitted to. The index below 0 means transition[i] cannot fit in any sequence.
	 * It IS possible the allocation is invalid (all [i] < 0). 
	 */
	public static int enumerateDistinctEOBTransitionFitting(ResultState state, 
			List<Action> eobTransitions, boolean[] eosOnly, boolean tryMatching){
		//Generate transition domain
		int seqIndex = 0;
		IntDomain[] eobDomains = new IntDomain[eobTransitions.size()];
		for(int i=0; i<eobDomains.length; i++) eobDomains[i] = new IntervalDomain();
		for(Sequence seq : state.getResultSequences()){
			int transitionIndex = 0;
			for(Action transition : eobTransitions){
				if((new ASTFilter.EOBtransitionSlotFilter(transition, 
						eosOnly[transitionIndex], tryMatching)).test(seq)){
					//The sequence is valid -- add sequence index to transition domain
					eobDomains[transitionIndex].unionAdapt(seqIndex, seqIndex);
				}
				transitionIndex++;
			}
			seqIndex++;
		}
		//Solve for all allocations, get coverage count and record solutions
		CSPTemplate allocationProblem = new CSPTemplate.BestEffortAssignment(eobDomains);
		int allocationCost = JaCopUtility.solveAllSolutionCSP(allocationProblem);
		return allocationCost;
	}
	
	/**
	 * Filter agent(s) with state(s) containing sequences that can match all given Spawn().
	 * If all Spawn() can not be matched, this method tries to match as much as possible.
	 * This method try to match using LCSEmbedding + spawnComparator which checks for
	 * nesting condition. Any state with more than 1 solution will have duplicate 
	 * ResultState under the same ResultAgent with different containing sequences. 
	 * The sequences in ResultState are <br/>
	 * - Having their length equals to relation size (sequence count) <br/>
	 * - sequences[i] == null : i-th relation sequence has no matched skel sequence <br/>
	 * - else : i-th relation sequence is matched to this skel sequence <br/>
	 * This method DO NOT modify provided agentList but create a new one for the result.
	 */
	public static List<ResultAgent> filterHighestSpawnMatch(List<ResultAgent> agentList,
			List<List<ASTStatement>> relation){
		List<ResultAgent> filteredAgent = new LinkedList<ResultAgent>();
		List<ASTSequenceWrapper> wrappedRel = new LinkedList<ASTSequenceWrapper>();
		//Count a number of Spawn() call in each relation sequence
		List<Integer> relSpawnCount = new ArrayList<Integer>();
		for(List<ASTStatement> seq : relation){
			ASTSequenceWrapper rel = new ASTSequenceWrapper(seq);
			int spawnCount = 0;
			for(int i=0; i<rel.size(); i++){
				Action act = (Action)rel.itemAt(i).getNode();
				if(act.getName().equals("Spawn")){
					spawnCount++;
				}
			}
			wrappedRel.add(rel);
			relSpawnCount.add(spawnCount);
		}
		
		//Filter valid state using skel-rel cost table and solver
		int lowestNonMatchCost = Integer.MAX_VALUE;
		for(ResultAgent agent : agentList){
			List<ResultState> filteredState = new LinkedList<ResultState>();
			for(ResultState state : agent.getResultStates()){
				/*System.out.println("======== "+agent.getActualAgent().getIdentifier()+"."+
						state.getActualState().getIdentifier()+" =======");*/
				//Testing for current state
				/*
				 * Variable= relation, Domain value= target sequence index that this relation should be merged with
				 * Result
				 * var[i*2] : specify a sequence that i-th relation should be matched to.
				 * var[i*2+1 : corresponding non-matched cost
				 */
				BestEffortAssignmentVariedCost spawnMatchCSP = new BestEffortAssignmentVariedCost(wrappedRel.size());
				
				//Construct skel-rel non-match cost table, calculate cost threshold
				//A valid solution's non-match cost must not equals to this threshold
				//(if it is equal, the solution is literally the same as "no any match")
				int relSeqIndex = 0;
				int costThreshold = 0;
				for(ASTSequenceWrapper wrappedRelSeq : wrappedRel){
					int highestNonMatchCost = relSpawnCount.get(relSeqIndex);
					int skelSeqIndex = 0;
					for(Sequence seq : state.getResultSequences()){
						ASTSequenceWrapper wrappedSkelSeq = new ASTSequenceWrapper(seq.getStatements());
						Set<LCSSequenceEmbedding<ASTNode>> lcsResult = 
								SimpleLCSEmbedding.allLCSEmbeddings(wrappedRelSeq, wrappedSkelSeq, 
								ASTSequenceWrapper.spawnComparator);
						int matchCount = lcsResult.iterator().next().size();
						int nonMatchCost = highestNonMatchCost - matchCount;
						assert(nonMatchCost >= 0);
						spawnMatchCSP.putAssignmentCost(relSeqIndex, skelSeqIndex, nonMatchCost);
						skelSeqIndex++;
					}
					
					//Also add cost if relSeq is not matched with any skelSeq
					spawnMatchCSP.finalizeCostTableSetup(relSeqIndex, highestNonMatchCost);
					
					//Add cost to threshold
					costThreshold += highestNonMatchCost;
					
					relSeqIndex++;
				}

				//Solve and record if current state is qualified
				int usedNonMatchCost = JaCopUtility.solveAllSolutionCSP(spawnMatchCSP);
				//The solution is valid only if its cost is less than threshold
				if(usedNonMatchCost >= costThreshold) 
					continue;
				if(usedNonMatchCost <= lowestNonMatchCost){
					if(usedNonMatchCost < lowestNonMatchCost){
						//New better solution, older result(s) is obsolete
						filteredAgent.clear();
						filteredState.clear();
						lowestNonMatchCost = usedNonMatchCost;
					}

					//Record all solutions by looping through each solution
					for(int[] match : JaCopUtility.allPrecalculatedAssignments()){
						assert(relation.size() == match.length/2);
						List<Sequence> recordedSolution = new LinkedList<Sequence>();
						for(int i=0; i<match.length; i+=2){
							int targetSkelSeqIndex = match[i];
							if(targetSkelSeqIndex >= 0){
								recordedSolution.add(
										state.getResultSequences().get(targetSkelSeqIndex)
										);
							}else{
								recordedSolution.add(null);
							}
						}
						filteredState.add(new ResultState(state.getActualState(), recordedSolution));
					}
				}
			}
			if(!filteredState.isEmpty())
				filteredAgent.add(new ResultAgent(agent.getActualAgent(), filteredState));
		}
		return filteredAgent;
	}
}
