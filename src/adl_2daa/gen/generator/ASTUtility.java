package adl_2daa.gen.generator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;

public class ASTUtility {

	private static Random rand = new Random(1000);
	
	/**
	 * Return [start, end]
	 */
	private static int randomRange(int start, int end){
		return start+rand.nextInt(end-start+1);
	}
	
	public static Agent randomUniformAgent(Root root){
		return root.getRelatedAgents().get(randomRange(0, root.getRelatedAgents().size()-1));
	}
	
	public static State randomUniformState(Agent agent){
		return agent.getStates().get(randomRange(0, agent.getStates().size()-1));
	}
	
	public static Sequence randomUniformSequence(State state){
		return state.getSequences().get(randomRange(0, state.getSequences().size()-1));
	}
	
	public static Agent randomAgentAndGen(Root root, int requiredStateCount){
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
	
	public static Agent createEmptyAgent(String identifier){
		List<State> states = new LinkedList<State>();
		states.add(createEmptyState("state0"));
		Agent agent = new Agent(identifier, 
				createEmptySequence("init"), 
				createEmptySequence("des"), 
				states);
		return agent;
	}
	
	public static State createEmptyState(String identifier){
		List<Sequence> sequences = new LinkedList<Sequence>();
		sequences.add(createEmptySequence("seq0"));
		State state = new State(identifier, sequences);
		return state;
	}
	
	public static Sequence createEmptySequence(String identifier){
		List<ASTStatement> statement = new LinkedList<ASTStatement>();
		Sequence seq = new Sequence(identifier, statement);
		return seq;
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
