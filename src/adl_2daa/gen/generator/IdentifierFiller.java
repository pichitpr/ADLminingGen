package adl_2daa.gen.generator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.jacop.core.IntDomain;
import org.jacop.core.IntervalDomain;

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
import adl_2daa.gen.profile.ReachProfile;
import adl_2daa.jacop.CSPTemplate;
import adl_2daa.jacop.JaCopUtility;

public class IdentifierFiller {

	public static IdentifierFiller instance = new IdentifierFiller();
	
	private ReachProfile reachProfile;
	
	/*
	 * A map indicating all possible Goto/Spawn targets according to reach profile (only include target without reach) 
	 * for actions that has no specified target. Target Agent/State is specified by positive label instead of actual object.
	 */
	private HashMap<Action, List<Integer>> gotoTarget;
	private HashMap<Action, List<Integer>> spawnTarget;
	
	// A map indicating a sequence block that the action is in
	private HashMap<Action, List<ASTStatement>> spawnOwningSequence;
	
	/*
	 * Label map for Agent/State. Get the label using List#indexOf
	 */
	private List<Agent> agentLabelMap;
	private List<State> stateLabelMap;
	
	/**
	 * Fill all Goto/Spawn 's missing identifier and ensure that as many existing Agent/State are reachable. 
	 */
	//NOTE:: future feature: Spawn may be added to ensure all Agents are spawned. Goto may be added at the valid spot in the code
	//Reachability from first state???
	public void fillMissingIdentifier(Root root){
		reachProfile = new ReachProfile(root);
		gotoTarget = new HashMap<Action, List<Integer>>();
		spawnTarget = new HashMap<Action, List<Integer>>();
		spawnOwningSequence = new HashMap<Action, List<ASTStatement>>();
		constructEntityLabel(root);
		
		//First phase: Fill missing identifier so that unreachable target can be reached (as many as possible)
		populateTargetMap(root, true);
		assignTarget(true);
		assignTarget(false);
		
		//Second phase: Fill the remaining missing identifier
		gotoTarget.clear();
		spawnTarget.clear();
		populateTargetMap(root, false);
		for(Entry<Action, List<Integer>> entry : gotoTarget.entrySet()){
			entry.getKey().getParams()[0] = new Identifier("."+
					stateLabelMap.get(ASTUtility.randomUniform(entry.getValue())).getIdentifier()
					);
		}
		for(Entry<Action, List<Integer>> entry : spawnTarget.entrySet()){
			if(entry.getValue().size() == 0){
				//If no valid spawn target, trim Spawn()
				Action action = entry.getKey();
				List<ASTStatement> block = spawnOwningSequence.get(action);
				for(int i=0; i<block.size(); i++){
					if(block.get(i) == action){
						block.remove(i);
						break;
					}
				}
			}else{
				entry.getKey().getParams()[0] = new Identifier("."+
						agentLabelMap.get(ASTUtility.randomUniform(entry.getValue())).getIdentifier()
						);
			}
		}
	}
	
	private void constructEntityLabel(Root root){
		agentLabelMap = new LinkedList<Agent>();
		stateLabelMap = new LinkedList<State>();
		for(Agent agent : root.getRelatedAgents()){
			agentLabelMap.add(agent);
			for(State state : agent.getStates()){
				stateLabelMap.add(state);
			}
		}
	}
	
	private void populateTargetMap(Root root, boolean unreachableOnly){
		for(Agent agent : root.getRelatedAgents()){
			if(agent.getDes() != null){
				populateTargetMap(root, agent, null, agent.getDes().getStatements(), unreachableOnly);
			}
			for(State state : agent.getStates()){
				for(Sequence seq : state.getSequences()){
					populateTargetMap(root, agent, state, seq.getStatements(), unreachableOnly);
				}
			}
		}
	}
	
	private void populateTargetMap(Root owningRoot, Agent owningAgent, State owningState, List<ASTStatement> seq, 
			boolean unreachableOnly){
		for(ASTStatement st : seq){
			if(st instanceof Action){
				Action action = (Action)st;
				ASTExpression param;
				Agent mainAgent = null;
				List<Integer> labelList;
				if(action.getName().equals("Spawn")){
					param = action.getParams()[0];
					labelList = new LinkedList<Integer>();
					if(param instanceof ExpressionSkeleton){
						for(Agent agent : owningRoot.getRelatedAgents()){
							if(mainAgent == null){
								mainAgent = agent;
							}
							if(agent == owningAgent || agent == mainAgent){
								//Do not spawn its own OR main agent
								continue;
							}
							if(!unreachableOnly || !reachProfile.hasSpawnReach(agent)){
								labelList.add(agentLabelMap.indexOf(agent));
							}
						}
						spawnTarget.put(action, labelList);
						
						//Second phase, we need to collect enclosing block so that we can trim this action if needed
						if(!unreachableOnly){
							spawnOwningSequence.put(action, seq);
						}
					}
				}else if(action.getName().equals("Goto")){
					if(owningState == null){
						//Impossible case
						continue;
					}
					param = action.getParams()[0];
					labelList = new LinkedList<Integer>();
					if(param instanceof ExpressionSkeleton){
						for(State state : owningAgent.getStates()){
							if(state == owningState){
								//Goto  must not transition to its own state
								continue;
							}
							if(!unreachableOnly || !reachProfile.hasTransitionReach(state)){
								labelList.add(stateLabelMap.indexOf(state));
							}
						}
						gotoTarget.put(action, labelList);
					}
				}
			}else if(st instanceof Condition){
				Condition cond = (Condition)st;
				populateTargetMap(owningRoot, owningAgent, owningState, cond.getIfblock(), unreachableOnly);
				if(cond.getElseblock() != null){
					populateTargetMap(owningRoot, owningAgent, owningState, cond.getElseblock(), unreachableOnly);
				}
			}else{
				populateTargetMap(owningRoot, owningAgent, owningState, ((Loop)st).getContent(), unreachableOnly);
			}
		}
	}
	
	private void assignTarget(boolean assignForGoto){
		//TODO: Finish this -- no target case
		
		HashMap<Action, List<Integer>> possibleTargetMap = assignForGoto ? gotoTarget : spawnTarget;
		Set<Action> actions = possibleTargetMap.keySet();
		if(actions.size() == 0)
			return;
		IntDomain[] doms = new IntDomain[actions.size()];
		Iterator<Action> it = actions.iterator();
		int index=0;
		int noTargetLabel = Integer.MIN_VALUE; //Some actions may has no specific target by default
		//Convert possible target label into IntDomain
		while(it.hasNext()){
			List<Integer> possibleTarget = possibleTargetMap.get(it.next());
			if(possibleTarget.size() == 0){
				doms[index] = new IntervalDomain();
				doms[index].unionAdapt(noTargetLabel, noTargetLabel);
				noTargetLabel++;
			}else{
				doms[index] = new IntervalDomain();
				for(Integer targetLabel : possibleTarget){
					doms[index].unionAdapt(targetLabel, targetLabel);
				}
			}
			index++;
		}
		
		//Solve and assign target (if available)
		JaCopUtility.solveAllSolutionCSP(new CSPTemplate.BestEffortAssignment(doms));
		int[] assignment = JaCopUtility.randomUniformAssignment();
		assert(assignment.length == actions.size());
		it = actions.iterator();
		index = 0;
		while(it.hasNext()){
			Action noTargetAction = it.next();
			int label = assignment[index];
			if(label >= 0){
				//There is an assignment for actions[i], replace the identifier
				String identifier = assignForGoto ? stateLabelMap.get(label).getIdentifier() : agentLabelMap.get(label).getIdentifier();
				noTargetAction.getParams()[0] = new Identifier("."+identifier);
			}
			index++;
		}
	}
}