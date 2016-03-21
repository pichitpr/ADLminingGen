package adl_2daa.gen.encoder;

import java.util.Collection;
import java.util.Iterator;

import parsemis.extension.GraphPattern;
import adl_2daa.gen.Utility;
import adl_2daa.gen.signature.GeneratorRegistry;
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
		/*
		System.out.println("PATTERN "+pattern.getGraph().getID());
		for(Integer gid : pattern.getGraphIDs()){
			System.out.print(gid+" ");
		}
		System.out.println("");
		*/
		Node<Integer,Integer> patternRoot = Utility.findFirstRoot(pattern.getGraph());
		Iterator<Graph<Integer,Integer>> it = graphDB.iterator();
		while(it.hasNext()){
			Graph<Integer,Integer> graph = it.next();
			int graphID = Integer.parseInt(graph.getName());
			if(!pattern.getGraphIDs().contains(graphID)) continue;
			Iterator<Node<Integer,Integer>> nodeIt = graph.nodeIterator();
			while(nodeIt.hasNext()){
				Node<Integer,Integer> node = nodeIt.next();
				if(!Utility.exactSubgraphTest(node,patternRoot)) continue;
				collectLiteral(node, patternRoot);
			}
		}
		//System.out.println("#############################################");
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
			
			/*
			System.out.println("GraphID "+patternGraph.getID());
			Iterator<Edge<Integer,Integer>> edgeIt_ = literalCollectionNode.incommingEdgeIterator();
			Edge<Integer,Integer> edge__ = edgeIt_.next();
			int funcLabel = edge__.getOtherNode(literalCollectionNode).getLabel();
			if(funcLabel < EncodeTable.idOffset && funcLabel >= 0){
				if(!GeneratorRegistry.getActionName(funcLabel).equals("Set#direction"))
					return;
				System.out.print("Action: "+GeneratorRegistry.getActionName(funcLabel));
			}else{
				System.out.print("Function: "+GeneratorRegistry.getFunctionName(funcLabel-EncodeTable.idOffset));
				return;
			}
			System.out.println("["+edge__.getLabel()+"]");
			edgeIt_ = literalCollectionNode.outgoingEdgeIterator();
			while(edgeIt_.hasNext()){
				Edge<Integer,Integer> edge_ = edgeIt_.next();
				System.out.print(edge_.getLabel()+" ");
				int label_ = edge_.getOtherNode(literalCollectionNode).getLabel();
				if(ADLNestingDecoder.isLiteral(label_)){
					int expectType = (label_ >> 29) & 7;
					switch(expectType){
					case 0: System.out.println("Boolean"); break;
					case 1: System.out.println("Int"); break;
					case 2: System.out.println("Float"); break;
					case 3: System.out.println("Direction"); break;
					case 4: case 5: case 6:
						System.out.println("Position"); break;
					case 7:
						System.out.println("Collider"); break;
					default:
						System.out.println("NOTFOUND"); break;
					}
				}else{
					System.out.println("function: "+GeneratorRegistry.getFunctionName(label_-EncodeTable.idOffset));
				}
			}
			
			System.out.println("-------------------------------------------->>>>>>>");
			*/
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
}
