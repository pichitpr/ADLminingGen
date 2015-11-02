package adl_2daa.gen.generator;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jacop.constraints.XltY;
import org.jacop.core.IntVar;
import org.jacop.core.Store;
import org.jacop.search.DepthFirstSearch;
import org.jacop.search.IndomainMin;
import org.jacop.search.RandomSelect;
import org.jacop.search.Search;

import spmf.extension.algorithm.seqgen.SequentialPatternGen;
import spmf.extension.patterns.itemset_list_generic.ItemsetGen;
import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;
import adl_2daa.gen.encoder.ADLSequenceDecoder;
import adl_2daa.gen.profile.AgentProfile;

public class Skeleton {

	private Root skel;
	
	public void generateInitialSkeleton(AgentProfile[] profiles){
		List<Agent> agents = new LinkedList<Agent>();
		skel = new Root(agents);
		
		for(int i=0; i<profiles.length; i++){
			List<ASTStatement> init = new LinkedList<ASTStatement>();
			List<ASTStatement> des = new LinkedList<ASTStatement>();
			List<State> states = new LinkedList<State>();
			Agent agent = new Agent("agent"+i, new Sequence("init", init), 
					new Sequence("des", des), states);
			
			for(int j=0; j<profiles[i].getStructureInfo().length; j++){
				List<Sequence> sequences = new LinkedList<Sequence>();
				State state = new State("state"+j, sequences);
				int seqCount = profiles[i].getStructureInfo()[j];
				
				for(int k=0; k<seqCount; k++){
					sequences.add(new Sequence("seq"+k, new LinkedList<ASTStatement>()));
				}
				
				states.add(state);
			}
			
			agents.add(agent);
		}
	}
	
	//Order
	public void merge(SequentialPatternGen<String> relation){
		//Pick 1 agent -> state -> sequence
		//Generate [slot] from sequence as domain
		//Generate [constraint] from relation
		//Solve
		//Realize [assignment] --> need to know "location in AST" for each slot value
		
		Agent agent = ASTUtility.randomUniformAgent(skel);
		State state = ASTUtility.randomUniformState(agent);
		Sequence sequence = ASTUtility.randomUniformSequence(state);
		List<ASTStatement> skelStatements = sequence.getStatements();
		
		List<String> eSeq = new LinkedList<String>();
		for(ItemsetGen<String> iset : relation.getItemsets()){
			eSeq.add(iset.get(0));
		}
		List<ASTStatement> statements = ADLSequenceDecoder.instance.decode(eSeq);
		
		//Generate CSP and solve
		Store store = new Store();
		IntVar[] var = new IntVar[statements.size()];
		for(int i=0; i<var.length; i++){
			var[i] = new IntVar(store, i, var.length+i);
			if(i > 0){
				store.impose(new XltY(var[i-1], var[i]));
			}
		}
		Search<IntVar> search = new DepthFirstSearch<IntVar>();
        RandomSelect<IntVar> select = 
            new RandomSelect<IntVar>(var, new IndomainMin<IntVar>());
        search.labeling(store, select); //No result check, impossible that no result found
        
        //Assign CSP result to AST
        int i=0;
        for(ASTStatement statement : statements){
        	skelStatements.add(var[i].value(), statement);
        	i++;
        }
	}
	
	//Inter-state order
	public void merge(Agent agent){
		//Random agent (agent with available states has more chance)
		//If agent slot not sufficient: grow
		//Pick 2 state -> 1 sequence from each
		//Generate [slot] from sequence 1 as domain 1
		//Generate [slot] from sequence 2 as domain 2
		//Generate [constraint] from relation
		//Solve
		//Realize [assignment]
	}
	
	//Parallel
	public void merge(State state){
		//Random agent -> state (chance)
		
	}
	
	//Inter-entity parallel
	public void merge(Root root){
		
	}
	
	public void fillFunction(){
	}
	
	public void completeSkeleton(){
		
	}
	
	/*
	public void saveAsScript(File dir) throws Exception{
		StringBuilder strb = new StringBuilder();
		skel.toScript(strb, 0);
		FileUtils.writeStringToFile(new File(dir,  "sample.txt"), strb.toString());
	}
	*/
}
