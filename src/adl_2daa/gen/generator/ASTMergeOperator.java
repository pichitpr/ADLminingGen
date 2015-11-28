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

public class ASTMergeOperator {

	/**
	 * Merge EOB-transition into selected sequence, provided selected has available slot
	 */
	//TODO: finish this - handle eob transition
	public static void mergeEOBTransition(ASTSequenceSelection selection, 
			ASTStatement eobTransition, boolean mergeAsEOS, boolean useMatching){
		
	}
	
	//TODO: finish this - skel growing for key action ? parameter case (should perform unreachable state checking)
	public static void merge(ASTSequenceSelection selection, List<ASTStatement> relation){
		ASTSequenceWrapper wrappedRel = new ASTSequenceWrapper(relation);
		assert(relation.size() == wrappedRel.getIndicesCount()-1);
		ASTSequenceWrapper wrappedSkel = new ASTSequenceWrapper(selection.sequence.getStatements());
		merge(wrappedSkel, wrappedRel);
	}
	
	/**
	 * Merge the given relation sequence into a sequence selected from skeleton.
	 * The provided relation must NOT contain eob-transition. wrappedSkel will be
	 * finalized.
	 */
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
			if(relAction.getName().equals("Despawn")) continue; //Do nothing for Despawn
			//Matched actions' identifier has 3 cases: ? VS ?, IDEN_A vs IDEN_A and IDEN_? vs ?
			//We just have to take care of IDEN_? vs ? case
			if(relAction.getParams()[0] instanceof Identifier){
				skelAction.getParams()[0] = new Identifier("."+
						((Identifier)relAction.getParams()[0]).getValue()
						);
			}
		}

		List<ASTStatement> relation = wrappedRel.getWrappedAST();
		
		//Generate CSP
		Store store = new Store();
		IntVar[] vars = new IntVar[relation.size()];

		//Setup domain for allocated actions first -- used in constraint only
		for(int i=0; i<relAllocation.size(); i++){
			LCSSequenceEmbedding<ASTNode>.EmbeddingItem emb = relAllocation.itemAt(i);
			int relIndex = wrappedRel.embIndexToIndex(emb.getI());
			int skelIndex = wrappedSkel.embIndexToIndex(emb.getJ());
			skelIndex = skelIndex*2+1;
			vars[relIndex] = new IntVar(store, skelIndex, skelIndex);
		}

		//Setup domain for the remaining actions
		for(int i=0; i<relation.size(); i++){
			if(vars[i] != null) continue; //Skip allocated action
			vars[i] = new IntVar(store);
			for(int dom=0; dom<wrappedSkel.getIndicesCount(); dom++){
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
}
