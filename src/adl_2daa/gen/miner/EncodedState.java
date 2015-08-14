package adl_2daa.gen.miner;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;

public class EncodedState {

	protected String identifier;
	protected List<EncodedSequence> sequences;
	
	protected EncodedState(State astState, boolean analyzeFlow){
		identifier = astState.getIdentifier();
		sequences = new ArrayList<EncodedSequence>();
		for(Sequence seq : astState.getSequences()){
			sequences.add(ADLSequenceEncoder.instance.parseAsEncodedSequence(seq, analyzeFlow));
		}
	}
	
	protected List<String> getAllSpawnableEntity(){
		List<String> list = new LinkedList<String>();
		for(EncodedSequence eSeq : sequences){
			list.addAll(eSeq.allSpawnableAgent);
		}
		return list;
	}
}
