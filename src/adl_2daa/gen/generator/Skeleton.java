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
		/*
		Pick 1 agent -> state -> sequence
		Generate [slot] from sequence as domain
		Generate [constraint] from relation
			- Forced ordering
			- Must not appear after Goto/Despawn (prevent dead code)
		 */
		
		//Prepare skeleton
		Agent agent = ASTUtility.randomUniformAgent(skel);
		State state = ASTUtility.randomUniformState(agent);
		Sequence sequence = ASTUtility.randomUniformSequence(state);
		List<ASTStatement> skelStatements = sequence.getStatements();
		
		
		//Decode relation
		List<String> eSeq = new LinkedList<String>();
		for(ItemsetGen<String> iset : relation.getItemsets()){
			eSeq.add(iset.get(0));
		}
		List<ASTStatement> statements = ADLSequenceDecoder.instance.decode(eSeq);
		
		
		//Generate CSP and solve
		int startEOSindex = skelStatements.size();
		if(skelStatements.size() > 0){
			ASTStatement lastStatement = skelStatements.get(startEOSindex-1);
			if(lastStatement instanceof Action){
				String name = ((Action)lastStatement).getName();
				if(name.equals("Goto") || name.equals("Despawn")){
					startEOSindex--;
				}
			}
		}
		Store store = new Store();
		IntVar[] var = new IntVar[statements.size()];
		for(int i=0; i<var.length; i++){
			var[i] = new IntVar(store, i, startEOSindex+i);
			if(i > 0){
				store.impose(new XltY(var[i-1], var[i]));
			}
		}
        int[] result = JaCopUtility.randomUniformAssignment(store, var);
        
        //Assign CSP result to AST
        int i=0;
        for(ASTStatement statement : statements){
        	skelStatements.add(result[i], statement);
        	i++;
        }
	}
	
	//Inter-state order
	public void merge(boolean des, JSPatternGen<String> relation, boolean useTag){
		/*Random agent -- If agent slot not sufficient: grow
		Pick 2 state -> 1 sequence from each
		Generate [slot] from sequence 1 as domain 1
		Generate [slot] from sequence 2 as domain 2
		Generate [constraint] from relation
			- Goto/Despawn must be at the end of any block (regardless of nesting structure)
			- Ordering preserved
		*/
		
		//Prepare skeleton
		int requiredStateCount = des ? 1 : 2;
		if(useTag && relation.getTag() > requiredStateCount)
			requiredStateCount = relation.getTag();
		Agent agent = ASTUtility.randomAgentAndGen(skel, requiredStateCount);
		List<ASTStatement> starting= null, target = null;
		String targetStateIdentifier = null;
		if(des){
			starting = ASTUtility.randomUniformSequence(
					ASTUtility.randomUniformState(agent)
					).getStatements();
			target = agent.getDes().getStatements();
		}else{
			assert(agent.getStates().size() >= 2);
			State startingState = ASTUtility.randomUniformState(agent);
			State targetState = ASTUtility.randomUniformState(agent);
			while(startingState == targetState){
				targetState = ASTUtility.randomUniformState(agent);
			}
			targetStateIdentifier = targetState.getIdentifier();
			starting = ASTUtility.randomUniformSequence(startingState).getStatements();
			target = ASTUtility.randomUniformSequence(targetState).getStatements();
		}
		
		
		//Decode relation
		List<String> startESeq = new LinkedList<String>();
		for(ItemsetGen<String> iset : relation.getLeftSide().getItemsets()){
			startESeq.add(iset.get(0));
		}
		List<ASTStatement> startStatements = ADLSequenceDecoder.instance.decode(startESeq);
		List<ASTExpression> params = new ArrayList<ASTExpression>();
		if(des){
			startStatements.add(new Action("Despawn", params));
		}else{
			params.add(new Identifier("."+targetStateIdentifier));
			startStatements.add(new Action("Goto", params));
		}
		
		List<String> targetESeq = new LinkedList<String>(); //Target can be empty
		String eAct;
		for(ItemsetGen<String> iset : relation.getRightSide().getItemsets()){
			eAct = iset.get(0).trim();
			if(!eAct.isEmpty()) targetESeq.add(eAct);
		}
		List<ASTStatement> targetStatements = ADLSequenceDecoder.instance.decode(targetESeq);
		
		
		// ============================ Target
		//Generate CSP and solve
		Store store;
		IntVar[] var;
		int[] result;
		int varIndex;
		if(targetStatements.size() > 0){
			store = new Store();
			var = new IntVar[targetStatements.size()];
			for(int i=0; i<var.length; i++){
				var[i] = new IntVar(store, i, target.size()+i);
				if(i > 0){
					store.impose(new XltY(var[i-1], var[i]));
				}
			}
			result = JaCopUtility.randomUniformAssignment(store, var);
	        //Assign
			varIndex = 0;
	        for(ASTStatement statement : targetStatements){
	        	target.add(result[varIndex], statement);
	        	varIndex++;
	        }
		}
        
        //============================ Starting
        //Generate CSP and solve
        ASTSlotManager slotManager = new ASTSlotManager();
        slotManager.label(starting);
        List<Integer> endOfASTBlock = slotManager.getValidSlots( 
        	node -> {
        		if(node.isEndOfStatement()){
        			if(node.getNodeContainer().size() == 0) return true;
        			ASTStatement lastStatement = node.getNodeContainer().get(
        					node.getNodeContainer().size()-1
        					);
        			if(lastStatement instanceof Action){
        				String name = ((Action)lastStatement).getName();
        				if(name.equals("Goto") || name.equals("Despawn")){
        					return false;
        				}else{
        					return true;
        				}
        			}else{
        				return true;
        			}
        		}else{
        			return false;
        		}
        	});
        List<Integer> rootStatements = slotManager.getValidSlots(
        	node -> {
        		return node.getParent() == null;
			});
        store = new Store();
		var = new IntVar[startStatements.size()];
		var[var.length-1] = new IntVar(store);
		for(Integer index : endOfASTBlock){
			var[var.length-1].addDom(index, index);
		}
		for(int i=0; i<var.length-1; i++){
			var[i] = new IntVar(store);
			for(Integer index : rootStatements){
				var[i].addDom(index, index);
			}
			if(i > 0){
				store.impose(new XlteqY(var[i-1], var[i]));
			}
			store.impose(new XlteqY(var[i], var[var.length-1]));
		}
		result = JaCopUtility.randomUniformAssignment(store, var);
		//Assign
		for(varIndex=0; varIndex<result.length; varIndex++){
			slotManager.insert(result[varIndex], startStatements.get(varIndex));
		}
		slotManager.finalize();
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
