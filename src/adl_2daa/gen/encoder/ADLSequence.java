package adl_2daa.gen.encoder;

import java.util.ArrayList;
import java.util.List;

public class ADLSequence {
	
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
	
	public List<String> getEncodedSequence(){
		return encodedSequence;
	}
}
