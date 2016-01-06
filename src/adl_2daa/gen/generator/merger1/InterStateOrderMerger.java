package adl_2daa.gen.generator.merger1;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import org.jacop.constraints.XlteqY;
import org.jacop.core.IntVar;
import org.jacop.core.Store;

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
import adl_2daa.jacop.JaCopUtility;


public class InterStateOrderMerger {

	public static final InterStateOrderMerger instance = new InterStateOrderMerger();
	
	public void merge(Root rootSkel, boolean desType, 
			JSPatternGen<String> relation, boolean useTag){
		select(rootSkel, desType, useTag ? relation.getTag() : -1);
		decodeRelation(desType, relation);
		merge();
	}
	
	/*
	Get 2 random sequences where
		- Both sequences come from different state in the same agent
		- At least 1 of them must have at least 1 valid slot for transition 
	Generate [slot] from sequence 1 as domain 1
	Generate [slot] from sequence 2 as domain 2
	Generate [constraint] from relation
		- Transition must be at the end of any block (regardless of nesting structure)
		- Ordering preserved
	*/
	
	private Sequence startingSkelSequence, targetSkelSequence;
	private String targetStateName;
	private List<ASTStatement> startingDecodedRel, targetDecodedRel;
	
	private void select(Root rootSkel, boolean desType, int tag){
		int requiredStateCount = desType ? 1 : 2;
		if(tag > requiredStateCount)
			requiredStateCount = tag;
		
		Predicate<Sequence> sequenceFilter = new Predicate<Sequence>() {
			@Override
			public boolean test(Sequence t) {
				ASTSlotManager slotManager = new ASTSlotManager();
				slotManager.label(t.getStatements());
				List<Integer> eobSlots = slotManager.getValidSlots(
						node -> {
							if(node.isEndOfStatement()){
			        			return !ASTUtility.isStatementListEndWithTransition(node.getNodeContainer());
			        		}else{
			        			return false;
			        		}
						});
				return eobSlots.size() > 0;
			}
		};
		
		//Check for valid agent(s) -- complete selection if it has so
		final int fRequiredStateCount = requiredStateCount;
		List<Agent> validAgents = ASTUtility.filterAgent(rootSkel, 
				(agent,filteredState) -> agent.getStates().size() >= fRequiredStateCount,
				(state,filteredSequence) -> filteredSequence.size() > 0,
				sequenceFilter, false);
		if(validAgents.size() > 0){
			Agent agent = ASTUtility.randomUniform(validAgents);
			State startingState = ASTUtility.randomUniform(
					ASTUtility.filterState(agent, null, sequenceFilter, true)
					);
			startingSkelSequence = ASTUtility.randomUniform(startingState.getSequences());
			State targetState;
			do{
				targetState = ASTUtility.randomUniform(agent.getStates());
			}while(startingState.getIdentifier().equals(targetState.getIdentifier()));
			targetSkelSequence = ASTUtility.randomUniform(targetState.getSequences());
			targetStateName = targetState.getIdentifier();
			return;
		}
		
		//In case no valid agent!
		Agent agent = ASTUtility.randomAgentAndGenEmptyState(rootSkel, fRequiredStateCount);
		List<State> validStates = ASTUtility.filterState(agent, null, sequenceFilter, false);
		if(validStates.size() > 0){
			//Valid starting state
			State startingState = ASTUtility.randomUniform(validStates);
			List<Sequence> validSequences = ASTUtility.filterSequence(startingState, 
					sequenceFilter);
			startingSkelSequence = ASTUtility.randomUniform(validSequences);
			State targetState;
			do{
				targetState = ASTUtility.randomUniform(agent.getStates());
			}while(startingState.getIdentifier().equals(targetState.getIdentifier()));
			targetSkelSequence = ASTUtility.randomUniform(targetState.getSequences());
			targetStateName = targetState.getIdentifier();
		}else{
			//No valid starting state
			State startingState = ASTUtility.randomUniformState(agent);
			startingSkelSequence = ASTUtility.createEmptySequence(
					"seq"+startingState.getSequences().size());
			startingState.getSequences().add(startingSkelSequence);
			State targetState;
			do{
				targetState = ASTUtility.randomUniform(agent.getStates());
			}while(startingState.getIdentifier().equals(targetState.getIdentifier()));
			targetSkelSequence = ASTUtility.randomUniformSequence(targetState);
			targetStateName = targetState.getIdentifier();
		}
	}
	
	private void decodeRelation(boolean desType, JSPatternGen<String> relation){
		List<String> startESeq = new LinkedList<String>();
		for(ItemsetGen<String> iset : relation.getLeftSide().getItemsets()){
			startESeq.add(iset.get(0));
		}
		startingDecodedRel = ADLSequenceDecoder.instance.decode(startESeq);
		List<ASTExpression> params = new ArrayList<ASTExpression>();
		if(desType){
			startingDecodedRel.add(new Action("Despawn", params));
		}else{
			params.add(new Identifier("."+targetStateName));
			startingDecodedRel.add(new Action("Goto", params));
		}
		
		List<String> targetESeq = new LinkedList<String>(); //Target can be empty
		String eAct;
		for(ItemsetGen<String> iset : relation.getRightSide().getItemsets()){
			eAct = iset.get(0).trim();
			if(!eAct.isEmpty()) targetESeq.add(eAct);
		}
		targetDecodedRel = ADLSequenceDecoder.instance.decode(targetESeq);
	}
	
	private void merge(){
		Store store;
		IntVar[] var;
		int[] result;
		int varIndex;
		ASTSlotManager slotManager = new ASTSlotManager();
		
		//Target sequence
		if(targetDecodedRel.size() > 0){
			slotManager.label(targetSkelSequence.getStatements());
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
			JaCopUtility.solveAllSolutionCSP(store, var);
			result = JaCopUtility.randomUniformAssignment();
			for(varIndex=0; varIndex<result.length; varIndex++){
				slotManager.insert(result[varIndex], targetDecodedRel.get(varIndex));
			}
			slotManager.finalizeManager();
		}
		
		//Starting sequence
		slotManager.label(startingSkelSequence.getStatements());
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
		JaCopUtility.solveAllSolutionCSP(store, var);
		result = JaCopUtility.randomUniformAssignment();
		for(varIndex=0; varIndex<result.length; varIndex++){
			slotManager.insert(result[varIndex], startingDecodedRel.get(varIndex));
		}
		slotManager.finalizeManager();
	}
}
