package adl_2daa.jacop;

import org.jacop.constraints.Alldiff;
import org.jacop.constraints.Count;
import org.jacop.constraints.Sum;
import org.jacop.core.IntDomain;
import org.jacop.core.IntVar;
import org.jacop.core.IntervalDomain;
import org.jacop.core.Store;


public interface CSPTemplate {
	public CSPInstance newInstance();
	
	/**
	 * CSP for assigning variables as many as possible using value from provided domain under AllDiff constraint.
	 * Variables that are not assigned value will be given negative value instead. NOTE, All elements in provided domain
	 * must be non-negative.
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
}
