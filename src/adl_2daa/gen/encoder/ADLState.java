package adl_2daa.gen.encoder;

import java.util.LinkedList;
import java.util.List;

import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;

public class ADLState {

	protected String identifier;
	protected List<ADLSequence> sequences;
	
	public ADLState(State astState){
		this.identifier = astState.getIdentifier();
		this.sequences = new LinkedList<ADLSequence>();
		for(Sequence seq : astState.getSequences()){
			this.sequences.add(ADLSequenceEncoder.instance.encode(seq));
		}
	}

	protected List<String> getAllSpawnableEntity(){
		List<String> list = new LinkedList<String>();
		for(ADLSequence eSeq : sequences){
			list.addAll(eSeq.allSpawnableAgent);
		}
		return list;
	}
}
