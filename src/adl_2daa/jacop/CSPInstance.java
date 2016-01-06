package adl_2daa.jacop;
import org.jacop.core.IntVar;
import org.jacop.core.Store;


public class CSPInstance {
	private Store store;
	private IntVar[] vars;
	private IntVar costVar;
	
	public CSPInstance(Store store, IntVar[] vars, IntVar costVar) {
		super();
		this.store = store;
		this.vars = vars;
		this.costVar = costVar;
	}

	public Store getStore() {
		return store;
	}

	public IntVar[] getVars() {
		return vars;
	}

	public IntVar getCostVar() {
		return costVar;
	}
}
