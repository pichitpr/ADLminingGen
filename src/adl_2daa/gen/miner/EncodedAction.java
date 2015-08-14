package adl_2daa.gen.miner;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EncodedAction {

	protected byte actionID;
	protected List<byte[]> nestingConditions;
	
	protected EncodedAction(byte actionID, List<byte[]> nestingConditions, int nestingLevel){
		this.actionID = actionID;
		this.nestingConditions = new ArrayList<byte[]>();
		for(int i=0; i<nestingLevel; i++){
			this.nestingConditions.add(nestingConditions.get(i).clone());
		}
	}
	
	protected int getDeepestSharedNestingLevel(EncodedAction other){
		int minNesting = nestingConditions.size() < other.nestingConditions.size() ?
				nestingConditions.size() : other.nestingConditions.size();
		int sharedLevelCount = 0;
		while(sharedLevelCount < minNesting){
			if(!Arrays.equals(nestingConditions.get(sharedLevelCount), 
					other.nestingConditions.get(sharedLevelCount))){
				break;
			}
			sharedLevelCount++;
		}
		return sharedLevelCount-1;
	}
	
	protected String toItem(){
		StringBuilder itemStr = new StringBuilder();
		itemStr.append(new String(new byte[]{actionID}, StandardCharsets.US_ASCII));
		for(byte[] cond : nestingConditions){
			itemStr.append(new String(new byte[]{127}, StandardCharsets.US_ASCII)); 
			itemStr.append(new String(cond, StandardCharsets.US_ASCII));
		}
		return itemStr.toString();
	}
}
