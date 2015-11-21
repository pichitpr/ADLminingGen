package adl_2daa.gen.generator.merger2;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import spmf.extension.algorithm.seqgen.SequentialPatternGen;
import spmf.extension.patterns.itemset_list_generic.ItemsetGen;
import spmf.extension.prefixspan.JSPatternGen;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.State;
import adl_2daa.gen.encoder.ADLAgent;
import adl_2daa.gen.encoder.ADLRoot;
import adl_2daa.gen.encoder.ADLSequence;
import adl_2daa.gen.encoder.ADLState;
import adl_2daa.gen.generator.merger1.ASTUtility;
import adl_2daa.gen.profile.AgentProfile;

public class Skeleton {

	private String identifier;
	private ADLRoot skel;
	
	public void generateInitialSkeleton(AgentProfile[] profiles){
		List<ADLAgent> agents = new LinkedList<ADLAgent>();
		identifier = profiles[0].getRootName();
		skel = new ADLRoot(agents);
		
		for(int i=0; i<profiles.length; i++){
			List<String> init = new LinkedList<String>();
			List<String> des = new LinkedList<String>();
			List<ADLState> states = new LinkedList<ADLState>();
			ADLAgent agent = new ADLAgent("agent"+i, new ADLSequence("init", init), 
					new ADLSequence("des", des), states);
			
			for(int j=0; j<profiles[i].getStructureInfo().length; j++){
				List<ADLSequence> sequences = new LinkedList<ADLSequence>();
				ADLState state = new ADLState("state"+j, sequences);
				int seqCount = profiles[i].getStructureInfo()[j];
				
				for(int k=0; k<seqCount; k++){
					sequences.add(new ADLSequence("seq"+k, new LinkedList<String>()));
				}
				
				states.add(state);
			}
			
			agents.add(agent);
		}
	}
	
	//Order
	public void mergeOrder(SequentialPatternGen<String> relation){
		ADLAgent agent = ADLUtility.randomUniformAgent(skel);
		ADLState state = ADLUtility.randomUniformState(agent);
		ADLSequence skelSequence = ADLUtility.randomUniformSequence(state);
		
		List<String> eSeq = new LinkedList<String>();
		for(ItemsetGen<String> iset : relation.getItemsets()){
			eSeq.add(iset.get(0));
		}
		
		ADLUtility.merge(skelSequence, eSeq, null);
	}
	
	//Inter-state order
	public void mergeInterState(boolean des, JSPatternGen<String> relation, boolean useTag){
		
	}
	
	//Parallel
	public void mergeParallel(){

	}

	//Inter-entity parallel
	public void mergeInterEntity(){

	}

	public void fillFunction(){
	}

	public void completeSkeleton(){

	}

	public void saveAsScript(File dir) throws Exception{
		Root root = skel.toRoot();
		StringBuilder strb = new StringBuilder();
		root.toScript(strb, 0);
		FileUtils.writeStringToFile(new File(dir, identifier+".txt"), strb.toString());
	}
}
