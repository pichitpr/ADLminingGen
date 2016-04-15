package adl_2daa.gen.generator;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import parsemis.extension.GraphPattern;
import spmf.extension.algorithm.seqgen.SequentialPatternGen;
import spmf.extension.prefixspan.JSPatternGen;
import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;
import adl_2daa.gen.profile.AgentProfile;
import adl_2daa.gen.profile.ReachProfile;
import adl_2daa.gen.signature.GeneratorRegistry;

public class Skeleton {

	private String identifier;
	private Root skel;
	
	/**
	 * Generate initial skeleton from profiles, there must be at least 1 profile.
	 * All blocks in the profile (except sequence block) must be non-empty.
	 * The first agent in generated skeleton is considered "Main agent".
	 */
	public void generateInitialSkeleton(AgentProfile[] profiles){
		//TODO: Check main agent flag before generating
		List<Agent> agents = new LinkedList<Agent>();
		identifier = profiles[0].getRootName();
		skel = new Root(agents);
		
		for(int i=0; i<profiles.length; i++){
			List<ASTStatement> init = new LinkedList<ASTStatement>();
			List<ASTStatement> des = new LinkedList<ASTStatement>();
			List<State> states = new LinkedList<State>();
			Agent agent = new Agent("agent"+i, new Sequence("init", init), 
					new Sequence("des", des), states);
			
			assert(profiles[i].getStructureInfo().length > 0);
			for(int j=0; j<profiles[i].getStructureInfo().length; j++){
				List<Sequence> sequences = new LinkedList<Sequence>();
				State state = new State("state"+j, sequences);
				int seqCount = profiles[i].getStructureInfo()[j];
			
				assert(seqCount > 0);
				for(int k=0; k<seqCount; k++){
					sequences.add(new Sequence("seq"+k, new LinkedList<ASTStatement>()));
				}
				
				states.add(state);
			}
			
			agents.add(agent);
		}
	}
	
	public void mergeOrder(SequentialPatternGen<String> relation){
		OrderMerger.instance.merge(skel, relation);
	}
	
	public void mergeInterState(boolean des, JSPatternGen<String> relation, boolean useTag){
		InterStateOrderMerger.instance.merge(skel, des, relation, useTag);
	}
	
	public void mergeParallel(GraphPattern<String,Integer> relation){
		ParallelMerger.instance.merge(skel, relation);
	}
	
	public void mergeInterEntity(GraphPattern<String,Integer> relation, boolean useTag){
		InterEntityParallelMerger.instance.merge(skel, relation);
	}
	
	public void mergeNesting(List<GraphPattern<Integer,Integer>> relation){
		NestingMerger.instance.merge(skel, relation);
	}
	
	public void finalizeSkeleton(){
		//Trim block
		for(int i=skel.getRelatedAgents().size()-1; i>=0; i--){
			Agent agent = skel.getRelatedAgents().get(i);
			List<ASTStatement> seq;
			if(agent.getDes() != null){
				String dummyActionName = GeneratorRegistry.getActionName(
						GeneratorRegistry.getDummyActionSignature().getMainSignature().getId()
						);
				seq = agent.getDes().getStatements();
				for(int j=seq.size()-1; j>=0; j--){
					if(seq.get(j) instanceof Action){
						String actionName = ((Action)seq.get(j)).getName();
						//NOTE:: temporary solution to issue #1 written on Github
						if(actionName.equals("Goto") || actionName.equals("Despawn") || actionName.equals(dummyActionName))
							seq.remove(j);
					}
				}
			}
			for(int j=agent.getStates().size()-1; j>=0; j--){
				State state = agent.getStates().get(j);
				for(int k=state.getSequences().size()-1; k>=0; k--){
					if(state.getSequences().get(k).getStatements().size() == 0){
						state.getSequences().remove(k);
					}
				}
				if(state.getSequences().size() == 0){
					agent.getStates().remove(j);
				}
			}
		}
		
		//Analyze reach and modify key action target.
		IdentifierFiller.instance.fillMissingIdentifier(skel);
		
		//System.out.println( (new ReachProfile(skel)).profileToString(skel) );
	}
	
	public void saveAsScript(File dir) throws Exception{
		StringBuilder strb = new StringBuilder();
		skel.toScript(strb, 0);
		FileUtils.writeStringToFile(new File(dir, identifier+".txt"), strb.toString());
	}
}
