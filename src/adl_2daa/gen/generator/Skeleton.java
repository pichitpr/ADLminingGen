package adl_2daa.gen.generator;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import parsemis.extension.GraphPattern;
import spmf.extension.algorithm.seqgen.SequentialPatternGen;
import spmf.extension.patterns.itemset_list_generic.ItemsetGen;
import spmf.extension.prefixspan.JSPatternGen;
import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;
import adl_2daa.gen.encoder.ADLSequenceDecoder;
import adl_2daa.gen.filter.ASTFilter;
import adl_2daa.gen.filter.ASTFilterOperator;
import adl_2daa.gen.filter.ResultAgent;
import adl_2daa.gen.filter.ResultState;
import adl_2daa.gen.profile.AgentProfile;

public class Skeleton {

	private String identifier;
	private Root skel;
	
	/**
	 * Generate initial skeleton from profiles, there must be at least 1 profile.
	 * All blocks in the profile (except sequence block) must be non-empty
	 */
	public void generateInitialSkeleton(AgentProfile[] profiles){
		List<Agent> agents = new LinkedList<Agent>();
		identifier = profiles[0].getRootName();
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
	
	public void mergeOrder(SequentialPatternGen<String> relation){
		List<String> eSeq = new LinkedList<String>();
		for(ItemsetGen<String> iset : relation.getItemsets()){
			eSeq.add(iset.get(0));
		}
		List<ASTStatement> decodedRelation = ADLSequenceDecoder.instance.decode(eSeq);
		
		ASTSequenceSelection selection;
		Action eobTransition = ASTUtility.removeAllEOBTransition(decodedRelation);
		if(eobTransition != null){
			List<ResultAgent> validAgents = ASTFilterOperator.filterAgent(skel.getRelatedAgents(), 
					(agent, filteredState) -> !filteredState.isEmpty(), 
					(state, filteredSequence) -> !filteredSequence.isEmpty(), 
					new ASTFilter.EOStransitionSlotFilter(eobTransition, true));
			if(!validAgents.isEmpty()){
				ResultAgent agent = ASTUtility.randomUniform(validAgents);
				ResultState state = ASTUtility.randomUniform(agent.getResultStates());
				Sequence sequence = ASTUtility.randomUniform(state.getResultSequences());
				selection = new ASTSequenceSelection(agent.getActualAgent(), 
						state.getActualState(), sequence);
			}else{
				Agent agent = ASTUtility.randomUniformAgentOrCreate(skel);
				State state = ASTUtility.randomUniformStateOrCreate(agent);
				Sequence sequence = ASTUtility.createEmptySequence("seq"+state.getSequences().size());
				state.getSequences().add(sequence);
				selection = new ASTSequenceSelection(agent, state, sequence);
			}
			ASTMergeOperator.mergeEOBTransition(selection, eobTransition, true, true);
		}else{
			Agent agent = ASTUtility.randomUniformAgentOrCreate(skel);
			State state = ASTUtility.randomUniformStateOrCreate(agent);
			Sequence sequence = ASTUtility.randomUniformSequenceOrCreate(state);
			selection = new ASTSequenceSelection(agent, state, sequence);
		}
		ASTMergeOperator.merge(selection, decodedRelation);
	}
	
	public void mergeInterState(boolean des, JSPatternGen<String> relation, boolean useTag){
	}
	
	public void mergeParallel(GraphPattern<String,Integer> relation){
		
	}
	
	public void mergeInterEntity(GraphPattern<String,Integer> relation, boolean useTag){
		
	}
	
	public void saveAsScript(File dir) throws Exception{
		StringBuilder strb = new StringBuilder();
		skel.toScript(strb, 0);
		FileUtils.writeStringToFile(new File(dir, identifier+".txt"), strb.toString());
	}
}
