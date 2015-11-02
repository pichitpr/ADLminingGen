package adl_2daa.gen.profile;

import adl_2daa.gen.Miner;

/**
 * Store frequent relations usage of the dataset discovered by miner
 */
public class RelationUsage {
	
	private int[] sequenceUsageCount;
	private int[] sequenceGotoUsageCount;
	private int[] sequenceDespawnUsageCount;
	private int[] parallelUsageCount;
	private int[] parallelInterEntityUsageCount;
	private int[] nestingUsageCount;
	private int[] totalCount = new int[6];
	
	public RelationUsage(Miner miner){
		sequenceUsageCount = new int[miner.getFrequentOrder().size()];
		for(int i=0; i<sequenceUsageCount.length; i++){
			sequenceUsageCount[i] = miner.getFrequentOrder().get(i).getSequenceIDs().size();
			totalCount[0] += sequenceUsageCount[i];
		}
		
		sequenceGotoUsageCount = new int[miner.getFrequentInterStateOrder_Goto().size()];
		for(int i=0; i<sequenceGotoUsageCount.length; i++){
			sequenceGotoUsageCount[i] = miner.getFrequentInterStateOrder_Goto().get(i)
					.getSequenceIds().size();
			totalCount[1] += sequenceGotoUsageCount[i];
		}
		
		sequenceDespawnUsageCount = new int[miner.getFrequentInterStateOrder_Despawn().size()];
		for(int i=0; i<sequenceDespawnUsageCount.length; i++){
			sequenceDespawnUsageCount[i] = miner.getFrequentInterStateOrder_Despawn().get(i)
					.getSequenceIds().size();
			totalCount[2] += sequenceDespawnUsageCount[i];
		}
		
		parallelUsageCount = new int[miner.getFrequentParallel().size()];
		for(int i=0; i<parallelUsageCount.length; i++){
			parallelUsageCount[i] = miner.getFrequentParallel().get(i).getGraphIDs().size();
			totalCount[3] += parallelUsageCount[i];
		}
		
		parallelInterEntityUsageCount = new int[miner.getFrequentInterEntityParallel().size()];
		for(int i=0; i<parallelInterEntityUsageCount.length; i++){
			parallelInterEntityUsageCount[i] = miner.getFrequentInterEntityParallel().get(i)
					.getGraphIDs().size();
			totalCount[4] += parallelInterEntityUsageCount[i];
		}
		
		nestingUsageCount = new int[miner.getFrequentNesting().size()];
		for(int i=0; i<nestingUsageCount.length; i++){
			nestingUsageCount[i] = miner.getFrequentNesting().get(i).getGraphIDs().size();
			totalCount[5] += nestingUsageCount[i];
		}
	}
	
}
