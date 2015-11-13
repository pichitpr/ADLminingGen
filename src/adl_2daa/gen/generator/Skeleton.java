package adl_2daa.gen.generator;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jacop.constraints.XltY;
import org.jacop.constraints.XlteqY;
import org.jacop.core.IntVar;
import org.jacop.core.Store;
import org.jacop.search.DepthFirstSearch;
import org.jacop.search.IndomainMin;
import org.jacop.search.RandomSelect;
import org.jacop.search.Search;

import spmf.extension.algorithm.seqgen.SequentialPatternGen;
import spmf.extension.patterns.itemset_list_generic.ItemsetGen;
import spmf.extension.prefixspan.JSPatternGen;
import adl_2daa.ast.ASTExpression;
import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.expression.Identifier;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;
import adl_2daa.gen.encoder.ADLSequenceDecoder;
import adl_2daa.gen.profile.AgentProfile;

public class Skeleton {

	private String identifier;
	private Root skel;
	
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
	
	//Order
	public void merge(SequentialPatternGen<String> relation){
		SequenceOrderMerger.instance.merge(skel, relation);
	}
	
	//Inter-state order
	public void merge(boolean des, JSPatternGen<String> relation, boolean useTag){
		InterStateOrderMerger.instance.merge(skel, des, relation, useTag);
		/*

		//Prepare skeleton
		int requiredStateCount = des ? 1 : 2;
		if(useTag && relation.getTag() > requiredStateCount)
			requiredStateCount = relation.getTag();
		Agent agent = ASTUtility.randomAgentAndGen(skel, requiredStateCount);
		List<ASTStatement> startingSkelSequenceSt= null, targetSkelSequenceSt = null;
		String targetStateIdentifier = null;
		if(des){
			startingSkelSequenceSt = ASTUtility.randomUniformSequence(
					ASTUtility.randomUniformState(agent)
					).getStatements();
			targetSkelSequenceSt = agent.getDes().getStatements();
		}else{
			assert(agent.getStates().size() >= 2);
			State startingState = ASTUtility.randomUniformState(agent);
			State targetState = ASTUtility.randomUniformState(agent);
			while(startingState == targetState){
				targetState = ASTUtility.randomUniformState(agent);
			}
			targetStateIdentifier = targetState.getIdentifier();
			startingSkelSequenceSt = ASTUtility.randomUniformSequence(startingState).getStatements();
			targetSkelSequenceSt = ASTUtility.randomUniformSequence(targetState).getStatements();
		}
		
		
		//Decode relation
		List<String> startESeq = new LinkedList<String>();
		for(ItemsetGen<String> iset : relation.getLeftSide().getItemsets()){
			startESeq.add(iset.get(0));
		}
		List<ASTStatement> startingDecodedRel = ADLSequenceDecoder.instance.decode(startESeq);
		List<ASTExpression> params = new ArrayList<ASTExpression>();
		if(des){
			startingDecodedRel.add(new Action("Despawn", params));
		}else{
			params.add(new Identifier("."+targetStateIdentifier));
			startingDecodedRel.add(new Action("Goto", params));
		}
		
		List<String> targetESeq = new LinkedList<String>(); //Target can be empty
		String eAct;
		for(ItemsetGen<String> iset : relation.getRightSide().getItemsets()){
			eAct = iset.get(0).trim();
			if(!eAct.isEmpty()) targetESeq.add(eAct);
		}
		List<ASTStatement> targetDecodedRel = ADLSequenceDecoder.instance.decode(targetESeq);
		
		
		// ============================ Target
		//Generate CSP and solve
		Store store;
		IntVar[] var;
		int[] result;
		int varIndex;
		ASTSlotManager slotManager = new ASTSlotManager();
		if(targetDecodedRel.size() > 0){
			slotManager.label(targetSkelSequenceSt);
			List<Integer> rootSlots = slotManager.getValidSlots(
					node -> {
						if(node.getParent() != null) return false;
						if(node.isEndOfStatement()){
							return !ASTUtility.isStatementListEndWithTransition(node.getNodeContainer());
						}else{
							return true;
						}
					});
			assert(rootSlots.size() > 0);
			store = new Store();
			var = new IntVar[targetDecodedRel.size()];
			for(int i=0; i<var.length; i++){
				var[i] = new IntVar(store);
				for(Integer slot : rootSlots){
					var[i].addDom(slot, slot);
				}
				if(i > 0){
					store.impose(new XlteqY(var[i-1], var[i]));
				}
			}
			result = JaCopUtility.randomUniformAssignment(store, var);
	        //Assign
			varIndex = 0;
			for(varIndex=0; varIndex<result.length; varIndex++){
				slotManager.insert(result[varIndex], targetDecodedRel.get(varIndex));
			}
			slotManager.finalize();
		}
        
        //============================ Starting
        //Generate CSP and solve
        slotManager.label(startingSkelSequenceSt);
        List<Integer> eobSlots = slotManager.getValidSlots( 
        	node -> {
        		if(node.isEndOfStatement()){
        			return !ASTUtility.isStatementListEndWithTransition(node.getNodeContainer());
        		}else{
        			return false;
        		}
        	});
        assert(eobSlots.size() > 0);
        List<Integer> rootSlots = slotManager.getValidSlots(
        	node -> {
        		return node.getParent() == null;
			});
        assert(rootSlots.size() > 0);
        store = new Store();
		var = new IntVar[startingDecodedRel.size()];
		var[var.length-1] = new IntVar(store);
		for(Integer slot : eobSlots){
			var[var.length-1].addDom(slot, slot);
		}
		for(int i=0; i<var.length-1; i++){
			var[i] = new IntVar(store);
			for(Integer slot : rootSlots){
				var[i].addDom(slot, slot);
			}
			if(i > 0){
				store.impose(new XlteqY(var[i-1], var[i]));
			}
			store.impose(new XlteqY(var[i], var[var.length-1]));
		}
		result = JaCopUtility.randomUniformAssignment(store, var);
		//Assign
		for(varIndex=0; varIndex<result.length; varIndex++){
			slotManager.insert(result[varIndex], startingDecodedRel.get(varIndex));
		}
		slotManager.finalize();
		
		*/
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
	
	public void saveAsScript(File dir) throws Exception{
		StringBuilder strb = new StringBuilder();
		skel.toScript(strb, 0);
		FileUtils.writeStringToFile(new File(dir, identifier+".txt"), strb.toString());
	}
	
}
