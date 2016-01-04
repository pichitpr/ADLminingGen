package adl_2daa.gen.filter;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.jacop.core.IntDomain;
import org.jacop.core.IntervalDomain;

import adl_2daa.ast.statement.Action;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;
import adl_2daa.gen.generator.JaCopUtility;

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
	 * Filter an agent that contains a state containing sequences that can fit
	 * all given transitions into each distinct sequence. If all transitions cannot be fitted,
	 * this method tries to fit as much as possible. The sequences in ResultState are<br/>
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
		int highestTransitionSupport = 0;
		for(ResultAgent agent : agentList){
			List<ResultState> filteredState = new LinkedList<ResultState>();
			for(ResultState state : agent.getResultStates()){
				//Check for highest transition coverage for this state 
				
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
				int coverage = JaCopUtility.randomPartialAlldiffAssignment(eobDomains);
				if(coverage >= highestTransitionSupport){
					//Valid solutions found, record all of them
					if(coverage > highestTransitionSupport){
						//New better coverage, wipe all solutions found so far
						filteredAgent.clear();
						filteredState.clear();
						highestTransitionSupport = coverage;
					}
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
						filteredState.add(new ResultState(state.getActualState(), 
								recordedSolution));
					}
				}
			}
			if(!filteredState.isEmpty())
				filteredAgent.add(new ResultAgent(agent.getActualAgent(), filteredState));
		}
		
		return filteredAgent;
	}
}
