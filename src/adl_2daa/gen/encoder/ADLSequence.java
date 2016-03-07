package adl_2daa.gen.encoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import adl_2daa.ast.structure.Sequence;

public class ADLSequence {
	
	protected String identifier;
	protected List<String> encodedSequence;
	protected List<ADLSequence> allFlowToTerminal;
	/**
	 * Spawner sequence version (@Spawn encoded) of this sequence for each spawned child agent
	 */
	protected HashMap<String, ADLSequence> allSpawnerSequence;
	
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
	
	//===============================
	
	public List<String> getEncodedSequence(){
		return encodedSequence;
	}
	
	public Sequence toSequence(){
		return new Sequence(identifier, 
				ADLSequenceDecoder.instance.decode(encodedSequence));
	}
}
