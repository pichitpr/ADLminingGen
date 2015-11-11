package adl_2daa.gen.generator;
import java.util.Random;

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
			
	public static int[] randomUniformAssignment(Store store, IntVar[] var){
		Search<IntVar> search = new DepthFirstSearch<IntVar>();
		search.setPrintInfo(false);
		SolutionListener<IntVar> solutions = search.getSolutionListener();
		RandomSelect<IntVar> select = new RandomSelect<IntVar>(var, new IndomainRandom<IntVar>());
		solutions.searchAll(true);
		solutions.recordSolutions(true);
		search.labeling(store, select);
		
		cachedSolutions = new int[solutions.solutionsNo()][var.length];
		Domain[] solution;
		Domain dom;
		System.out.println("Solution size : "+solutions.solutionsNo());
        for(int i=1; i<=solutions.solutionsNo(); i++){
        	solution = solutions.getSolution(i);
        	if(solution.length != var.length){
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
	
	public static int[] randomUniformAssignment(){
		int randomIndex = (new Random()).nextInt(cachedSolutions.length);
        return cachedSolutions[randomIndex];
	}
	
	public static int[][] allPrecalculatedAssignments(){
		return cachedSolutions;
	}
}
