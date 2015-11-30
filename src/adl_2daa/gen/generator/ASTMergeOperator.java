package adl_2daa.gen.generator;

import java.util.List;
import java.util.Set;

import lcs.LCSSequenceEmbedding;
import lcs.SimpleLCSEmbedding;

import org.jacop.constraints.XlteqY;
import org.jacop.core.IntVar;
import org.jacop.core.Store;

import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.expression.Identifier;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.structure.Root;
import adl_2daa.gen.filter.ASTNodeFilter;

public class ASTMergeOperator {

	/**
	 * Merge EOB-transition into selected sequence, provided the sequence has available slot.
	 * If statement matched, merge immediately. This method either uses EOS slot or EOB slot. 
	 * Return true if merging happens. Merge index or suggested insertion index (if no
	 * merging happens) is put in mergeIndex[0].   
	 */
	public static boolean matchAndMergeEOBTransition(ASTSequenceWrapper wrappedSkel, 
			Action eobTransition, boolean mergeAsEOS, int[] mergeIndex){
		assert(wrappedSkel.getSlotCount() > 0);
		if(wrappedSkel.getSlotCount() <= 1){
			//Empty sequence
			mergeIndex[0] = 0;
			return false;
		}
		
		List<Integer> eobSlots = wrappedSkel.getValidSlots(
				ASTNodeFilter.eobTransitionSlotFilter(eobTransition, mergeAsEOS, true)
				);
		assert(!eobSlots.isEmpty());
		int insertingIndex = ASTUtility.randomUniform(eobSlots);
		ASTNode insertPosition = wrappedSkel.getUnfoldNode(insertingIndex);
		if(insertPosition.getNodeContainer().isEmpty()){
			mergeIndex[0] = insertingIndex;
			return false;
		}else{
			ASTStatement lastStatement = insertPosition.getNodeContainer().get(insertPosition.getLocalIndex()-1);
			if(!tryMatchAndMerge(lastStatement, eobTransition, true)){
				//Cannot merge -- append
				mergeIndex[0] = insertingIndex;
				return false;
			}
			mergeIndex[0] = wrappedSkel.slotIndexOf(lastStatement);
			assert(mergeIndex[0] > -1);
			return true;
		}
	}
	
	/**
	 * Merge EOB-transition into selected sequence, provided the sequence has available slot.
	 * This method either uses EOS slot or EOB slot.
	 */
	/*
	public static void mergeEOBTransition(ASTSequenceSelection selection, 
			Action eobTransition, boolean mergeAsEOS){
		List<ASTStatement> stList = selection.sequence.getStatements();
		if(stList.isEmpty()){
			stList.add(eobTransition);
			return;
		}
		
		if(mergeAsEOS){
			ASTStatement lastStatement = stList.get(stList.size()-1);
			if(!tryMatchAndMerge(lastStatement, eobTransition, true)){
				stList.add(eobTransition);
				return;
			}
			return;
		}
		
		//EOB-T case: get a list of existing slot first, then pick one and merge (or match)
		ASTSequenceWrapper wrappedSequence = new ASTSequenceWrapper(stList);
		List<Integer> eobSlots = wrappedSequence.getValidSlots(
				ASTNodeFilter.eobTransitionFilter(eobTransition, true)
				);
		assert(!eobSlots.isEmpty());
		int insertingIndex = ASTUtility.randomUniform(eobSlots);
		ASTNode insertPosition = wrappedSequence.getUnfoldNode(insertingIndex);
		if(insertPosition.getNodeContainer().isEmpty()){
			insertPosition.getNodeContainer().add(eobTransition);
		}else{
			ASTStatement lastStatement = insertPosition.getNodeContainer().get(insertPosition.getLocalIndex()-1);
			if(!tryMatchAndMerge(lastStatement, eobTransition, true)){
				stList.add(eobTransition);
				return;
			}
		}
	}
	*/
	
	/**
	 * Try to match/merge given key action with the given statement from skeleton 
	 * ignoring nesting condition. If both action do not match, return false.
	 * Otherwise, merge them.
	 */
	public static boolean tryMatchAndMerge(ASTStatement skelStatement, Action keyAction,
			boolean testMatchFirst){
		if(testMatchFirst){
			if(!ASTUtility.isKeyAction(skelStatement, false)) return false;
			if(!ASTUtility.isKeyActionsMatched((Action)skelStatement, keyAction))
				return false;
		}
		Action skelAction = (Action)skelStatement;
		//Matched Despawn dont need extra process
		if(keyAction.getName().equals("Despawn")) return true;
		//Matched actions' identifier has 3 cases: ? VS ?, IDEN_A vs IDEN_A and IDEN_? vs ?
		//We just have to take care of IDEN_? vs ? case
		if(keyAction.getParams()[0] instanceof Identifier){
			skelAction.getParams()[0] = new Identifier("."+
					((Identifier)keyAction.getParams()[0]).getValue()
					);
		}
		return true;
	}
	
	/**
	 * @see ASTMergeOperator#merge(ASTSequenceWrapper, ASTSequenceWrapper)
	 */
	public static void merge(ASTSequenceSelection selection, List<ASTStatement> relation){
		ASTSequenceWrapper wrappedRel = new ASTSequenceWrapper(relation);
		ASTSequenceWrapper wrappedSkel = new ASTSequenceWrapper(selection.sequence.getStatements());
		merge(wrappedSkel, wrappedRel);
	}
	
	/**
	 * Merge the given relation sequence into a sequence selected from skeleton.
	 * The provided relation must NOT contain eob-transition. wrappedSkel will be
	 * finalized.
	 */
	//TODO: check for existing EOB-T before merge
	public static void merge(ASTSequenceWrapper wrappedSkel, ASTSequenceWrapper wrappedRel){
		//Allocate matched key actions
		Set<LCSSequenceEmbedding<ASTNode>> lcsResult = SimpleLCSEmbedding.allLCSEmbeddings(
				wrappedRel, wrappedSkel, ASTSequenceWrapper.keyActionComparator);
		LCSSequenceEmbedding<ASTNode> relAllocation = null;
		int lcsIndex = ASTUtility.randomRange(0, lcsResult.size()-1);
		for(LCSSequenceEmbedding<ASTNode> emb : lcsResult){
			if(lcsIndex == 0){
				relAllocation = emb;
				break;
			}
			lcsIndex--;
		}

		//Merge all matched key actions
		for(int i=0; i<relAllocation.size(); i++){
			LCSSequenceEmbedding<ASTNode>.EmbeddingItem emb = relAllocation.itemAt(i);
			Action relAction = (Action)wrappedRel.itemAt(emb.getI()).getNode();
			Action skelAction = (Action)wrappedSkel.itemAt(emb.getJ()).getNode();
			/*
			if(relAction.getName().equals("Despawn")) continue; //Do nothing for Despawn
			//Matched actions' identifier has 3 cases: ? VS ?, IDEN_A vs IDEN_A and IDEN_? vs ?
			//We just have to take care of IDEN_? vs ? case
			if(relAction.getParams()[0] instanceof Identifier){
				skelAction.getParams()[0] = new Identifier("."+
						((Identifier)relAction.getParams()[0]).getValue()
						);
			}
			*/
			tryMatchAndMerge(skelAction, relAction, false);
		}

		List<ASTStatement> relation = wrappedRel.getWrappedAST();
		assert(relation.size() == wrappedRel.getActionCount());
		
		//Generate CSP
		Store store = new Store();
		IntVar[] vars = new IntVar[relation.size()];

		//Setup domain for allocated actions first -- used in constraint only
		for(int i=0; i<relAllocation.size(); i++){
			LCSSequenceEmbedding<ASTNode>.EmbeddingItem emb = relAllocation.itemAt(i);
			int relIndex = wrappedRel.embIndexToActionIndex(emb.getI());
			int skelIndex = wrappedSkel.embIndexToSlotIndex(emb.getJ());
			skelIndex = skelIndex*2+1;
			vars[relIndex] = new IntVar(store, skelIndex, skelIndex);
		}

		//Setup domain for the remaining actions
		for(int i=0; i<relation.size(); i++){
			if(vars[i] != null) continue; //Skip allocated action
			vars[i] = new IntVar(store);
			for(int dom=0; dom<wrappedSkel.getSlotCount(); dom++){
				vars[i].addDom(dom*2, dom*2);
			}
		}

		//Ordering constraint
		for(int i=1; i<relation.size(); i++){
			store.impose(new XlteqY(vars[i-1], vars[i]));
		}

		//Solving and assign
		int[] assignment = JaCopUtility.randomUniformAssignment(store, vars);
		assert(assignment.length == relation.size());
		for(int i=0; i<assignment.length; i++){
			if(assignment[i] % 2 == 0){
				wrappedSkel.queueInsertion(assignment[i]/2, relation.get(i));
			}
		}

		wrappedSkel.finalizeWrapper();
	}
	
	/**
	 * Fill up incomplete key action (Goto/Spawn with no specified target), the method
	 * try to make every state/agent reachable (has incoming/outgoing transition).
	 * Growing is done if needed
	 */
	//TODO: finish this - skel growing for key action with ? parameter
	public static void fillIncompleteKeyAction(Root skel, ASTSequenceSelection selection){
		
	}
}
