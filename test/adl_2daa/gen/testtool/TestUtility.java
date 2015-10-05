package adl_2daa.gen.testtool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

import spmf.extension.algorithm.seqgen.SequentialPatternGen;
import spmf.extension.patterns.itemset_list_generic.ItemsetGen;
import adl_2daa.gen.encoder.EncodeTable;
import adl_2daa.gen.signature.GeneratorRegistry;
import de.parsemis.graph.Edge;
import de.parsemis.graph.Graph;
import de.parsemis.graph.Node;

public class TestUtility {

	public static String byteArrayToString0(byte[] ary, boolean decode){
		String s = "";
		for(int i=0; i<ary.length; i++){
			if(decode)
				s += decode0(ary[i])+" ";
			else
				s += ary[i]+" ";
		}
		return s.trim();
	}
	
	public static String decode0(byte b){
		if(b == 127) return "|";
		else if(b >= 121) return "#"+(b-121);
		else if(b >= 101) return "d"+(b-101);
		else if(b == 95) return "Lit";
		else if(b == 94) return "Un";
		else if(b == 85) return "Cmp";
		else if(b == 75) return "Math";
		else if(b == 73) return "Lg";
		else if(b == 72) return "Else";
		else if(b == 71) return "If-E";
		else if(b == 70) return "If";
		else return ""+b;
	}
	
	public static String actionToString0(byte actionID, List<byte[]> nestingConditions,
			boolean decode){
		String s = ""+actionID;
		for(int i=0; i<nestingConditions.size(); i++){
			s += (decode ? " | " : " 127 ")+
					TestUtility.byteArrayToString0(nestingConditions.get(i), decode);
		}
		return s;
	}
	
	//======================================================
	
	public static String stringToByteString(String str){
		String s = "";
		boolean isAction = true;
		boolean isFunction = false;
		for(byte b : str.getBytes(StandardCharsets.US_ASCII)){
			if(isAction){
				isAction = false;
				s += "["+b+"] "+GeneratorRegistry.getActionName(b)+" ";
				continue;
			}else if(isFunction){
				isFunction = false;
				s += "["+b+"] "+GeneratorRegistry.getFunctionName(b)+" ";
				continue;
			}
			switch(b){
			case EncodeTable.COND_IF: s += "if "; break;
			case EncodeTable.COND_IF_IFELSE: s += "if-e "; break;
			case EncodeTable.COND_ELSE_IFELSE: s += "else "; break;
			case EncodeTable.EXP_BINARY: s += "bin "; break;
			case EncodeTable.EXP_UNARY_NEG: s += "- "; break;
			case EncodeTable.EXP_UNARY_NOT: s += "! "; break;
			case EncodeTable.EXP_FUNCTION:
				isFunction = true;
				s += "func "; 
				break;
			case EncodeTable.EXP_LITERAL: s += "lit "; break;	
			default:
				s += b+" ";
			}
		}
		return s.trim();
	}
	
	public static String readFileAsString(String dir) throws Exception{
		return readFileAsString(new File(dir));
	}
	
	public static String readFileAsString(File f) throws Exception{
		BufferedReader reader = new BufferedReader(new FileReader(f));
		String str = "";
		String line;
		while((line=reader.readLine())!=null){
			str += line+"\n";
		}
		str = str.trim();
		reader.close();
		return str;
	}
	
	public static String sequencePatternToByteString(SequentialPatternGen<String> seq){
		String result = "";
		for(ItemsetGen<String> itemset : seq.getItemsets()){
			result += stringToByteString(itemset.get(0))+"\n";
		}
		return result.trim();
	}
	
	public static String parallelGraphToByteString(Graph<String,Integer> validGraph){
		String result = "";
		Node<String,Integer> root = null;
		Iterator<Node<String,Integer>> nodeIt = validGraph.nodeIterator();
		while(nodeIt.hasNext()){
			root = nodeIt.next();
			if(root.getInDegree() == 0)
				break;
		}
		Iterator<Edge<String,Integer>> edgeIt = root.outgoingEdgeIterator();
		Edge<String,Integer> edge;
		while(edgeIt.hasNext()){
			edge = edgeIt.next();
			Node<String,Integer> seqNode = edge.getOtherNode(root);
			if(edge.getLabel() == EncodeTable.TAG_EDGE){
				result += "TAG: "+(int)seqNode.getLabel().charAt(0)+"\n";
				continue;
			}
			Iterator<Edge<String,Integer>> edgeIt2 = seqNode.outgoingEdgeIterator();
			while(edgeIt2.hasNext()){
				edge = edgeIt2.next();
				if(edge.getLabel() == EncodeTable.SEQUENCE_ACTION_EDGE){
					result += "SPAWNER ";
				}else if(edge.getLabel() == EncodeTable.SEQUENCE_ACTION_OTHER_ENTITY_EDGE){
					result += "CHILD ";
				}
				result += stringToByteString(
						edge.getOtherNode(seqNode).getLabel()
						)+"\n";
			}
			if(edgeIt.hasNext()) result += "////\n";
		}
		return result.trim();
	}
	
	public static String nestingGraphToString(Graph<Integer,Integer> graph){
		Node<Integer,Integer> root = null;
		Iterator<Node<Integer,Integer>> nodeIt = graph.nodeIterator();
		while(nodeIt.hasNext()){
			root = nodeIt.next();
			if(root.getInDegree() == 0) break;
		}
		return nestingNodeToString(root, 0, 0).trim();
	}
	
	private static String nestingNodeToString(Node<Integer,Integer> root, int depth,
			int branchingIndex){
		String result = "";
		for(int i=1; i<=depth; i++) result += "-";
		if(depth > 0)
			result += " "+branchingIndex+": ";
		if(root.getLabel() >= EncodeTable.idOffset){
			result += GeneratorRegistry.getFunctionName(root.getLabel()-EncodeTable.idOffset);
		}else{
			result += GeneratorRegistry.getActionName(root.getLabel());
		}
		result += "\n";
		Iterator<Edge<Integer,Integer>> edgeIt = root.outgoingEdgeIterator();
		Edge<Integer,Integer> edge;
		while(edgeIt.hasNext()){
			edge = edgeIt.next();
			result += nestingNodeToString(edge.getOtherNode(root), depth+1, edge.getLabel());
		}
		return result;
	}
	
}
