package adl_2daa.gen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import adl_2daa.gen.testtool.TestInitializer;

public class Main {

	public static void main(String[] args){
		//runMinerTest();
		
		TestInitializer.init();
		Miner gen = new Miner();
		gen.initialize("data");
		gen.mineInterEntityParallelSequence(0.02, false);
	}
	
	public static void runMinerTest(){
		TestInitializer.init();
		Miner gen = new Miner();
		gen.initialize("data");
		
		double minSupport;
		String csvRow;
		
		Path[] path = new Path[]{
				Paths.get("result", "order.csv"),
				Paths.get("result", "order_goto.csv"),
				Paths.get("result", "order_despawn.csv"),
				Paths.get("result", "parallel.csv"),
				Paths.get("result", "parallel_inter.csv"),
				Paths.get("result", "nesting.csv"),
		};
		for(int i=0; i<path.length; i++){
			try{
				Files.deleteIfExists(path[i]);
				Files.createFile(path[i]);
				minSupport = 0.5;
				while(minSupport-0.01 >= -0.0005){
					if(i != 2)
						gen.clearResult();
					csvRow = minSupport+","+mineAndGetResult(gen, i, minSupport)+
							System.getProperty("line.separator");
					Files.write(path[i], 
							csvRow.getBytes(), 
							StandardOpenOption.APPEND);
					minSupport -= 0.01;
				}
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	
	private static int mineAndGetResult(Miner miner, int mode, double minSup){
		switch(mode){
		case 0:
			miner.mineSequenceOrdering(minSup, false, true);
			return miner.getFrequentOrder().size();
		case 1:
			miner.mineInterStateOrder(minSup, false);
			return miner.getFrequentInterStateOrder_Goto().size();
		case 2:
			return miner.getFrequentInterStateOrder_Despawn().size();
		case 3:
			miner.mineParallelSequence(minSup, false);
			return miner.getFrequentParallel().size();
		case 4:
			miner.mineInterEntityParallelSequence(minSup, false);
			return miner.getFrequentInterEntityParallel().size();
		case 5:
			miner.mineNesting(minSup, false);
			return miner.getFrequentNesting().size();
		}
		return -1;
	}
}
