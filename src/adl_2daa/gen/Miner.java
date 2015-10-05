package adl_2daa.gen;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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

public class Miner {

	private DatabaseCreator dbCreator;
	private SequentialPatternsGen<String> frequentOrder;
	private List<JSPatternGen<String>> frequentInterStateOrder_Goto;
	private List<JSPatternGen<String>> frequentInterStateOrder_Despawn;
	private Collection<Graph<String,Integer>> frequentParallel;
	private Collection<Graph<String,Integer>> frequentInterEntityParallel;
	private Collection<Graph<Integer,Integer>> frequentNesting;
	
	public SequentialPatternsGen<String> getFrequentOrder() {
		return frequentOrder;
	}

	public List<JSPatternGen<String>> getFrequentInterStateOrder_Goto() {
		return frequentInterStateOrder_Goto;
	}

	public List<JSPatternGen<String>> getFrequentInterStateOrder_Despawn() {
		return frequentInterStateOrder_Despawn;
	}

	public Collection<Graph<String, Integer>> getFrequentParallel() {
		return frequentParallel;
	}

	public Collection<Graph<String, Integer>> getFrequentInterEntityParallel() {
		return frequentInterEntityParallel;
	}

	public Collection<Graph<Integer, Integer>> getFrequentNesting() {
		return frequentNesting;
	}

	/**
	 * Setup dataset to be mined
	 */
	public void initialize(String directory){
		dbCreator = new DatabaseCreator();
		dbCreator.load(directory);
	}
	
	public void mineSequenceOrdering(double relativeMinSup, boolean verbose){
		List<List<String>>[] seqDB = dbCreator.createDatabaseForOrder(2);
		System.out.println("DB size : "+seqDB.length);
		SequenceDatabaseGen<String> spmfDB = spmf.extension.prefixspan.Utility.<String>loadGen(seqDB);
		AlgoPrefixSpanGen<String> ps = new AlgoPrefixSpanGen<String>();
		try {
			frequentOrder = ps.runAlgorithm(spmfDB, relativeMinSup, null);
			System.out.println("Patterns found : "+frequentOrder.sequenceCount);
			if(verbose){
				for(List<SequentialPatternGen<String>> level : frequentOrder.levels){
					for(SequentialPatternGen<String> seq : level){
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
		AlgoPrefixSpanJSGen<String> jsps = new AlgoPrefixSpanJSGen<String>();
		try {
			System.out.println("Left DB size : "+spmfDBleft.size());
			System.out.println("Right DB size : "+spmfDBright.size());
			System.out.println("Tag DB size : "+tag.length);
			jsps.prefixSpanJS(spmfDBleft, spmfDBright, tag, (int)(spmfDBleft.size()*relativeMinSup));
			frequentInterStateOrder_Goto = jsps.getResult();
			System.out.println("Patterns found : "+frequentInterStateOrder_Goto.size());
			
			if(verbose){
				for(JSPatternGen<String> jspattern : frequentInterStateOrder_Goto){
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
				(List<List<String>>[]) istateSeqDB[3] 
				);
		spmfDBright = spmf.extension.prefixspan.Utility.<String>loadGen( 
				(List<List<String>>[]) istateSeqDB[4] 
				);
		tag = Arrays.<Integer>stream((Integer[])istateSeqDB[5]).
				mapToInt(i -> (int)i).toArray();
		try {
			System.out.println("Left DB size : "+spmfDBleft.size());
			System.out.println("Right DB size : "+spmfDBright.size());
			System.out.println("Tag DB size : "+tag.length);
			jsps.prefixSpanJS(spmfDBleft, spmfDBright, tag, (int)(spmfDBleft.size()*relativeMinSup));
			frequentInterStateOrder_Despawn = jsps.getResult();
			System.out.println("Patterns found : "+frequentInterStateOrder_Despawn.size());
			
			if(verbose){
				for(JSPatternGen<String> jspattern : frequentInterStateOrder_Despawn){
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
	
	public void mineParallelSequence(double relativeMinSup, boolean verbose){
		Collection<Graph<String,Integer>> graphDB = dbCreator.createDatabaseForParallel();
		System.out.println("DB size : "+graphDB.size());
		frequentParallel = SimpleMiner.<String,Integer>mine(
				graphDB, (int)(graphDB.size()*relativeMinSup)
				);
		System.out.println("Patterns found : "+frequentParallel.size());
		frequentParallel.removeIf( graph -> !isValidParallelGraph(graph) );
		System.out.println("Valid pattern : "+frequentParallel.size());
		
		if(verbose){
			for(Graph<String,Integer> graph : frequentParallel){
				System.out.println("===============");
				System.out.println(TestUtility.parallelGraphToByteString(graph));
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
	
	public void mineInterEntityParallelSequence(double relativeMinSup, boolean verbose){
		Collection<Graph<String,Integer>> graphDB = dbCreator.createDatabaseForInterEntityParallel();
		System.out.println("DB size : "+graphDB.size());
		frequentInterEntityParallel = SimpleMiner.<String,Integer>mine(
				graphDB, (int)(graphDB.size()*relativeMinSup)
				);
		System.out.println("Patterns found : "+frequentInterEntityParallel.size());
		frequentInterEntityParallel.removeIf( graph -> !isValidInterEntityParallelGraph(graph) );
		System.out.println("Valid pattern : "+frequentInterEntityParallel.size());
		
		if(verbose){
			for(Graph<String,Integer> graph : frequentInterEntityParallel){
				System.out.println("===============");
				System.out.println(TestUtility.parallelGraphToByteString(graph));
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
	
	public void mineNesting(double relativeMinSup, boolean verbose){
		Collection<Graph<Integer,Integer>> graphDB = dbCreator.createDatabaseForNesting();
		System.out.println("DB size : "+graphDB.size());
		frequentNesting = SimpleMiner.<Integer,Integer>mine(
				graphDB, (int)(graphDB.size()*relativeMinSup)
				);
		System.out.println("Patterns found : "+frequentNesting.size());
	}
}
