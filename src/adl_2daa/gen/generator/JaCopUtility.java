package adl_2daa.gen.generator;

import java.util.Random;

import org.jacop.constraints.Alldiff;
import org.jacop.constraints.Count;
import org.jacop.constraints.Sum;
import org.jacop.constraints.XeqC;
import org.jacop.core.Domain;
import org.jacop.core.IntDomain;
import org.jacop.core.IntVar;
import org.jacop.core.IntervalDomain;
import org.jacop.core.Store;
import org.jacop.search.DepthFirstSearch;
import org.jacop.search.IndomainRandom;
import org.jacop.search.RandomSelect;
import org.jacop.search.Search;
import org.jacop.search.SolutionListener;


public class JaCopUtility {

	private static int[][] cachedSolutions;
	
	/**
	 * Solve the given CSP problem and return a random solution. This method also
	 * caches all solution for further use.
	 */
	public static int[] randomUniformAssignment(Store store, IntVar[] vars){
		Search<IntVar> search = new DepthFirstSearch<IntVar>();
		search.setPrintInfo(false);
		SolutionListener<IntVar> solutions = search.getSolutionListener();
		RandomSelect<IntVar> select = new RandomSelect<IntVar>(vars, new IndomainRandom<IntVar>());
		solutions.searchAll(true);
		solutions.recordSolutions(true);
		search.labeling(store, select);
		
		System.out.println("Solution size : "+solutions.solutionsNo());
		cachedSolutions = new int[solutions.solutionsNo()][vars.length];
		Domain[] solution;
		Domain dom;
        for(int i=1; i<=solutions.solutionsNo(); i++){
        	solution = solutions.getSolution(i);
        	if(solution.length != vars.length){
        		System.out.println("Inconsistent solution");
        	}
        	for(int j=0; j<solution.length; j++){
        		dom = solution[j];
        		if(!dom.singleton() || dom.isEmpty()){
        			System.out.println("Strange solution domain");
        		}
        		cachedSolutions[i-1][j] = dom.valueEnumeration().nextElement();
        	}
        }
        int randomIndex = (new Random()).nextInt(cachedSolutions.length);
        return cachedSolutions[randomIndex];
	}
	
	/**
	 * Return a random solution from cached solutions. MUST NOT use if no cached solution
	 * exists
	 */
	public static int[] randomUniformAssignment(){
		int randomIndex = (new Random()).nextInt(cachedSolutions.length);
        return cachedSolutions[randomIndex];
	}
	
	/**
	 * Return cached solutions 
	 */
	public static int[][] allPrecalculatedAssignments(){
		return cachedSolutions;
	}
	
	/**
	 * Create variables with provided "non-negative" domains and solve AllDiff(vars).
	 * The solver tries to assign value to as many variables as possible.
	 * All possible solutions are cached and a random solution is returned.
	 * Negative assignment means the corresponding variable is not used.  
	 */
	public static int[] randomPartialAlldiffAssignment(IntDomain[] varsDom){
		//Create a clone of domains since searching change their state!
		IntDomain[] varsDomClone = new IntDomain[varsDom.length];
		for(int i=0; i<varsDom.length; i++){
			varsDomClone[i] = new IntervalDomain();
			varsDomClone[i].addDom(varsDom[i]);
		}
		
		//Setup first search for optimal cost
		Store store = new Store();
		IntVar[] vars = new IntVar[varsDom.length];
		IntVar costVar = setupPartialAlldiffAssignment(store, vars, varsDom);
		
		Search<IntVar> search = new DepthFirstSearch<IntVar>();
		search.setPrintInfo(false);
		RandomSelect<IntVar> select = new RandomSelect<IntVar>(vars, new IndomainRandom<IntVar>());
		search.labeling(store, select, costVar);
		
		//Perform another search with fixed optimal cost
		int minimumCost = costVar.value();
		store = new Store();
		vars = new IntVar[varsDom.length];
		costVar = setupPartialAlldiffAssignment(store, vars, varsDomClone);
		store.impose(new XeqC(costVar, minimumCost));
		return randomUniformAssignment(store, vars);
	}
	
	private static IntVar setupPartialAlldiffAssignment(Store store, IntVar[] vars,
			IntDomain[] varsDom){
		for(int i=0; i<vars.length; i++){
			vars[i] = new IntVar(store, varsDom[i]);
			vars[i].addDom(-(i+1), -(i+1));
		}
		store.impose(new Alldiff(vars));
		
		IntVar[] counter = new IntVar[vars.length];
		for(int i=0; i<vars.length; i++){
			counter[i] = new IntVar(store, 0, 1);
			store.impose(new Count(new IntVar[]{vars[i]}, counter[i], -(i+1)));
		}
		IntVar cost = new IntVar(store, 0, vars.length);
		store.impose(new Sum(counter, cost));
		
		return cost;
	}
}
