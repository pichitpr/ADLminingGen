package adl_2daa.jacop;

import java.util.HashMap;
import java.util.Map.Entry;

import org.jacop.constraints.Alldiff;
import org.jacop.constraints.Count;
import org.jacop.constraints.Sum;
import org.jacop.constraints.regular.Regular;
import org.jacop.core.IntDomain;
import org.jacop.core.IntVar;
import org.jacop.core.IntervalDomain;
import org.jacop.core.Store;
import org.jacop.util.fsm.FSM;
import org.jacop.util.fsm.FSMState;
import org.jacop.util.fsm.FSMTransition;


public interface CSPTemplate {
	public CSPInstance newInstance();
	
	/**
	 * CSP for assigning variables as many as possible using value from provided domain under AllDiff constraint.
	 * Variables that are not assigned value will be given negative value instead. NOTE, All elements in provided domain
	 * must be non-negative as negative value is considered "no assignment".
	 */
	public static class BestEffortAssignment implements CSPTemplate{

		private IntDomain[] dom;
		public BestEffortAssignment(IntDomain[] dom){
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
			
			IntVar[] costCounters = new IntVar[vars.length];
			int uniqueIndex = -1;
			for(int i=0; i<costCounters.length; i++){
				vars[i].addDom(uniqueIndex, uniqueIndex);
				costCounters[i] = new IntVar(store, 0, 1);
				store.impose(new Count(vars, costCounters[i], uniqueIndex));
				uniqueIndex--;
			}
			IntVar costVar = new IntVar(store, 0, vars.length);
			store.impose(new Sum(costCounters, costVar));
			store.impose(new Alldiff(vars));
			
			return new CSPInstance(store, vars, costVar);
		}
	}
	
	/**
	 * CSP for assigning variables as many as possible using value from provided domain under AllDiff constraint.
	 * This class differs from BestEffortAssignment that different assignment of the same variable has different cost.
	 * Assignment -> Cost is designated by cost table.<br/> 
	 * CSP Result will have array length equals to variableCount*2 where: <br/>
	 * result[i*2] = assignment of i-th variable, negative is considered "no assignment" <br/>
	 * result[i*2+1] = cost of assignment for i-th variable
	 */
	public static class BestEffortAssignmentVariedCost implements CSPTemplate {
		
		private HashMap<Integer,Integer>[] costTable; //costTable[variableIndex][assigned value] = cost
		private int uniqueDomain; //Unique value for "not assigned" case
		
		@SuppressWarnings("unchecked")
		public BestEffortAssignmentVariedCost(int variableCount){
			costTable = new HashMap[variableCount];
			uniqueDomain = -1;
		}
		
		/**
		 * Add data to cost table. Cost must be positive
		 */
		public void putAssignmentCost(int variableIndex, int assignedValue, int cost){
			if(costTable[variableIndex] == null){
				costTable[variableIndex] = new HashMap<Integer, Integer>();
			}
			costTable[variableIndex].put(assignedValue, cost);
		}
		
		/**
		 * Add no assignment cost. Normally, The cost should be the highest amongst all possible assignment for the variable
		 */
		public void finalizeCostTableSetup(int variableIndex, int noAssignmentCost){
			putAssignmentCost(variableIndex, uniqueDomain, noAssignmentCost);
			uniqueDomain--;
		}
		
		@Override
		public CSPInstance newInstance() {
			Store store = new Store();
			IntVar[] vars = new IntVar[costTable.length*2];
			IntVar[] allDiffVars = new IntVar[costTable.length];
			IntVar[] allMatchScoreVars = new IntVar[costTable.length];
			IntVar costVar = new IntVar(store, 0, Integer.MAX_VALUE);
			
			//Loop through all data in cost table to setup assignment constraint (If variable is assigned X, cost must be Y)
			for(int i=0; i<costTable.length; i++){
				int varMin = Integer.MAX_VALUE;
				int varMax = Integer.MIN_VALUE;
				for(Integer domainVal : costTable[i].keySet()){
					if(domainVal < varMin) varMin = domainVal;
					if(domainVal > varMax) varMax = domainVal;
				}
				if(varMin > uniqueDomain)
					varMin = uniqueDomain;
				vars[i*2] = new IntVar(store, varMin, varMax);
				vars[i*2+1] = new IntVar(store);
				FSM fsm = new FSM();
				FSMState start = new FSMState();
				FSMState end = new FSMState();
				fsm.allStates.add(start);
				fsm.allStates.add(end);
				fsm.initState = start;
				fsm.finalStates.add(end);
				//For each assignment and corresponding cost of i-th variable
				for(Entry<Integer,Integer> entry : costTable[i].entrySet()){
					FSMState intermediate = new FSMState();
					fsm.allStates.add(intermediate);
					start.transitions.add(new FSMTransition(
							new IntervalDomain(entry.getKey(), entry.getKey()), intermediate)
							);
					intermediate.transitions.add(new FSMTransition(
							new IntervalDomain(entry.getValue(), entry.getValue()), end)
							);
					//Also setup domain for corresponding cost
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
