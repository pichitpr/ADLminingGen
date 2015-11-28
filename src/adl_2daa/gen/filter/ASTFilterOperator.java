package adl_2daa.gen.filter;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;

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
}
