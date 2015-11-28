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
import adl_2daa.gen.filter.ResultAgent;
import adl_2daa.gen.filter.ResultState;

public class OrderMerger {

	public static final OrderMerger instance = new OrderMerger();
	
	public void merge(Root skel, SequentialPatternGen<String> relation){
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
		ASTMergeOperator.fillIncompleteKeyAction(skel, selection);
	}
}
