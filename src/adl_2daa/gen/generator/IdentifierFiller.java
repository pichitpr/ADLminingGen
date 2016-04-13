package adl_2daa.gen.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
import adl_2daa.gen.filter.ASTNodeFilter;
import adl_2daa.gen.profile.ReachProfile;
import adl_2daa.jacop.CSPTemplate;
import adl_2daa.jacop.CSPTemplate.BestEffortAssignmentVariedCost;
import adl_2daa.jacop.JaCopUtility;

public class IdentifierFiller {

	public static IdentifierFiller instance = new IdentifierFiller();
	
	//Convienient class for storing action and enclosing sequence block together
	private static class ActionInfo {
		private Agent agent;
		private State state;
		private Sequence sequence;
		private Action action;
		
		private ActionInfo(Agent agent, State state, Sequence sequence, Action action){
			this.agent = agent;
			this.state = state;
			this.sequence = sequence;
			this.action = action;
		}
	}
	
	//A class representing a position that new Goto can be inserted into the sequence
	private static class GotoStub {
		private State owningState;
		private ASTSequenceWrapper owningWrappedSequence;
		private int insertionIndex;
		
		private GotoStub(State owningState, ASTSequenceWrapper owningWrappedSequence, int insertionIndex) {
			this.owningState = owningState;
			this.owningWrappedSequence = owningWrappedSequence;
			this.insertionIndex = insertionIndex;
		}
	}
	
	private ReachProfile reachProfile;
	
	/*
	 * A map indicating all possible Goto/Spawn targets according to reach profile.
	 * Target Agent/State is specified by positive label instead of actual object. (Use indexOf to get the actual block from map)
	 */
	private HashMap<ActionInfo, HashSet<Integer>> spawnTarget;
	private HashMap<ActionInfo, HashSet<Integer>> gotoTarget;
	private HashMap<GotoStub, HashSet<Integer>> gotoStubTarget;
	
	/*
	 * A map indicating selected target. A default value -1 indicating no target
	 */
	private HashMap<ActionInfo, Integer> spawnChosenTarget;
	private HashMap<ActionInfo, Integer> gotoChosenTarget;
	private HashMap<GotoStub, Integer> gotoStubChosenTarget;
	
	///////////TODO: Populate map in single pass and separate target set for unreachableOnly or not
	
	/*
	 * Label map for Agent/State. Get the label using List#indexOf
	 */
	private List<Agent> agentLabelMap;
	private List<State> stateLabelMap;
	
	/**
	 * Fill all Goto/Spawn 's missing identifier and ensure that as many existing Agent/State are reachable.
	 * Goto() may be added to cover unreachable state. This method try to make as many states connected with initial state
	 * as possible 
	 */
	public void fillMissingIdentifier(Root root){
		reachProfile = new ReachProfile(root);
		spawnTarget = new HashMap<ActionInfo, HashSet<Integer>>();
		gotoTarget = new HashMap<ActionInfo, HashSet<Integer>>();
		gotoStubTarget = new HashMap<GotoStub, HashSet<Integer>>();
		spawnChosenTarget = new HashMap<ActionInfo, Integer>();
		gotoChosenTarget = new HashMap<ActionInfo, Integer>();
		gotoStubChosenTarget = new HashMap<GotoStub, Integer>();
		constructEntityLabel(root);
		
		//Phase 1 : try to cover unreachable Agent/State
		populateTargetMap(root, true, true);
		assignSpawnTargetWithCSP();
		assignGotoTargetWithCSP(root);
		
		//Phase 2 : apply result + fill the remaining
		populateTargetMap(root, false, false);
		applyTarget();
	}
	
	private void constructEntityLabel(Root root){
		agentLabelMap = new ArrayList<Agent>();
		stateLabelMap = new ArrayList<State>();
		for(Agent agent : root.getRelatedAgents()){
			agentLabelMap.add(agent);
			for(State state : agent.getStates()){
				stateLabelMap.add(state);
			}
		}
	}

	/**
	 * Populate target for all target maps. The result is appended to the existing one. 
	 * If unreachableOnly is true, this method only consider Agent/State as target
	 * if it has no reach according to current reachProfile. This method also helps setup chosen target with default value (if needed).
	 */
	private void populateTargetMap(Root root, boolean unreachableOnly, boolean setupChosenTarget){
		for(Agent agent : root.getRelatedAgents()){
			if(agent.getDes() != null){
				populateTargetMap(root, agent, null, agent.getDes(), agent.getDes().getStatements(), 
						unreachableOnly, setupChosenTarget);
			}
			for(State state : agent.getStates()){
				for(Sequence seq : state.getSequences()){
					populateTargetMap(root, agent, state, seq, seq.getStatements(), unreachableOnly, setupChosenTarget);
					ASTSequenceWrapper wrappedSequence = new ASTSequenceWrapper(seq.getStatements());
					List<Integer> eobSlots = wrappedSequence.getValidSlots(
							ASTNodeFilter.eobTransitionSlotFilter(null, false, false)
							);
					if(eobSlots.isEmpty()) continue;
					
					//Setup GotoStub
					HashSet<Integer> labelSet = new HashSet<Integer>();
					for(State _state : agent.getStates()){
						if(state == _state){
							//Goto must not transition to its own state
							continue;
						}
						if(!unreachableOnly || !reachProfile.hasTransitionReach(_state)){
							labelSet.add(stateLabelMap.indexOf(_state));
						}
					}
					for(Integer stubIndex : eobSlots){
						GotoStub stub = new GotoStub(state, wrappedSequence, stubIndex);
						gotoStubTarget.put(stub, new HashSet<Integer>(labelSet));
						if(setupChosenTarget)
							gotoStubChosenTarget.put(stub, -1);
					}
				}
			}
		}
	}
	
	private void populateTargetMap(Root owningRoot, Agent owningAgent, State owningState, Sequence owningSequence, 
			List<ASTStatement> enclosingBlock, boolean unreachableOnly, boolean setupChosenTarget){
		for(ASTStatement st : enclosingBlock){
			if(st instanceof Action){
				Action action = (Action)st;
				ASTExpression param;
				Agent mainAgent = null;
				HashSet<Integer> labelSet;
				ActionInfo actionInfo;
				if(action.getName().equals("Spawn")){
					param = action.getParams()[0];
					labelSet = new HashSet<Integer>();
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
								labelSet.add(agentLabelMap.indexOf(agent));
							}
						}
						actionInfo = new ActionInfo(owningAgent, owningState, owningSequence, action);
						spawnTarget.put(actionInfo, labelSet);
						if(setupChosenTarget)
							spawnChosenTarget.put(actionInfo, -1);
					}
				}else if(action.getName().equals("Goto")){
					if(owningState == null){
						//Impossible case
						continue;
					}
					param = action.getParams()[0];
					labelSet = new HashSet<Integer>();
					if(param instanceof ExpressionSkeleton){
						for(State state : owningAgent.getStates()){
							if(state == owningState){
								//Goto  must not transition to its own state
								continue;
							}
							if(!unreachableOnly || !reachProfile.hasTransitionReach(state)){
								labelSet.add(stateLabelMap.indexOf(state));
							}
						}
						actionInfo = new ActionInfo(owningAgent, owningState, owningSequence, action);
						gotoTarget.put(actionInfo, labelSet);
						if(setupChosenTarget)
							gotoChosenTarget.put(actionInfo, -1);
					}
				}
			}else if(st instanceof Condition){
				Condition cond = (Condition)st;
				populateTargetMap(owningRoot, owningAgent, owningState, owningSequence, cond.getIfblock(), 
						unreachableOnly, setupChosenTarget);
				if(cond.getElseblock() != null){
					populateTargetMap(owningRoot, owningAgent, owningState, owningSequence, cond.getElseblock(), 
							unreachableOnly, setupChosenTarget);
				}
			}else{
				populateTargetMap(owningRoot, owningAgent, owningState, owningSequence, ((Loop)st).getContent(), 
						unreachableOnly, setupChosenTarget);
			}
		}
	}
	
	/**
	 * Assign spawn target (in the map, not applying to the actual sequence) using BestEffortAllDiff.
	 * ReachProfile is also updated. Spawn target map must be populated before calling this.
	 */
	private void assignSpawnTargetWithCSP(){
		if(spawnTarget.size() == 0){
			//Do nothing if there is no Spawn(#IDENTIFIER)
			return;
		}
		Set<ActionInfo> actions = spawnTarget.keySet();
		IntDomain[] doms = new IntDomain[actions.size()];
		Iterator<ActionInfo> it = actions.iterator();
		int index=0;
		int noTargetLabel = Integer.MIN_VALUE; //Some actions may has no specific target by default
		//Convert possible target label into IntDomain
		while(it.hasNext()){
			HashSet<Integer> possibleTarget = spawnTarget.get(it.next());
			if(possibleTarget.isEmpty()){
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
			ActionInfo actionInfo = it.next();
			int label = assignment[index];
			if(label >= 0){
				//There is an assignment for actions[i], set result and update ReachProfile
				spawnChosenTarget.put(actionInfo, label);
				reachProfile.updateSpawnReach(agentLabelMap.get(label), actionInfo.agent);
			}
			index++;
		}
	}
	
	/**
	 * Assign goto target (in the map, not applying to the actual sequence) using BestEffortAllDiffVariedCost.
	 * This method tries to cover all unreachable state without using stub first. This method update ReachProfile. 
	 * Goto/GotoStub target map must be populated before calling this.
	 */
	private void assignGotoTargetWithCSP(Root root){
		if(gotoTarget.size()+gotoStubTarget.size() == 0){
			//Do nothing if there is no Goto(#IDENTIFIER) and no slot to insert Goto stub
			return;
		}
		
		/*
		 * Cost usage:
		 * Goto is assigned with target : 0 (Best case)
		 * Goto is not assigned : 100 (Should use as many Goto as possible -- higher priority than using stub)
		 * GotoStub is assigned : 10 (Only use stub as needed)
		 * GotoStub is not assigned : 0 (Preferred)
		 */
		
		int costThreshold = 1000000; //Actually, we just want all solutions
		
		//Solve with cost threshold mode
		BestEffortAssignmentVariedCost csp = new CSPTemplate.BestEffortAssignmentVariedCost(
				gotoTarget.size()+gotoStubTarget.size());
		int index = 0;
		for(Entry<ActionInfo,HashSet<Integer>> entry : gotoTarget.entrySet()){
			for(Integer target : entry.getValue()){
				csp.putAssignmentCost(index, target, 0);
			}
			csp.finalizeCostTableSetup(index, 100);
			index++;
		}
		for(Entry<GotoStub,HashSet<Integer>> entry : gotoStubTarget.entrySet()){
			for(Integer target : entry.getValue()){
				csp.putAssignmentCost(index, target, 10);
			}
			csp.finalizeCostTableSetup(index, 0);
			index++;
		}
		JaCopUtility.solveAllSolutionCSP(csp, costThreshold);
		int[][] solutions = JaCopUtility.allPrecalculatedAssignments();
		assert(solutions.length > 0); //Always have solution because BestEffort approach
		
		//Sort solutions by cost (lower cost comes first)
		final int[] solutionCost = new int[solutions.length];
		Integer[] solutionIndices = new Integer[solutions.length];
		for(int i=0; i<solutions.length; i++){
			solutionIndices[i] = i;
			for(int j=1; j<solutions[i].length; j+=2){
				solutionCost[i] += solutions[i][j];
			}
		}
		Arrays.sort(solutionIndices, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return Integer.compare(solutionCost[o1], solutionCost[o2]);
			}
		});
		
		//Find all solutions with valid reach OR highest count of all-states-reached agent and using the least cost
		int highestAllReachedAgentCount = 0;
		int lowestCostUsed = Integer.MAX_VALUE;
		List<Integer> validSolutionIndices = new ArrayList<Integer>();
		for(Integer solIndex : solutionIndices){
			//Check reachability of solution
			int[] checkedSolution = solutions[solIndex];
			assert(checkedSolution.length/2 == gotoTarget.size()+gotoStubTarget.size());
			
			//First, setup temporary ReachProfile using current solution that we want to check
			ReachProfile tempProfile = new ReachProfile(root);
			index = 0;
			for(ActionInfo actionInfo : gotoTarget.keySet()){
				int target = checkedSolution[index*2];
				if(target >= 0){
					tempProfile.updateTransitionReach(stateLabelMap.get(target), actionInfo.state);
				}
				index++;
			}
			for(GotoStub stub : gotoStubTarget.keySet()){
				int target = checkedSolution[index*2];
				if(target >= 0){
					tempProfile.updateTransitionReach(stateLabelMap.get(target), stub.owningState);
				}
				index++;
			}
			
			//Get a number of agent that has all-state-reached property
			int allReachedAgentCount = 0;
			for(Agent agent : root.getRelatedAgents()){
				if(tempProfile.initialStateCanReachAllOthers(agent)){
					allReachedAgentCount++;
				}
			}
			
			//Record value if the solution is good enough OR reset if there is better one
			if(allReachedAgentCount > highestAllReachedAgentCount){
				highestAllReachedAgentCount = allReachedAgentCount;
				lowestCostUsed = solutionCost[solIndex];
				validSolutionIndices.clear();
				validSolutionIndices.add(solIndex);
			}else if(allReachedAgentCount == highestAllReachedAgentCount){
				if(lowestCostUsed > solutionCost[solIndex]){
					lowestCostUsed = solutionCost[solIndex];
					validSolutionIndices.clear();
				}
				validSolutionIndices.add(solIndex);
			}
		}
		
		//If(!empty) randomly pick 1 Else randomly pick 1 solution with least cost
		if(validSolutionIndices.isEmpty()){
			lowestCostUsed = solutionCost[0];
			for(int i=0; i<solutionIndices.length; i++){
				if(solutionCost[i] <= lowestCostUsed){
					validSolutionIndices.add(solutionIndices[i]);
				}
			}
		}
		int[] selectedSolution = solutions[ASTUtility.randomUniform(validSolutionIndices)];
		
		//Update chosen target and actual ReachProfile
		index = 0;
		for(ActionInfo actionInfo : gotoTarget.keySet()){
			int target = selectedSolution[index*2];
			if(target >= 0){
				gotoChosenTarget.put(actionInfo, target);
				reachProfile.updateTransitionReach(stateLabelMap.get(target), actionInfo.state);
			}
			index++;
		}
		for(GotoStub stub : gotoStubTarget.keySet()){
			int target = selectedSolution[index*2];
			if(target >= 0){
				gotoStubChosenTarget.put(stub, target);
				reachProfile.updateTransitionReach(stateLabelMap.get(target), stub.owningState);
			}
			index++;
		}
	}
	
	/**
	 * Assign target for an action, also generate Goto from stub and insert into sequence. If target is not specified for an action,
	 * this method randomly pick 1 target from target map. If there is no available target for picking, this method remove the
	 * action from its sequence. Target map must be populated before calling.
	 */
	private void applyTarget(){
		//Insert stub first since insertion relies on slot index which can be changed during target assignment phase
		HashSet<ASTSequenceWrapper> allWrappedSequences = new HashSet<ASTSequenceWrapper>();
		for(Entry<GotoStub,Integer> stubTargetPair : gotoStubChosenTarget.entrySet()){
			int targetLabel = stubTargetPair.getValue();
			if(targetLabel > -1){
				GotoStub stub = stubTargetPair.getKey();
				List<ASTExpression> param = new ArrayList<ASTExpression>();
				param.add(new Identifier("."+stateLabelMap.get(targetLabel)));
				stub.owningWrappedSequence.queueInsertion(stub.insertionIndex, new Action("Goto", param));
				allWrappedSequences.add(stub.owningWrappedSequence);
			}
		}
		for(ASTSequenceWrapper wrappedSequence : allWrappedSequences){
			wrappedSequence.finalizeWrapper();
		}
		
		//Assign Goto() target, remove an action if needed
		
		//Assign Spawn() target, remove an action if needed
		for(Entry<ActionInfo,Integer> spawnTargetPair : spawnChosenTarget.entrySet()){
			Action spawnAction = spawnTargetPair.getKey().action;
			int targetLabel = spawnTargetPair.getValue();
			if(targetLabel == -1){
				//No assign label, try to get one randomly
				HashSet<Integer> allPossibleTarget = spawnTarget.get(spawnTargetPair.getKey());
				if(!allPossibleTarget.isEmpty()){
					//////////////////////////////////////////////
				}
			}
			if(targetLabel == -1){
				
			}else{
				spawnAction.getParams()[0] = new Identifier("."+agentLabelMap.get(targetLabel).getIdentifier());
			}
		}
	}
	
	/*
	private void assignTarget(boolean assignForGoto){		
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
	*/
}