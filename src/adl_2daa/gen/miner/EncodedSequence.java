package adl_2daa.gen.miner;

import java.util.ArrayList;
import java.util.List;

public class EncodedSequence {

	protected String identifier;
	protected List<EncodedAction> eActList;
	/**
	 * All possible flows to transition retrieved from flow analyzing during encoding process
	 */
	protected List<EncodedSequence> allFlowToTransition = null;
	protected List<String> allSpawnableAgent = null;
	
	protected EncodedSequence(String identifier, List<EncodedAction> sequence){
		this.identifier = identifier;
		this.eActList = sequence;
	}
	
	protected List<List<String>> toSequence(){
		List<List<String>> sequence = new ArrayList<List<String>>();
		List<String> itemset;
		for(EncodedAction act : eActList){
			itemset = new ArrayList<String>();
			itemset.add(act.toItem());
			sequence.add(itemset);
		}
		return sequence;
	}
}
