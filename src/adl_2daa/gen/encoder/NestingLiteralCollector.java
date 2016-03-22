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
			int graphID = Integer.parseInt(graph.getName());
			if(!pattern.getGraphIDs().contains(graphID)) continue;
			Iterator<Node<Integer,Integer>> nodeIt = graph.nodeIterator();
			while(nodeIt.hasNext()){
				Node<Integer,Integer> node = nodeIt.next();
				if(!exactSubgraphTest(node,patternRoot)) continue;
				collectLiteral(node, patternRoot);
			}
		}
	}
	
	private static void collectLiteral(Node<Integer,Integer> root, Node<Integer,Integer> patternRoot){
		//Loop through all outgoing edge of subgraph
		Iterator<Edge<Integer,Integer>> edgeIt = root.outgoingEdgeIterator();
		while(edgeIt.hasNext()){
			Edge<Integer,Integer> edge = edgeIt.next();
			Iterator<Edge<Integer,Integer>> patternEdgeIt = patternRoot.outgoingEdgeIterator();
			
			/*
			 * Check if this subgraph edge should be explored. The edge should be explored IF
			 * - Pattern node CONTAINS "LiteralCollection" node for this edge  OR
			 * - Pattern node does NOT have edge with the same label
			 */
			boolean shouldExplorethisEdge = true;
			Node<Integer,Integer> literalCollectionNode = null;
			while(patternEdgeIt.hasNext()){
				Edge<Integer,Integer> patternEdge = patternEdgeIt.next();
				if(patternEdge.getLabel() == edge.getLabel()){
					//Edge label matched -- explore if it is LiteralCollection (otherwise, this means it has function and should not be explored)
					if(patternEdge.getOtherNode(patternRoot).getLabel() == EncodeTable.LITERAL_COLLECTION_ROOT){
						shouldExplorethisEdge = true;
						literalCollectionNode = patternEdge.getOtherNode(patternRoot);
					}else{
						shouldExplorethisEdge = false;
					}
					break;
				}
			}
			if(!shouldExplorethisEdge) continue;
			
			//LiteralCollection node not exist, create new one
			MutableGraph<Integer, Integer> patternGraph = (MutableGraph<Integer, Integer>)patternRoot.getGraph();
			if(literalCollectionNode == null){
				literalCollectionNode = patternGraph.addNode(EncodeTable.LITERAL_COLLECTION_ROOT);
				patternGraph.addEdge(patternRoot, literalCollectionNode, edge.getLabel(), Edge.OUTGOING);
			}
			
			//Explore the node and add it to the collection (encoding used if literal)
			Node<Integer,Integer> exploringNode  = edge.getOtherNode(root);
			if(ADLNestingDecoder.isLiteral(exploringNode.getLabel())){
				patternGraph.addNodeAndEdge(literalCollectionNode, exploringNode.getLabel(), literalCollectionNode.getOutDegree(), 
						Edge.OUTGOING);
			}else{
				 cloneGraphAndAttach(exploringNode, literalCollectionNode, literalCollectionNode.getOutDegree(), patternGraph);
			}
		}
	}
	
	/**
	 * Create a copy of originalNode and attach the new node to attachTarget using OUTGOING edge (attachTarget --> new node) 
	 * with edgeLabel as label. edgeLabel should be the same as the INCOMING edge that connect originalNode
	 * (??? --> originalNode)
	 */
	private static void cloneGraphAndAttach(Node<Integer,Integer> originalNode, Node<Integer,Integer> attachTarget, 
			int edgeLabel, MutableGraph<Integer, Integer> graphBuilder){
		Node<Integer,Integer> newNode = graphBuilder.addNodeAndEdge(attachTarget, originalNode.getLabel(), edgeLabel, 
				Edge.OUTGOING);
		Iterator<Edge<Integer,Integer>> edgeIt = originalNode.outgoingEdgeIterator();
		while(edgeIt.hasNext()){
			Edge<Integer,Integer> edge = edgeIt.next();
			cloneGraphAndAttach(edge.getOtherNode(originalNode), newNode, edge.getLabel(), graphBuilder);
		}
	}
	
	private static <N,E> boolean exactSubgraphTest(Node<N,E> root, Node<N,E> patternRoot){
		//Test node
		if(!root.getLabel().equals(patternRoot.getLabel()))
			return false;
		
		//Test edge and subgraph
		Iterator<Edge<N,E>> patternEdgeIt = patternRoot.outgoingEdgeIterator();
		while(patternEdgeIt.hasNext()){
			Edge<N,E> patternEdge = patternEdgeIt.next();
			Iterator<Edge<N,E>> edgeIt = root.outgoingEdgeIterator();
			Edge<N,E> matchingEdge = null;
			boolean matchingEdgeFound = false;
			while(edgeIt.hasNext()){
				matchingEdge = edgeIt.next();
				if(matchingEdge.getLabel().equals(patternEdge.getLabel())){
					matchingEdgeFound = true;
					break;
				}
			}
			if(!matchingEdgeFound)
				return false;
			//Do not explore further if literalCollection found (this kind of node is appended during collecting process)
			if(patternEdge.getOtherNode(patternRoot).getLabel().equals(EncodeTable.LITERAL_COLLECTION_ROOT))
				return true;
			if(!exactSubgraphTest(matchingEdge.getOtherNode(root), patternEdge.getOtherNode(patternRoot)) )
				return false;
		}
		
		return true;
	}
}
