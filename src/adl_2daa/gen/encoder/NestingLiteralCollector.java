package adl_2daa.gen.encoder;

import java.util.Collection;
import java.util.Iterator;

import parsemis.extension.GraphPattern;
import adl_2daa.gen.Utility;
import de.parsemis.graph.Edge;
import de.parsemis.graph.Graph;
import de.parsemis.graph.MutableGraph;
import de.parsemis.graph.Node;

public class NestingLiteralCollector {

	/**
	 * Verify all graphs containing specified pattern in the DB. Then, gather all literal values that are used in those graphs
	 * for each parameter in the pattern.
	 */
	public static void collectPossibleLiteral(Collection<Graph<Integer,Integer>> graphDB, GraphPattern<Integer,Integer> pattern){
		Node<Integer,Integer> patternRoot = Utility.findFirstRoot(pattern.getGraph());
		
		Iterator<Graph<Integer,Integer>> it = graphDB.iterator();
		while(it.hasNext()){
			Graph<Integer,Integer> graph = it.next();
			if(!pattern.getGraphIDs().contains(graph.getID())) continue;
			Node<Integer,Integer> root = Utility.findFirstRoot(graph);
			if(!Utility.exactSubgraphTest(root,patternRoot)) continue;
			collectLiteral(root, patternRoot);
		}
	}
	
	private static void collectLiteral(Node<Integer,Integer> root, Node<Integer,Integer> patternRoot){
		//Loop through all outgoing edge of subgraph
		Iterator<Edge<Integer,Integer>> edgeIt = root.outgoingEdgeIterator();
		while(edgeIt.hasNext()){
			Edge<Integer,Integer> edge = edgeIt.next();
			Iterator<Edge<Integer,Integer>> patternEdgeIt = patternRoot.outgoingEdgeIterator();
			int literalCollectionEdgeLabel = edge.getLabel() + EncodeTable.collectionEdgeLabelOffset;
			
			/*
			 * Check if this subgraph edge should be explored. The edge should be explored IF
			 * - Pattern node CONTAINS "LiteralCollection" node for this edge  OR
			 * - Pattern node does NOT have edge with the same label
			 */
			boolean shouldExplorethisEdge = true;
			Node<Integer,Integer> literalCollectionNode = null;
			while(patternEdgeIt.hasNext()){
				Edge<Integer,Integer> patternEdge = patternEdgeIt.next();
				//Pattern edge with the same label -- the edge is not valid
				if(patternEdge.getLabel() == edge.getLabel()){
					shouldExplorethisEdge = false;
					break;
				}
				//Pattern edge with corresponding LiteralCollection node
				if(patternEdge.getLabel() == literalCollectionEdgeLabel){
					shouldExplorethisEdge = true;
					literalCollectionNode = patternEdge.getOtherNode(patternRoot);
					break;
				}
			}
			if(!shouldExplorethisEdge) continue;
			
			//Explore the node, add to the collection in pattern if it is literal
			Node<Integer,Integer> exploringNode  = edge.getOtherNode(root);
			if(!ADLNestingDecoder.isLiteral(exploringNode.getLabel()))
				continue;
			
			MutableGraph<Integer, Integer> patternGraph = (MutableGraph<Integer, Integer>)patternRoot.getGraph();
			if(literalCollectionNode == null){
				literalCollectionNode = patternGraph.addNode(0);
				patternGraph.addEdge(patternRoot, literalCollectionNode, literalCollectionEdgeLabel, Edge.OUTGOING);
			}
			
			patternGraph.addNodeAndEdge(literalCollectionNode, exploringNode.getLabel(), literalCollectionNode.getOutDegree(), 
					Edge.OUTGOING);
		}
	}
}
