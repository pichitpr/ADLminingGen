package adl_2daa.gen.encoder;

import java.util.LinkedList;
import java.util.List;

import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;

public class ADLState {

	protected String identifier;
	protected List<ADLSequence> encodedSequences;
	
	public ADLState(State astState){
		this.identifier = astState.getIdentifier();
		this.encodedSequences = new LinkedList<ADLSequence>();
		for(Sequence seq : astState.getSequences()){
			this.encodedSequences.add(ADLSequenceEncoder.instance.encode(seq));
		}
	}

	protected List<String> getAllSpawnableEntity(){
		List<String> list = new LinkedList<String>();
		for(ADLSequence eSeq : encodedSequences){
			list.addAll(eSeq.allSpawnableAgent);
		}
		return list;
	}
}
