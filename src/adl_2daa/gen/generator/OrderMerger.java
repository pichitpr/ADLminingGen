package adl_2daa.gen.generator;

import java.util.LinkedList;
import java.util.List;

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
	
	public void decodeAndDumpRelation(SequentialPatternGen<String> relation, StringBuilder strb){
		decodeRelation(relation);
		(new Sequence("seq", decodedRelation)).toScript(strb, 0);
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
		ASTSequenceWrapper wrappedSkel = new ASTSequenceWrapper(selection.sequence.getStatements());
		ASTSequenceWrapper wrappedRel = new ASTSequenceWrapper(decodedRelation);
		
		int[] transitionSlot = new int[]{-1};
		boolean requireTransitionInsertion = false;
		if(transition != null){
			if(!ASTMergeOperator.matchAndMergeEOBTransition(wrappedSkel, transition, true,
					transitionSlot)){
				requireTransitionInsertion = true;
			}
			//System.out.println("[T] "+transitionSlot[0]);
		}else{
			List<Integer> existingEOS = wrappedSkel.getValidSlots(
					ASTNodeFilter.existingTransition(true)
					);
			if(!existingEOS.isEmpty()){
				transitionSlot[0] = existingEOS.get(existingEOS.size()-1);
				//System.out.println("[EOS] "+transitionSlot[0]);
			}
		}
		if(wrappedRel.getActionCount() > 0){
			ASTMergeOperator.queueSequenceInsertion(wrappedSkel, wrappedRel, transitionSlot[0]);
		}
		if(requireTransitionInsertion){
			wrappedSkel.queueInsertion(transitionSlot[0], transition);
		}
		wrappedSkel.finalizeWrapper();
	}
}
