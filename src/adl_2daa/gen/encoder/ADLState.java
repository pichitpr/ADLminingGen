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

	//=========================================
	
	public ADLState(String identifier, List<ADLSequence> sequences) {
		super();
		this.identifier = identifier;
		this.sequences = sequences;
	}

	public String getIdentifier() {
		return identifier;
	}

	public List<ADLSequence> getSequences() {
		return sequences;
	}
	
	public State toState(){
		List<Sequence> seqs = new LinkedList<Sequence>();
		for(ADLSequence eSeq : sequences){
			seqs.add(eSeq.toSequence());
		}
		return new State(identifier, seqs);
	}
}
