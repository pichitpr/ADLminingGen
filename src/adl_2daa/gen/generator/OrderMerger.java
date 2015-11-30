package adl_2daa.gen.generator;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import lcs.LCSSequenceEmbedding;
import lcs.SimpleLCSEmbedding;

import org.jacop.constraints.XltC;
import org.jacop.constraints.XlteqY;
import org.jacop.core.IntVar;
import org.jacop.core.Store;

import spmf.extension.algorithm.seqgen.SequentialPatternGen;
import spmf.extension.patterns.itemset_list_generic.ItemsetGen;
import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;
import adl_2daa.gen.encoder.ADLSequenceDecoder;
import adl_2daa.gen.filter.ASTFilter;
import adl_2daa.gen.filter.ASTFilterOperator;
import adl_2daa.gen.filter.ASTNodeFilter;
import adl_2daa.gen.filter.ResultAgent;
import adl_2daa.gen.filter.ResultState;
import adl_2daa.gen.filter.ASTFilter.EOBtransitionSlotFilter;
import adl_2daa.gen.testtool.TestUtility;

public class OrderMerger {

	public static final OrderMerger instance = new OrderMerger();
	
	private ASTSequenceSelection selection;
	private List<ASTStatement> decodedRelation;
	private Action transition;
	
	public void merge(Root skel, SequentialPatternGen<String> relation){
		decodeRelation(relation);
		select(skel);
		merge(skel);
		ASTMergeOperator.fillIncompleteKeyAction(skel, selection);
	}
	
	private void decodeRelation(SequentialPatternGen<String> relation){
		List<String> eSeq = new LinkedList<String>();
		for(ItemsetGen<String> iset : relation.getItemsets()){
			eSeq.add(iset.get(0));
		}
		decodedRelation = ADLSequenceDecoder.instance.decode(eSeq);
	}
	
	private void select(Root skel){
		transition = ASTUtility.removeAllEOBTransition(decodedRelation);
		if(transition != null){
			List<ResultAgent> validAgents = ASTFilterOperator.filterAgent(skel.getRelatedAgents(), 
					(agent, filteredState) -> !filteredState.isEmpty(), 
					(state, filteredSequence) -> !filteredSequence.isEmpty(), 
					new ASTFilter.EOBtransitionSlotFilter(transition, true, true));
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
			
		}else{
			Agent agent = ASTUtility.randomUniformAgentOrCreate(skel);
			State state = ASTUtility.randomUniformStateOrCreate(agent);
			Sequence sequence = ASTUtility.randomUniformSequenceOrCreate(state);
			selection = new ASTSequenceSelection(agent, state, sequence);
		}
		/*
		StringBuilder strb = new StringBuilder("\n"+selection.state.getIdentifier());
		selection.sequence.toScript(strb, 0);
		System.out.println(strb);
		*/
	}
	
	private void merge(Root skel){
		/*
		if(transition != null){
			ASTMergeOperator.mergeEOBTransition(selection, transition, true);
		}
		ASTMergeOperator.merge(selection, decodedRelation);
		*/
		
		ASTSequenceWrapper wrappedSkel = new ASTSequenceWrapper(selection.sequence.getStatements());
		ASTSequenceWrapper wrappedRel = new ASTSequenceWrapper(decodedRelation);
		
		Store store = new Store();
		IntVar[] vars = new IntVar[wrappedRel.getActionCount()];
		int[] transitionSlot = new int[]{Integer.MAX_VALUE};
		boolean requireTransitionInsertion = false;
		if(transition != null){
			if(!ASTMergeOperator.matchAndMergeEOBTransition(wrappedSkel, transition, true,
					transitionSlot)){
				requireTransitionInsertion = true;
			}
			transitionSlot[0] = transitionSlot[0]*2+1;
			//System.out.println("[T] "+transitionSlot[0]);
		}else{
			List<Integer> existingEOS = wrappedSkel.getValidSlots(
					ASTNodeFilter.existingTransition(true)
					);
			if(!existingEOS.isEmpty()){
				transitionSlot[0] = existingEOS.get(existingEOS.size()-1);
				transitionSlot[0] = transitionSlot[0]*2+1;
				//System.out.println("[EOS] "+transitionSlot[0]);
			}
		}
		
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
			//System.out.println("Matched "+relAction.getName()+".."+skelAction.getName());
			ASTMergeOperator.tryMatchAndMerge(skelAction, relAction, false);
		}
		
		//Setup domain for allocated actions first -- used in constraint only
		for(int i=0; i<relAllocation.size(); i++){
			LCSSequenceEmbedding<ASTNode>.EmbeddingItem emb = relAllocation.itemAt(i);
			int relIndex = wrappedRel.embIndexToActionIndex(emb.getI());
			int skelIndex = wrappedSkel.embIndexToSlotIndex(emb.getJ());
			skelIndex = skelIndex*2+1;
			vars[relIndex] = new IntVar(store, skelIndex, skelIndex);
		}
		
		//Setup domain for the remaining actions
		for(int i=0; i<wrappedRel.getActionCount(); i++){
			if(vars[i] != null){
				//System.out.print("[M] "+vars[i].dom().toStringFull()+" ");
				//TestUtility.printASTStatement(decodedRelation.get(i));
				continue; //Skip allocated action
			}
			vars[i] = new IntVar(store);
			for(int dom=0; dom<wrappedSkel.getSlotCount(); dom++){
				vars[i].addDom(dom*2, dom*2);
				//System.out.print(dom+" ");
			}
			//TestUtility.printASTStatement(decodedRelation.get(i));
		}

		//Ordering constraint
		store.impose(new XltC(vars[0], transitionSlot[0]));
		for(int i=1; i<wrappedRel.getActionCount(); i++){
			store.impose(new XlteqY(vars[i-1], vars[i]));
			store.impose(new XltC(vars[i], transitionSlot[0]));
		}

		//Solving and assign
		int[] assignment = JaCopUtility.randomUniformAssignment(store, vars);
		assert(assignment.length == wrappedRel.getActionCount());
		for(int i=0; i<assignment.length; i++){
			if(assignment[i] % 2 == 0){
				wrappedSkel.queueInsertion(assignment[i]/2, decodedRelation.get(i));
			}
		}
		if(requireTransitionInsertion){
			wrappedSkel.queueInsertion(transitionSlot[0]/2, transition);
		}

		wrappedSkel.finalizeWrapper();
	}
}
