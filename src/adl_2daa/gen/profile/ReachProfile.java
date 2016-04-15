package adl_2daa.gen.profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

public class ReachProfile {
	
	/*
	 * A map indicating agent/state/seq --> List<Spawner/StartingState/DespawnCaller>
	 */
	private HashMap<Agent, List<Agent>> agentReach;
	private HashMap<State, List<State>> stateReach;
	private HashMap<Sequence, List<State>> desBlockReach;
	
	/**
	 * Generate empty reach profile and populate.
	 */
	public ReachProfile(Root root){
		agentReach = new HashMap<Agent, List<Agent>>();
		stateReach = new HashMap<State, List<State>>();
		desBlockReach = new HashMap<Sequence, List<State>>();
		boolean prepareMainAgent = false;
		boolean prepareInitialState = false;
		for(Agent agent : root.getRelatedAgents()){
			agentReach.put(agent, new ArrayList<Agent>());
			if(!prepareMainAgent){
				agentReach.get(agent).add(null); //Main agent always has at least 1 spawner
				prepareMainAgent = true;
			}
			prepareInitialState = false;
			for(State state : agent.getStates()){
				stateReach.put(state, new ArrayList<State>());
				if(!prepareInitialState){
					stateReach.get(state).add(null); //Initial state always has at least 1 reach
					prepareInitialState = true;
				}
			}
			//For profiling purpose, we don't need to care about .des much as it is guaranteed to be reached if the agent is destroyed
			if(agent.getDes() != null){
				desBlockReach.put(agent.getDes(), new ArrayList<State>());
			}
		}
		populate(root);
	}
	
	public boolean hasSpawnReach(Agent agent){
		if(!agentReach.containsKey(agent))
			return false;
		return !agentReach.get(agent).isEmpty();
	}
	
	public boolean hasTransitionReach(State state){
		if(!stateReach.containsKey(state))
			return false;
		return !stateReach.get(state).isEmpty();
	}
	
	public void updateSpawnReach(Agent agent, Agent spawner){
		if(!agentReach.containsKey(agent)){
			agentReach.put(agent, new ArrayList<Agent>());
		}
		agentReach.get(agent).add(spawner);
	}
	
	public void updateTransitionReach(State state, State startingState){
		if(!stateReach.containsKey(state)){
			stateReach.put(state, new ArrayList<State>());
		}
		stateReach.get(state).add(startingState);
	}
	
	public boolean initialStateCanReachAllOthers(Agent agent){
		//Do not explore if provided agent is not profiled
		if(!agentReach.containsKey(agent) || agent.getStates().size() == 0){
			return false;
		}
		//[i][j] is true means that there is a transition from i-th state to j-th state 
		boolean[][] stateReachTable = new boolean[agent.getStates().size()][agent.getStates().size()];
		//Build initial reach table
		for(int i=0; i<agent.getStates().size(); i++){
			State targetState = agent.getStates().get(i);
			stateReachTable[i][i] = true;
			//Stop exploring if untracked state found
			if(!stateReach.containsKey(targetState)){
				return false;
			}
			for(State startingState : stateReach.get(targetState)){
				if(startingState == null){
					//Skip null symbol (used to indicate initial state)
					continue;
				}
				int startingStateIndex = agent.getStates().indexOf(startingState);
				//Stop exploring if untracked state found
				if(startingStateIndex == -1){
					return false;
				}
				stateReachTable[startingStateIndex][i] = true;
			}
		}
		//Check if there is a reach to all other nodes
		for(int i=1; i<agent.getStates().size(); i++){
			if(!hasStateReach(0, i, stateReachTable, new boolean[stateReachTable.length]))
				return false;
		}
		return true;
	}
	
	private boolean hasStateReach(int from, int to, boolean[][] reachTable, boolean[] explored){
		if(reachTable[from][to]) return true;
		if(explored[from]) return false;
		explored[from] = true;
		for(int j=0; j<reachTable[from].length; j++){
			if(j == from || j == to)
				continue;
			//Recursively do DFS using available edge from->j as stepping stone
			if(reachTable[from][j]){
				if(hasStateReach(j, to, reachTable, explored)){
					return true;
				}
			}
		}
		return false;
	}
	
	private void populate(Root root){
		for(Agent agent : root.getRelatedAgents()){
			if(agent.getDes() != null){
				search(root, agent, null, agent.getDes().getStatements());
			}
			for(State state : agent.getStates()){
				for(Sequence seq : state.getSequences()){
					search(root, agent, state, seq.getStatements());
				}
			}
		}
	}
	
	/**
	 * Search the block under owningRoot -> owningAgent [-> owningState] for key action and populate the profile.
	 * If it is .des block, owningState must be set to null
	 */
	private void search(Root owningRoot, Agent owningAgent, State owningState, List<ASTStatement> block){
		for(ASTStatement st : block){
			if(st instanceof Action){
				populateWithThisAction(owningRoot, owningAgent, owningState, (Action)st);
			}else if(st instanceof Condition){
				Condition cond = (Condition)st;
				search(owningRoot, owningAgent, owningState, cond.getIfblock());
				if(cond.getElseblock() != null)
					search(owningRoot, owningAgent, owningState, cond.getElseblock());
			}else{
				search(owningRoot, owningAgent, owningState, ((Loop)st).getContent());
			}
		}
	}
	
	/**
	 * Check if the provided action found under owningRoot -> owningAgent [-> owningState] is a valid key action. If so, 
	 * add to the profile indicating that specific Agent is spawned by owningAgent (for Spawn) OR 
	 * State/.des can be reached from owningState (for Goto/Despawn) <br/>
	 * owningState == null means the action is under .des block of owningAgent
	 */
	private void populateWithThisAction(Root owningRoot, Agent owningAgent, State owningState, Action action){
		if(owningState == null && !action.getName().equals("Spawn")){
			return;
		}
		String name = action.getName();
		if(name.equals("Spawn")){
			ASTExpression param = action.getParams()[0]; 
			if(param instanceof Identifier){
				//Spawn(X) : X is spawned by owningAgent
				String iden = ((Identifier)param).getValue();
				for(Agent spawnTarget : owningRoot.getRelatedAgents()){
					if(spawnTarget.getIdentifier().equals(iden)){
						agentReach.get(spawnTarget).add(owningAgent);
					}
				}
			}
		}else if(name.equals("Goto")){
			if(action.getParams()[0] instanceof Identifier){
				//Goto(X) : X can be reached from owningState
				String iden = ((Identifier)(action.getParams()[0])).getValue();
				for(State gotoTarget : owningAgent.getStates()){
					if(gotoTarget.getIdentifier().equals(iden)){
						stateReach.get(gotoTarget).add(owningState);
					}
				}
			}
		}else if(name.equals("Despawn")){
			if(owningAgent.getDes() != null){
				//owningAgent .des block can be reached from owningState
				desBlockReach.get(owningAgent.getDes()).add(owningState);
			}
		}
	}
	
	public String profileToString(Root root){
		StringBuilder strb = new StringBuilder();
		for(Agent agent : root.getRelatedAgents()){
			strb.append("Agent: ").append(agent.getIdentifier()).append(" ").append(agentReach.get(agent).size()).append("\n");
			if(agent.getDes() != null){
				strb.append("\t.des: ").append(desBlockReach.get(agent.getDes()).size()).append("\n");
			}
			for(State state : agent.getStates()){
				strb.append("\tState: .").append(state.getIdentifier()).append(" ").append(stateReach.get(state).size()).append("\n");
			}
		}
		return strb.toString();
	}
}
