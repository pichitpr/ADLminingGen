package adl_2daa.gen.encoder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ADLSequence {

	public static final List<String> dummySequence;
	static{
		dummySequence = new LinkedList<String>();
		dummySequence.add(ADLSequenceEncoder.impossibleAction);
	}
	
	protected String identifier;
	protected List<String> encodedSequence;
	protected List<ADLSequence> allFlowToTerminal;
	protected List<String> allSpawnableAgent;
	
	public ADLSequence(String identifier, List<String> encodedSequence){
		this.identifier = identifier;
		this.encodedSequence = encodedSequence;
	}
	
	public List<List<String>> toMinerSequence(){
		List<List<String>> sequence = new ArrayList<List<String>>();
		List<String> itemset;
		for(String action : encodedSequence){
			itemset = new ArrayList<String>();
			itemset.add(action);
			sequence.add(itemset);
		}
		return sequence;
	}
}
