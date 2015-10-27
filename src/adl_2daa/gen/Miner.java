package adl_2daa.gen;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import parsemis.extension.GraphPattern;
import parsemis.extension.SimpleMiner;
import spmf.extension.algorithm.seqgen.AlgoPrefixSpanGen;
import spmf.extension.algorithm.seqgen.AlgoPrefixSpanJSGen;
import spmf.extension.algorithm.seqgen.SequentialPatternGen;
import spmf.extension.algorithm.seqgen.SequentialPatternsGen;
import spmf.extension.input.sequence_db_generic.SequenceDatabaseGen;
import spmf.extension.prefixspan.JSPatternGen;
import adl_2daa.gen.encoder.DatabaseCreator;
import adl_2daa.gen.encoder.EncodeTable;
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
	private SequentialPatternsGen<String> frequentOrder;
	private List<JSPatternGen<String>> frequentInterStateOrder_Goto;
	private List<JSPatternGen<String>> frequentInterStateOrder_Despawn;
	private List<GraphPattern<String,Integer>> frequentParallel;
	private List<GraphPattern<String,Integer>> frequentInterEntityParallel;
	private List<GraphPattern<Integer,Integer>> frequentNesting;
	
	public SequentialPatternsGen<String> getFrequentOrder() {
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
	
	/**
	 * Setup dataset to be mined
	 */
	public void initialize(String directory){
		dbCreator = new DatabaseCreator();
		dbCreator.load(directory);
	}
	
	@SuppressWarnings("unchecked")
	public void mineSequenceOrdering(double relativeMinSup, boolean verbose){
		Object[] db = dbCreator.createDatabaseForOrder(2);
		List<List<String>>[] seqDB = (List[])db[0];
		List<Integer> idMap = (List<Integer>)db[1];
		System.out.println("min_sup : "+relativeMinSup);
		System.out.println("DB size : "+seqDB.length);
		SequenceDatabaseGen<String> spmfDB = spmf.extension.prefixspan.Utility.<String>loadGen(seqDB);
		AlgoPrefixSpanGen<String> ps = new AlgoPrefixSpanGen<String>();
		try {
			frequentOrder = ps.runAlgorithm(spmfDB, relativeMinSup, null);
			System.out.println("Patterns found : "+frequentOrder.sequenceCount);
			for(List<SequentialPatternGen<String>> level : frequentOrder.levels){
				for(SequentialPatternGen<String> seq : level){
					for(int seqID : seq.getSequenceIDs()){
						dbCreator.getProfile(idMap.get(seqID)).addOrderRelationUsage();;
					}
					if(verbose){
						System.out.println(TestUtility.sequencePatternToByteString(seq));
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
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
				graphDB, (int)(graphDB.size()*relativeMinSup)
				);
		System.out.println("Patterns found : "+frequentParallel.size());
		frequentParallel.removeIf( pattern -> !isValidParallelGraph(pattern.getGraph()) );
		System.out.println("Valid pattern : "+frequentParallel.size());
		
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
				sequenceCounter++;
				sequenceNode = (edge.getDirection() == Edge.OUTGOING) ? edge.getNodeB() : edge.getNodeA();
				if(sequenceNode.getOutDegree() < 1) return false;
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
		System.out.println("Valid pattern : "+frequentInterEntityParallel.size());
		
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
				if(sequenceNode.getOutDegree() < 1) return false;
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
				graphDB, (int)(graphDB.size()*relativeMinSup)
				);
		System.out.println("Patterns found : "+frequentNesting.size());
		
		for(GraphPattern<Integer,Integer> pattern : frequentNesting){
			for(int id : pattern.getGraphIDs()){
				dbCreator.getProfile(idMap.get(id)).addNestingRelationUsage();
			}
			if(verbose){
				System.out.println("===============");
				System.out.println(TestUtility.nestingGraphToString(pattern.getGraph()));
				System.out.println(pattern.getGraphIDs());
				System.out.println("===============\n");
			}
		}
	}
}