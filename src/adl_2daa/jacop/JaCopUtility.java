package adl_2daa.jacop;

import java.util.Random;

import org.jacop.constraints.Alldiff;
import org.jacop.constraints.Count;
import org.jacop.constraints.Sum;
import org.jacop.constraints.XeqC;
import org.jacop.core.Domain;
import org.jacop.core.IntVar;
import org.jacop.core.Store;
import org.jacop.search.DepthFirstSearch;
import org.jacop.search.IndomainRandom;
import org.jacop.search.RandomSelect;
import org.jacop.search.Search;
import org.jacop.search.SolutionListener;


public class JaCopUtility {

	private static int[][] cachedSolutions;
	
	/**
	 * @see JaCopUtility#solveAllSolutionCSP(CSPTemplate)
	 */
	public static int solveAllSolutionCSP(Store store, IntVar[] vars){
		return solveAllSolutionCSP(new CSPTemplate() {
			@Override
			public CSPInstance newInstance() {
				return new CSPInstance(store, vars, null);
			}
		});
	}
	
	/**
	 * Solve the given CSP problem and caches all solution. Minimum cost is also returned
	 * or 0 if no cost function provided
	 */
	public static int solveAllSolutionCSP(CSPTemplate problemTemplate){
		CSPInstance problem = problemTemplate.newInstance();
		
		Search<IntVar> search = new DepthFirstSearch<IntVar>();
		search.setPrintInfo(false);
		SolutionListener<IntVar> solutions = search.getSolutionListener();
		RandomSelect<IntVar> select = new RandomSelect<IntVar>(problem.getVars(), new IndomainRandom<IntVar>());
		solutions.searchAll(true);
		solutions.recordSolutions(true);
		int minimumCost = 0;
		if(problem.getCostVar() != null){
			search.labeling(problem.getStore(), select, problem.getCostVar());
			minimumCost = problem.getCostVar().value();
			problem = problemTemplate.newInstance();
			problem.getStore().impose(new XeqC(problem.getCostVar(), minimumCost));
			
			//Recreate search strategy
			search = new DepthFirstSearch<IntVar>();
			search.setPrintInfo(false);
			solutions = search.getSolutionListener();
			select = new RandomSelect<IntVar>(problem.getVars(), new IndomainRandom<IntVar>());
			solutions.searchAll(true);
			solutions.recordSolutions(true);
		}
		search.labeling(problem.getStore(), select);
		
		System.out.println("Solution size : "+solutions.solutionsNo());
		cachedSolutions = new int[solutions.solutionsNo()][problem.getVars().length];
		Domain[] solution;
		Domain dom;
        for(int i=1; i<=solutions.solutionsNo(); i++){
        	solution = solutions.getSolution(i);
        	if(solution.length != problem.getVars().length){
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
        
        return minimumCost;
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
	 * Prepare all variables that will be used in PartialAllDiff. Must be called
	 * before imposing PartialAllDiff constraint ONCE AND ONLY ONCE. Return a value
	 * that a value of any variable specified as "no assignment" must not exceed. 
	 */
	public static int prepareVarsForPartialAllDiff(IntVar[] allVars){
		int universalLowerBound = Integer.MAX_VALUE;
		for(IntVar var : allVars){
			if(universalLowerBound > var.min()){
				universalLowerBound = var.min();
			}
		}
		universalLowerBound -= 1;
		assert(Integer.MIN_VALUE+allVars.length <= universalLowerBound);
		for(int i=0; i<allVars.length; i++){
			allVars[i].addDom(universalLowerBound-i, universalLowerBound-i);
		}
		
		return universalLowerBound+1;
	}

	/**
	 * Impose PartialAllDiff. Domain min of each variable represent "not selected"
	 */
	public static IntVar imposePartialAllDiff(Store store, IntVar[] vars){
		int[] unused = new int[vars.length];
		for(int i=0; i<vars.length; i++){
			unused[i] = vars[i].min();
		}
		store.impose(new Alldiff(vars));
		IntVar[] counter = new IntVar[vars.length];
		for(int i=0; i<vars.length; i++){
			counter[i] = new IntVar(store, 0, 1);
			store.impose(new Count(new IntVar[]{vars[i]}, counter[i], unused[i]));
		}
		IntVar cost = new IntVar(store, 0, vars.length);
		store.impose(new Sum(counter, cost));

		return cost;
	}
}
