package adl_2daa.gen.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import lcs.LCSSequenceEmbedding;
import lcs.SimpleLCSEmbedding;

import org.jacop.constraints.Alldiff;
import org.jacop.constraints.Sum;
import org.jacop.constraints.regular.Regular;
import org.jacop.core.IntDomain;
import org.jacop.core.IntVar;
import org.jacop.core.IntervalDomain;
import org.jacop.core.Store;
import org.jacop.util.fsm.FSM;
import org.jacop.util.fsm.FSMState;
import org.jacop.util.fsm.FSMTransition;

import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;
import adl_2daa.gen.generator.ASTNode;
import adl_2daa.gen.generator.ASTSequenceWrapper;
import adl_2daa.jacop.CSPInstance;
import adl_2daa.jacop.CSPTemplate;
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
				CSPTemplate allocationProblem = new EOBTransitionAllocationProblem(eobDomains);
				int allocationCost = JaCopUtility.solveAllSolutionCSP(allocationProblem);
				if(allocationCost <= lowestAllocationCost){
					//Valid solutions found, record all of them
					if(allocationCost < lowestAllocationCost){
						//New better coverage, wipe all solutions found so far
						filteredAgent.clear();
						filteredState.clear();
						lowestAllocationCost = allocationCost;
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
	
	
	private static class EOBTransitionAllocationProblem implements CSPTemplate{

		private IntDomain[] dom;
		public EOBTransitionAllocationProblem(IntDomain[] dom){
			this.dom = dom;
		}
		
		@Override
		public CSPInstance newInstance() {
			IntDomain[] varsDomClone = new IntDomain[dom.length];
			for(int i=0; i<dom.length; i++){
				varsDomClone[i] = new IntervalDomain();
				varsDomClone[i].addDom(dom[i]);
			}
			
			Store store = new Store();
			IntVar[] vars = new IntVar[varsDomClone.length];
			for(int i=0; i<vars.length; i++){
				vars[i] = new IntVar(store);
				vars[i].addDom(varsDomClone[i]);
			}
			JaCopUtility.prepareVarsForPartialAllDiff(vars);
			IntVar costVar = JaCopUtility.imposePartialAllDiff(store, vars);
			
			return new CSPInstance(store, vars, costVar);
		}
		
	}
	
	/**
	 * Filter agent(s) with state(s) containing sequences that can match all given Spawn().
	 * If all Spawn() can not be matched, this method tries to match as much as possible.
	 * This method try to match using LCSEmbedding + spawnComparator which checks for
	 * nesting condition. Any state with more than 1 solution will have duplicate 
	 * ResultState under the same ResultAgent with different containing sequences. 
	 * The sequences in ResultState are
	 * - Having their length equals to relation size (sequence count)
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
				//System.out.println("======== state "+state.getActualState().getIdentifier()+" =======");
				//Testing for current state
				SpawnMatchProblem spawnMatchCSP = new SpawnMatchProblem(
						wrappedRel.size(), state.getResultSequences().size());
				
				//Construct skel-rel non-match cost table
				int relSeqIndex = 0;
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
						spawnMatchCSP.putNonMatchCost(relSeqIndex, skelSeqIndex, nonMatchCost);
						skelSeqIndex++;
					}
					
					//Also add cost if relSeq is not matched with any skelSeq
					spawnMatchCSP.putNotMatchedSequenceCase(relSeqIndex, highestNonMatchCost);
					
					relSeqIndex++;
				}
				//spawnMatchCSP.printNonMatchCostTable();
				//Solve and record if current state is qualified
				int usedNonMatchCost = JaCopUtility.solveAllSolutionCSP(spawnMatchCSP);
				if(usedNonMatchCost <= lowestNonMatchCost){
					if(usedNonMatchCost < lowestNonMatchCost){
						//New better solution, older result(s) is obsolete
						filteredAgent.clear();
						filteredState.clear();
						lowestNonMatchCost = usedNonMatchCost;
					}

					//Record all solutions
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
	
	/**
	 * CSP problem for matching multiple spawn() in merging parallel relation with existing
	 * spawn() in skeleton as many as possible.<br/>
	 * Result: <br/>
	 * - var[i*2] : specify a sequence that i-th relation should be matched to.
	 * If the value is less than 0, this means the relation cannot be matched.
	 * - var[i*2+1] : related match score for i-th relation. Usually not used.
	 */
	private static class SpawnMatchProblem implements CSPTemplate{

		//Every lowest non-match cost (highest match score) skel-rel sequence pairs
		private HashMap<Integer,Integer>[] nonMatchCostTable; //<skelSeqIndex, nonMatchCost>
		private int skelSeqCount;
		private int uniqueDomain; //Unique value for relSeq when no skelSeq match
		
		@SuppressWarnings("unchecked")
		public SpawnMatchProblem(int relSeqCount, int skelSeqCount){
			nonMatchCostTable = new HashMap[relSeqCount];
			for(int i=0; i<relSeqCount; i++){
				nonMatchCostTable[i] = new HashMap<Integer,Integer>();
			}
			this.skelSeqCount = skelSeqCount;
			uniqueDomain = -1;
		}
		
		public void putNonMatchCost(int relSeqIndex, int skelSeqIndex, int nonMatchCost){
			nonMatchCostTable[relSeqIndex].put(skelSeqIndex, nonMatchCost);
		}
		
		public void putNotMatchedSequenceCase(int relSeqIndex, int highestNonMatchCost){
			putNonMatchCost(relSeqIndex, uniqueDomain, highestNonMatchCost);
			uniqueDomain--;
		}
		
		public void printNonMatchCostTable(){
			int relSeqIndex = 0;
			for(HashMap<Integer,Integer> hash : nonMatchCostTable){
				System.out.println("Relation sequence #"+relSeqIndex);
				for(Entry<Integer,Integer> entry : hash.entrySet()){
					System.out.println("Skel "+entry.getKey()+"= "+
							entry.getValue());
				}
				relSeqIndex++;
			}
		}
		
		@Override
		public CSPInstance newInstance() {
			Store store = new Store();
			//vars[i*2] = skelSeqIndex for i-th relation sequence
			//vars[i*2+1] = non-match cost for i-th relation sequence when skelSeqIndex is selected
			IntVar[] vars = new IntVar[nonMatchCostTable.length*2];
			IntVar[] allDiffVars = new IntVar[nonMatchCostTable.length];
			IntVar[] allMatchScoreVars = new IntVar[nonMatchCostTable.length];
			IntVar costVar = new IntVar(store, 0, Integer.MAX_VALUE);
			
			//Every <skelSeqIndex, non-match cost> pairs for each relation sequence
			for(int i=0; i<nonMatchCostTable.length; i++){
				vars[i*2] = new IntVar(store, 0, skelSeqCount-1);
				//vars[i*2+1] = new IntVar(store, 0, Integer.MAX_VALUE);
				vars[i*2+1] = new IntVar(store);
				FSM fsm = new FSM();
				FSMState start = new FSMState();
				FSMState end = new FSMState();
				fsm.allStates.add(start);
				fsm.allStates.add(end);
				fsm.initState = start;
				fsm.finalStates.add(end);
				for(Entry<Integer,Integer> entry : nonMatchCostTable[i].entrySet()){
					FSMState intermediate = new FSMState();
					fsm.allStates.add(intermediate);
					start.transitions.add(new FSMTransition(
							new IntervalDomain(entry.getKey(), entry.getKey()), intermediate)
							);
					intermediate.transitions.add(new FSMTransition(
							new IntervalDomain(entry.getValue(), entry.getValue()), end)
							);
					//If this is <notMatchedID, highestNonMatchCost> case, setup nonMatchCost var domain
					if(entry.getKey() < 0){
						vars[i*2+1].addDom(0, entry.getValue());
					}
				}
				store.impose(new Regular(fsm, new IntVar[]{vars[i*2], vars[i*2+1]} ));
				
				allDiffVars[i] = vars[i*2];
				allMatchScoreVars[i] = vars[i*2+1];
			}
			
			store.impose(new Alldiff(allDiffVars));
			store.impose(new Sum(allMatchScoreVars, costVar));
			
			return new CSPInstance(store, vars, costVar);
		}
	}
}
