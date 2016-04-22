package adl_2daa.gen;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;

import parsemis.extension.GraphPattern;
import parsemis.extension.SimpleMiner;
import spmf.extension.algorithm.seqgen.AlgoPrefixSpanGen;
import spmf.extension.algorithm.seqgen.AlgoPrefixSpanJSGen;
import spmf.extension.algorithm.seqgen.SequentialPatternGen;
import spmf.extension.algorithm.seqgen.SequentialPatternsGen;
import spmf.extension.input.sequence_db_generic.SequenceDatabaseGen;
import spmf.extension.patterns.itemset_list_generic.ItemsetGen;
import spmf.extension.prefixspan.JSPatternGen;
import adl_2daa.gen.encoder.DatabaseCreator;
import adl_2daa.gen.encoder.EncodeTable;
import adl_2daa.gen.encoder.NestingLiteralCollector;
import adl_2daa.gen.generator.ASTUtility;
import adl_2daa.gen.profile.AgentProfile;
import adl_2daa.gen.signature.GeneratorRegistry;
import adl_2daa.gen.testtool.TestUtility;
import de.parsemis.graph.Edge;
import de.parsemis.graph.Graph;
import de.parsemis.graph.Node;

/**
 * This class mines interesting relations and other information
 * from specified dataset and store in memory
 */
public class Miner {

	private DatabaseCreator dbCreator;
	private List<SequentialPatternGen<String>> frequentOrder;
	private List<JSPatternGen<String>> frequentInterStateOrder_Goto;
	private List<JSPatternGen<String>> frequentInterStateOrder_Despawn;
	private List<GraphPattern<String,Integer>> frequentParallel;
	private List<GraphPattern<String,Integer>> frequentInterEntityParallel;
	private List<GraphPattern<Integer,Integer>> frequentNesting;
	
	public List<SequentialPatternGen<String>> getFrequentOrder() {
		return frequentOrder;
	}

	public List<JSPatternGen<String>> getFrequentInterStateOrder_Goto() {
		return frequentInterStateOrder_Goto;
	}

	public List<JSPatternGen<String>> getFrequentInterStateOrder_Despawn() {
		return frequentInterStateOrder_Despawn;
	}

	public List<GraphPattern<String,Integer>> getFrequentParallel() {
		return frequentParallel;
	}

	public List<GraphPattern<String,Integer>> getFrequentInterEntityParallel() {
		return frequentInterEntityParallel;
	}

	public List<GraphPattern<Integer,Integer>> getFrequentNesting() {
		return frequentNesting;
	}
	
	public void clearResult(){
		frequentOrder = null;
		frequentInterStateOrder_Goto = null;
		frequentInterStateOrder_Despawn = null;
		frequentParallel = null;
		frequentInterEntityParallel = null;
		frequentNesting = null;
	}
	
	public void dumpDatasetProfile(File file){
		try {
			FileUtils.write(file, dbCreator.createProfileDump());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Setup dataset to be mined
	 */
	public void initialize(String directory){
		dbCreator = new DatabaseCreator();
		dbCreator.load(directory);
	}
	
	/**
	 * Mine ADL as sequence and stored in memory 
	 */
	@SuppressWarnings("unchecked")
	public void mineSequenceOrdering(double relativeMinSup, boolean verbose, 
			boolean filterOutLength1){
		Object[] db = dbCreator.createDatabaseForOrder(2);
		List<List<String>>[] seqDB = (List[])db[0];
		List<Integer> idMap = (List<Integer>)db[1];
		System.out.println("min_sup : "+relativeMinSup);
		System.out.println("DB size : "+seqDB.length);
		SequenceDatabaseGen<String> spmfDB = spmf.extension.prefixspan.Utility.<String>loadGen(seqDB);
		AlgoPrefixSpanGen<String> ps = new AlgoPrefixSpanGen<String>();
		try {
			SequentialPatternsGen<String> frequentOrderSeq = 
					ps.runAlgorithm(spmfDB, relativeMinSup, null);
			System.out.println("Patterns found : "+frequentOrderSeq.sequenceCount);
			frequentOrder = new ArrayList<SequentialPatternGen<String>>();
			for(List<SequentialPatternGen<String>> level : frequentOrderSeq.levels){
				for(SequentialPatternGen<String> seq : level){
					if(isValidSequenceOrdering(seq, filterOutLength1))
						frequentOrder.add(seq);
				}
			}
			System.out.println("Valid patterns : "+frequentOrder.size());
			for(SequentialPatternGen<String> seq : frequentOrder){
				for(int seqID : seq.getSequenceIDs()){
					dbCreator.getProfile(idMap.get(seqID)).addOrderRelationUsage();
				}
				if(verbose){
					System.out.println(TestUtility.sequencePatternToByteString(seq));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private boolean isValidSequenceOrdering(SequentialPatternGen<String> seq, 
			boolean filterOutLength1){
		if(filterOutLength1 && seq.getItemsets().size() == 1){
			return false;
		}
		for(ItemsetGen<String> iset : seq.getItemsets()){
			int eActID = iset.get(0).charAt(0);
			String actionName = GeneratorRegistry.getActionName(eActID);
			if(actionName.equals("Goto") || actionName.equals("Despawn")){
				return false;
			}
		}
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public void mineInterStateOrder(double relativeMinSup, boolean verbose){
		Object[] istateSeqDB = dbCreator.createDatabaseForInterStateOrder();
		//Goto order
		SequenceDatabaseGen<String> spmfDBleft = spmf.extension.prefixspan.Utility.<String>loadGen( 
				(List<List<String>>[]) istateSeqDB[0] 
				);
		SequenceDatabaseGen<String> spmfDBright = spmf.extension.prefixspan.Utility.<String>loadGen( 
				(List<List<String>>[]) istateSeqDB[1] 
				);
		int[] tag = Arrays.<Integer>stream((Integer[])istateSeqDB[2]).
				mapToInt(i -> (int)i).toArray();
		List<Integer> idMap = (List<Integer>)istateSeqDB[3];
		AlgoPrefixSpanJSGen<String> jsps = new AlgoPrefixSpanJSGen<String>();
		try {
			System.out.println("min_sup : "+relativeMinSup);
			System.out.println("Left DB size : "+spmfDBleft.size());
			System.out.println("Right DB size : "+spmfDBright.size());
			System.out.println("Tag DB size : "+tag.length);
			jsps.prefixSpanJS(spmfDBleft, spmfDBright, tag, (int)(spmfDBleft.size()*relativeMinSup));
			frequentInterStateOrder_Goto = jsps.getResult();
			System.out.println("Patterns found : "+frequentInterStateOrder_Goto.size());
			
			/*
			//Filter out invalid pattern
			frequentInterStateOrder_Goto.removeIf(
					jspattern -> jspattern.getRightSide().getItemsets().isEmpty()
					);
					*/
			
			System.out.println("Valid patterns : "+frequentInterStateOrder_Goto.size());
			for(JSPatternGen<String> jspattern : frequentInterStateOrder_Goto){
				for(int seqID : jspattern.getSequenceIds()){
					dbCreator.getProfile(idMap.get(seqID)).addInterStateGotoRelationUsage();
				}
				if(verbose){
					System.out.println("======");
					System.out.println("Tag: "+jspattern.getTag());
					System.out.println(TestUtility.sequencePatternToByteString(jspattern.getLeftSide()));
					System.out.println("Goto\n>");
					System.out.println(TestUtility.sequencePatternToByteString(jspattern.getRightSide()));
					System.out.println(jspattern.getSequenceIds());
					System.out.println("======\n");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Despawn order
		spmfDBleft = spmf.extension.prefixspan.Utility.<String>loadGen( 
				(List<List<String>>[]) istateSeqDB[4] 
				);
		spmfDBright = spmf.extension.prefixspan.Utility.<String>loadGen( 
				(List<List<String>>[]) istateSeqDB[5] 
				);
		tag = Arrays.<Integer>stream((Integer[])istateSeqDB[6]).
				mapToInt(i -> (int)i).toArray();
		idMap = (List<Integer>)istateSeqDB[7];
		try {
			System.out.println("min_sup : "+relativeMinSup);
			System.out.println("Left DB size : "+spmfDBleft.size());
			System.out.println("Right DB size : "+spmfDBright.size());
			System.out.println("Tag DB size : "+tag.length);
			jsps.prefixSpanJS(spmfDBleft, spmfDBright, tag, (int)(spmfDBleft.size()*relativeMinSup));
			frequentInterStateOrder_Despawn = jsps.getResult();
			System.out.println("Patterns found : "+frequentInterStateOrder_Despawn.size());
			
			/*
			//Filter out invalid pattern
			frequentInterStateOrder_Despawn.removeIf(
					jspattern -> jspattern.getRightSide().getItemsets().isEmpty()
					);
					*/
			
			System.out.println("Valid patterns : "+frequentInterStateOrder_Despawn.size());
			for(JSPatternGen<String> jspattern : frequentInterStateOrder_Despawn){
				for(int seqID : jspattern.getSequenceIds()){
					dbCreator.getProfile(idMap.get(seqID)).addInterStateDespawnRelationUsage();;
				}
				if(verbose){
					System.out.println("======");
					System.out.println("Tag: "+jspattern.getTag());
					System.out.println(TestUtility.sequencePatternToByteString(jspattern.getLeftSide()));
					System.out.println("Despawn\n>");
					System.out.println(TestUtility.sequencePatternToByteString(jspattern.getRightSide()));
					System.out.println(jspattern.getSequenceIds());
					System.out.println("======\n");
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void mineParallelSequence(double relativeMinSup, boolean verbose){
		Object[] db = dbCreator.createDatabaseForParallel();
		Collection<Graph<String,Integer>> graphDB = (Collection<Graph<String,Integer>>)db[0];
		List<Integer> idMap = (List<Integer>)db[1];
		System.out.println("min_sup : "+relativeMinSup);
		System.out.println("DB size : "+graphDB.size());
		frequentParallel = SimpleMiner.<String,Integer>mine(
				graphDB, (int)(graphDB.size()*relativeMinSup), 8
				);
		System.out.println("Patterns found : "+frequentParallel.size());
		frequentParallel.removeIf( pattern -> !isValidParallelGraph(pattern.getGraph()) );
		System.out.println("Valid patterns : "+frequentParallel.size());
		
		for(GraphPattern<String,Integer> pattern : frequentParallel){
			for(int id : pattern.getGraphIDs()){
				dbCreator.getProfile(idMap.get(id)).addParallelRelationUsage();
			}
			if(verbose){
				System.out.println("===============");
				System.out.println(TestUtility.parallelGraphToByteString(pattern.getGraph()));
				System.out.println(pattern.getGraphIDs());
				System.out.println("===============\n");
			}
		}
	}
	
	private boolean isValidParallelGraph(Graph<String,Integer> g){
		Iterator<Edge<String,Integer>> edgeIt = g.edgeIterator();
		Node<String,Integer> sequenceNode = null;
		Edge<String,Integer> edge;
		int sequenceCounter = 0;
		while(edgeIt.hasNext()){
			edge = edgeIt.next();
			if(edge.getLabel() == EncodeTable.STATE_SEQUENCE_EDGE){
				sequenceNode = (edge.getDirection() == Edge.OUTGOING) ? edge.getNodeB() : edge.getNodeA();
				if(sequenceNode.getOutDegree() >= 1) 
					sequenceCounter++;
			}
		}
		return sequenceCounter > 1;
	}
	
	@SuppressWarnings("unchecked")
	public void mineInterEntityParallelSequence(double relativeMinSup, boolean verbose){
		Object[] db = dbCreator.createDatabaseForInterEntityParallel();
		Collection<Graph<String,Integer>> graphDB = (Collection<Graph<String,Integer>>)db[0];
		List<Integer> idMap = (List<Integer>)db[1];
		System.out.println("min_sup : "+relativeMinSup);
		System.out.println("DB size : "+graphDB.size());
		frequentInterEntityParallel = SimpleMiner.<String,Integer>mine(
				graphDB, (int)(graphDB.size()*relativeMinSup), 8
				);
		System.out.println("Patterns found : "+frequentInterEntityParallel.size());
		frequentInterEntityParallel.removeIf( pattern -> !isValidInterEntityParallelGraph(pattern.getGraph()) );
		System.out.println("Valid patterns : "+frequentInterEntityParallel.size());
		
		for(GraphPattern<String,Integer> pattern : frequentInterEntityParallel){
			for(int id : pattern.getGraphIDs()){
				dbCreator.getProfile(idMap.get(id)).addInterEntityParallelRelationUsage();
			}
			if(verbose){
				System.out.println("===============");
				System.out.println(TestUtility.parallelGraphToByteString(pattern.getGraph()));
				System.out.println("===============\n");
			}
		}
	}
	
	private boolean isValidInterEntityParallelGraph(Graph<String,Integer> g){
		Iterator<Edge<String,Integer>> edgeIt = g.edgeIterator();
		Node<String,Integer> sequenceNode = null;
		Edge<String,Integer> edge;
		boolean hasSpawnerSequence = false;
		boolean hasChildSequence = false;
		boolean hasTag = false;
		while(edgeIt.hasNext()){
			edge = edgeIt.next();
			if(edge.getLabel() == EncodeTable.STATE_SEQUENCE_EDGE){
				sequenceNode = (edge.getDirection() == Edge.OUTGOING) ? edge.getNodeB() : edge.getNodeA();
				if(sequenceNode.getOutDegree() < 1) continue;
				edge = sequenceNode.outgoingEdgeIterator().next(); //Sampling 1 outgoing edge
				if(edge.getLabel() == EncodeTable.SEQUENCE_ACTION_EDGE){
					hasSpawnerSequence = true;
				}else if(edge.getLabel() == EncodeTable.SEQUENCE_ACTION_OTHER_ENTITY_EDGE){
					hasChildSequence = true;
				}
			}else if(edge.getLabel() == EncodeTable.TAG_EDGE){
				hasTag = true;
			}
		}
		return hasChildSequence && hasSpawnerSequence && hasTag;
	}
	
	@SuppressWarnings("unchecked")
	public void mineNesting(double relativeMinSup, boolean verbose){
		Object[] db = dbCreator.createDatabaseForNesting();
		Collection<Graph<Integer,Integer>> graphDB = (Collection<Graph<Integer,Integer>>)db[0];
		List<Integer> idMap = (List<Integer>)db[1];
		System.out.println("min_sup : "+relativeMinSup);
		System.out.println("DB size : "+graphDB.size());
		frequentNesting = SimpleMiner.<Integer,Integer>mine(
				graphDB, (int)(graphDB.size()*relativeMinSup), 8
				);
		System.out.println("Patterns found : "+frequentNesting.size());
		frequentNesting.removeIf(graph -> graph.getGraph().getNodeCount() <= 1);
		System.out.println("Valid patterns : "+frequentNesting.size());
		
		for(GraphPattern<Integer,Integer> pattern : frequentNesting){
			for(int id : pattern.getGraphIDs()){
				dbCreator.getProfile(idMap.get(id)).addNestingRelationUsage();
			}
			NestingLiteralCollector.collectPossibleLiteral(graphDB, pattern);
			if(verbose){
				System.out.println("===============");
				System.out.println(TestUtility.nestingGraphToString(pattern.getGraph()));
				System.out.println(pattern.getGraphIDs());
				System.out.println("===============\n");
			}
		}
	}
	
	public AgentProfile[] generateAgentProfile(){
		AgentProfile mainAgent = new AgentProfile();
		mainAgent.setId(1);
		mainAgent.setComplexAgent(false);
		mainAgent.setMainAgent(true);
		mainAgent.setRootName("Sample");
		
		int orderCount = dbCreator.getRandomProfile().getOrderRelationUsage();
		int interStateCount = dbCreator.getRandomProfile().getInterStateGotoRelationUsage();
		int interStateDesCount = dbCreator.getRandomProfile().getInterStateDespawnRelationUsage();
		int parallelCount = dbCreator.getRandomProfile().getParallelRelationUsage();
		int interEntityCount = dbCreator.getRandomProfile().getParallelInterEntityRelationUsage();
		boolean hasDes = dbCreator.getRandomProfile().hasDes();
		
		int stateCount = dbCreator.getRandomMainProfile().getStructureInfo().length;
		if(stateCount <= 0){
			stateCount = 1;
		}
		int[] structureInfo = new int[stateCount];
		for(int i=0; i<structureInfo.length; i++){
			if(i == 0){
				structureInfo[i] = dbCreator.getRandomMainProfile().getStructureInfo()[0];
			}else{
				structureInfo[i] = randomNonInitStateCountFromMainProfile();
			}
			if(structureInfo[i] <= 0){
				structureInfo[i] = 1;
			}
		}
		
		mainAgent.setRelationUsage(orderCount, interStateCount, interStateDesCount, parallelCount, interEntityCount);
		mainAgent.setStructureInfo(hasDes, structureInfo);
		
		return new AgentProfile[]{mainAgent};
	}
	
	private int randomNonInitStateCountFromMainProfile(){
		int randomLimit = 100;
		while(randomLimit > 0){
			int[] info = dbCreator.getRandomMainProfile().getStructureInfo();
			if(info.length > 1){
				return info[ASTUtility.randomRange(1, info.length-1)];
			}
			randomLimit--;
		}
		int[] info = dbCreator.getRandomMainProfile().getStructureInfo();
		return info[ASTUtility.randomRange(0, info.length-1)]; 
	}
}
