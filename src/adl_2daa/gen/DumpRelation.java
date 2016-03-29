package adl_2daa.gen;

import java.io.File;

import org.apache.commons.io.FileUtils;

import adl_2daa.gen.generator.InterEntityParallelMerger;
import adl_2daa.gen.generator.InterStateOrderMerger;
import adl_2daa.gen.generator.NestingMerger;
import adl_2daa.gen.generator.OrderMerger;
import adl_2daa.gen.generator.ParallelMerger;
import adl_2daa.gen.testtool.TestInitializer;
import parsemis.extension.GraphPattern;
import spmf.extension.algorithm.seqgen.SequentialPatternGen;
import spmf.extension.prefixspan.JSPatternGen;

public class DumpRelation {
	
	public static String supString(double minSup,int count){
		return String.format("%1$.3f (%2$d)", minSup, count);
	}
	
	public static void main(String[] args){
		try{
			TestInitializer.init();
			Miner gen = new Miner();
			gen.initialize("data");
			gen.clearResult();
			
			StringBuilder strb;
			double minSup;
			
			minSup = 20;
			while(minSup > 0.005){
				strb = new StringBuilder();
				gen.mineSequenceOrdering(minSup, false, true);
				for(SequentialPatternGen<String> pattern : gen.getFrequentOrder()){
					OrderMerger.instance.decodeAndDumpRelation(pattern, strb);
					strb.append('\n').append('\n').append('\n');
				}
				FileUtils.write(new File("result_script/order-"+supString(minSup, gen.getFrequentOrder().size())+".txt"), strb.toString());
				if(minSup > 1) minSup -= 1;
				else if(minSup > 0.5) minSup -= 0.5;
				else minSup -= 0.01;
			}
			
			minSup = 20;
			while(minSup > 0.005){
				strb = new StringBuilder();
				gen.mineInterStateOrder(minSup, false);
				for(JSPatternGen<String> pattern : gen.getFrequentInterStateOrder_Goto()){
					InterStateOrderMerger.instance.decodeAndDumpRelation(false, pattern, strb);
					strb.append('\n').append('\n').append('\n');
				}
				FileUtils.write(new File("result_script/goto-"+supString(minSup, gen.getFrequentInterStateOrder_Goto().size())+".txt"), strb.toString());
				for(JSPatternGen<String> pattern : gen.getFrequentInterStateOrder_Despawn()){
					InterStateOrderMerger.instance.decodeAndDumpRelation(true, pattern, strb);
					strb.append('\n').append('\n').append('\n');
				}
				FileUtils.write(new File("result_script/despawn-"+supString(minSup, gen.getFrequentInterStateOrder_Despawn().size())+".txt"), strb.toString());
				if(minSup > 1) minSup -= 1;
				else if(minSup > 0.5) minSup -= 0.5;
				else minSup -= 0.01;
			}
			
			minSup = 20;
			while(minSup > 0.005){
				strb = new StringBuilder();
				gen.mineParallelSequence(minSup, false);
				for(GraphPattern<String,Integer> pattern : gen.getFrequentParallel()){
					ParallelMerger.instance.decodeAndDumpRelation(pattern, strb);
					strb.append('\n').append('\n').append('\n');
				}
				FileUtils.write(new File("result_script/parallel-"+supString(minSup, gen.getFrequentParallel().size())+".txt"), strb.toString());
				if(minSup > 1) minSup -= 1;
				else if(minSup > 0.5) minSup -= 0.5;
				else minSup -= 0.01;
			}
			
			minSup = 20;
			while(minSup > 0.005){
				strb = new StringBuilder();
				gen.mineNesting(minSup, false);
				NestingMerger.instance.decodeAndDumpRelation(gen.getFrequentNesting(), strb);
				FileUtils.write(new File("result_script/nesting-"+supString(minSup,gen.getFrequentNesting().size())+".txt"), strb.toString());
				if(minSup > 1) minSup -= 1;
				else if(minSup > 0.5) minSup -= 0.5;
				else minSup -= 0.01;
			}
				
			minSup = 20;
			while(minSup > 0.1){
				strb = new StringBuilder();
				gen.mineInterEntityParallelSequence(minSup, false);
				for(GraphPattern<String,Integer> pattern : gen.getFrequentInterEntityParallel()){
					InterEntityParallelMerger.instance.decodeAndDumpRelation(pattern, strb);
					strb.append('\n').append('\n').append('\n');
				}
				FileUtils.write(new File("result_script/inter-entity-"+supString(minSup,gen.getFrequentInterEntityParallel().size())+".txt"), strb.toString());
				if(minSup > 1) minSup -= 1;
				else minSup -= 0.05;
			}	
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
	}
}
