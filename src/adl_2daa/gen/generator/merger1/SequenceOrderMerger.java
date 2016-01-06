package adl_2daa.gen.generator.merger1;

import java.util.LinkedList;
import java.util.List;

import org.jacop.constraints.XlteqY;
import org.jacop.core.IntVar;
import org.jacop.core.Store;

import spmf.extension.algorithm.seqgen.SequentialPatternGen;
import spmf.extension.patterns.itemset_list_generic.ItemsetGen;
import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;
import adl_2daa.gen.encoder.ADLSequenceDecoder;
import adl_2daa.jacop.JaCopUtility;


public class SequenceOrderMerger {

	public static final SequenceOrderMerger instance = new SequenceOrderMerger();
	
	public void merge(Root rootSkel, SequentialPatternGen<String> relation){
		select(rootSkel);
		decodeRelation(relation);
		merge();
	}
	
	/*
	Pick 1 agent -> state -> sequence
	Generate [slot] from sequence as domain
	Generate [constraint] from relation
		- Forced ordering
		- Must not appear after Goto/Despawn (prevent dead code)
	 */
	
	private Sequence skelSequence;
	private List<ASTStatement> decodedRelation;
	
	private void select(Root rootSkel){
		Agent agent = ASTUtility.randomUniformAgent(rootSkel);
		State state = ASTUtility.randomUniformState(agent);
		skelSequence = ASTUtility.randomUniformSequence(state);
	}
	
	private void decodeRelation(SequentialPatternGen<String> relation){
		List<String> eSeq = new LinkedList<String>();
		for(ItemsetGen<String> iset : relation.getItemsets()){
			eSeq.add(iset.get(0));
		}
		decodedRelation = ADLSequenceDecoder.instance.decode(eSeq);
	}
	
	private void merge(){
		ASTSlotManager slotManager = new ASTSlotManager();
		slotManager.label(skelSequence.getStatements());
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
		Store store = new Store();
		IntVar[] var = new IntVar[decodedRelation.size()];
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
        int[] result = JaCopUtility.randomUniformAssignment();
        
        for(int varIndex=0; varIndex<result.length; varIndex++){
			slotManager.insert(result[varIndex], decodedRelation.get(varIndex));
		}
		slotManager.finalizeManager();
	}
}
