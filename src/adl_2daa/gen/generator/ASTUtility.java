package adl_2daa.gen.generator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;

public class ASTUtility {

	private static Random random = new Random(1000);
	
	/*private static Random rand(){
		return new Random();
	}*/
	
	/**
	 * Return [start, end]
	 */
	public static int randomRange(int start, int end){
		return start+random.nextInt(end-start+1);
	}
	
	/**
	 * Precondition: root with at least 1 agent
	 */
	public static Agent randomUniformAgent(Root root){
		return randomUniform(root.getRelatedAgents());
	}
	
	/**
	 * Precondition: agent with at least 1 state
	 */
	public static State randomUniformState(Agent agent){
		return randomUniform(agent.getStates());
	}
	
	/**
	 * Precondition: state with at least 1 sequence
	 */
	public static Sequence randomUniformSequence(State state){
		return randomUniform(state.getSequences());
	}
	
	/**
	 * Precondition: non-empty list
	 */
	public static <T> T randomUniform(List<T> list){
		return list.get(randomRange(0, list.size()-1));
	}
	
	/**
	 * Random 1 agent which satisfies minimum requiredStateCount from root (uniform)
	 * If no agent satisfies the requirement, 1 agent will be randomly picked, then,
	 * states (with 1 empty sequence) will be generated and added to the agent until
	 * requirement satisfied<br/>
	 * Precondition: root with at least 1 agent
	 */
	public static Agent randomAgentAndGenEmptyState(Root root, int requiredStateCount){
		List<Integer> agentWithRequiredStateCount = new ArrayList<Integer>();
		int i=0;
		for(Agent agent : root.getRelatedAgents()){
			if(agent.getStates().size() >= requiredStateCount){
				agentWithRequiredStateCount.add(i);
			}
			i++;
		}
		if(agentWithRequiredStateCount.isEmpty()){
			Agent agent = randomUniformAgent(root);
			while(agent.getStates().size() < requiredStateCount){
				agent.getStates().add(createEmptyState(
						"state"+agent.getStates().size()
						));
			}
			return agent;
		}else{
			int index = randomRange(0, agentWithRequiredStateCount.size()-1);
			return root.getRelatedAgents().get(index);
		}
	}
	
	/*
	public static State randomStateAndGen(Agent agent, int requiredSequenceCount){
		List<Integer> stateWithRequiredSequenceCount = new ArrayList<Integer>();
		int i=0;
		for(State state : agent.getStates()){
			if(state.getSequences().size() >= requiredSequenceCount){
				stateWithRequiredSequenceCount.add(i);
			}
			i++;
		}
		if(stateWithRequiredSequenceCount.isEmpty()){
			State state = randomUniformState(agent);
			while(state.getSequences().size() < requiredSequenceCount){
				state.getSequences().add(createEmptySequence(
						"seq"+state.getSequences().size()
						));
			}
			return state;
		}else{
			int index = randomRange(0, stateWithRequiredSequenceCount.size()-1);
			return agent.getStates().get(index);
		}
	}
	*/
	
	/**
	 * Create an agent named identifier with: empty init, enpty des,
	 * state0 with empty seq0 inside.
	 */
	public static Agent createEmptyAgent(String identifier){
		List<State> states = new LinkedList<State>();
		states.add(createEmptyState("state0"));
		Agent agent = new Agent(identifier, 
				createEmptySequence("init"), 
				createEmptySequence("des"), 
				states);
		return agent;
	}
	
	/**
	 * Create a state named identifier with empty seq0 inside
	 */
	public static State createEmptyState(String identifier){
		List<Sequence> sequences = new LinkedList<Sequence>();
		sequences.add(createEmptySequence("seq0"));
		State state = new State(identifier, sequences);
		return state;
	}
	
	/**
	 * Create an empty sequence named identifier
	 */
	public static Sequence createEmptySequence(String identifier){
		List<ASTStatement> statement = new LinkedList<ASTStatement>();
		Sequence seq = new Sequence(identifier, statement);
		return seq;
	}
	
	/**
	 * Filter valid agents from a given root based on given filters. 
	 * If trimMode is used, the returned agents are cloned and modified to have only 
	 * valid content based on given filters (Sequences are not cloned!). 
	 * If trimMode is not used, actual agents that 1) passes the filter 2) all its contents
	 * pass the filter are returned
	 */
	public static List<Agent> filterAgent(Root root, 
			BiPredicate<Agent, List<State>> agentFilter, 
			BiPredicate<State, List<Sequence>> stateFilter, 
			Predicate<Sequence> sequenceFilter, boolean trimMode){
		List<Agent> validAgents = new LinkedList<Agent>();
		for(Agent agent : root.getRelatedAgents()){
			List<State> filteredState = filterState(agent, stateFilter, sequenceFilter, 
					trimMode);
			if(agentFilter != null && !agentFilter.test(agent, filteredState)) 
				continue;
			if(trimMode){
				agent = new Agent(agent.getIdentifier(), agent.getInit(), 
						agent.getDes(), filteredState);
				validAgents.add(agent);
			}else{
				if(filteredState.size() == agent.getStates().size()){
					validAgents.add(agent);
				}
			}
		}
		return validAgents;
	}
	
	/**
	 * Filter valid states from a given agent based on given filters
	 * If trimMode is used, the returned states are cloned and modified to have only 
	 * valid content based on given filters (Sequences are not cloned!). 
	 * If trimMode is not used, actual states that 1) passes the filter 2) all its contents
	 * pass the filter are returned
	 */
	public static List<State> filterState(Agent agent, 
			BiPredicate<State, List<Sequence>> stateFilter, 
			Predicate<Sequence> sequenceFilter, boolean trimMode){
		List<State> validStates = new LinkedList<State>();
		for(State state : agent.getStates()){
			List<Sequence> filteredSequence = filterSequence(state, sequenceFilter);
			if(stateFilter != null && !stateFilter.test(state, filteredSequence))
				continue;
			if(trimMode){
				state = new State(state.getIdentifier(), filteredSequence);
				validStates.add(state);
			}else{
				if(filteredSequence.size() == state.getSequences().size()){
					validStates.add(state);
				}
			}
		}
		return validStates;
	}
	
	/**
	 * Filter valid sequences from a given state based on a given filter. Sequences returned
	 * refers to the actual sequence in given state (NOT the clone!).
	 */
	public static List<Sequence> filterSequence(State state, 
			Predicate<Sequence> sequenceFilter){
		List<Sequence> validSequences = new LinkedList<Sequence>();
		for(Sequence sequence : state.getSequences()){
			if(sequenceFilter == null || sequenceFilter.test(sequence)){
				validSequences.add(sequence);
			}
		}
		return validSequences;
	}
	
	public static boolean isStatementListEndWithTransition(List<ASTStatement> list){
		if(list.size() == 0) return false;
		ASTStatement st = list.get(list.size()-1);
		if(st instanceof Action){
			String name = ((Action)st).getName();
			if(name.equals("Goto") || name.equals("Despawn")){
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}
	
	/*
	public static HashMap<Integer, ASTPosition> generateASTPositionMap(Sequence sequence){
		HashMap<Integer, ASTPosition> result = new HashMap<Integer, ASTPosition>();
		generateASTPositionMap(0, sequence.getStatements(), null, result);
		return result;
	}
	
	private static int generateASTPositionMap(int startIndex, List<ASTStatement> statements,
			ASTPosition parent, HashMap<Integer, ASTPosition> map){
		ASTPosition positionInfo;
		int index = startIndex;
		int i=0;
		for(ASTStatement st : statements){
			positionInfo = new ASTPosition(i, statements, parent);
			map.put(index, positionInfo);
			index++;
			if(st instanceof Condition){
				Condition cond = (Condition)st;
				index = generateASTPositionMap(index, cond.getIfblock(), positionInfo, map);
				if(cond.getElseblock() != null){
					index = generateASTPositionMap(index, cond.getElseblock(), positionInfo, map);
				}
			}else if(st instanceof Loop){
				index = generateASTPositionMap(index, ((Loop)st).getContent(), positionInfo, map);
			}
			i++;
		}
		//End of statements
		map.put(index, new ASTPosition(i, statements, parent));
		index++;
		return index;
	}
	
	public static class ASTPosition {
		public int index;
		public List<ASTStatement> astContent;
		public ASTPosition parent = null;
		
		public ASTPosition(int index, List<ASTStatement> astContent,
				ASTPosition parent){
			this.index = index;
			this.astContent = astContent;
			this.parent = parent;
		}
	}
	*/
}
