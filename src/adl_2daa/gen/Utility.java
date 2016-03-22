package adl_2daa.gen;

import java.util.Iterator;
import java.util.List;

import de.parsemis.graph.Graph;
import de.parsemis.graph.Node;

public class Utility {

	public static byte[] toByteArray(List<Byte> list){
		byte[] ary = new byte[list.size()];
		for(int i=0; i<ary.length; i++){
			ary[i] = list.get(i);
		}
		return ary;
	}
	
	public static <T> void addArrayToList(List<T> list, T[] ary){
		for(T element : ary){
			list.add(element);
		}
	}

	public static int createBitMask(int bitCount){
		bitCount--;
		if(bitCount <= 0) return 1;
		else return (1 << bitCount) | createBitMask(bitCount);
	}
	
	public static <N,E> Node<N,E> findFirstRoot(Graph<N,E> graph){
		Node<N,E> root = null;
		Iterator<Node<N,E>> nodeIt = graph.nodeIterator();
		while(nodeIt.hasNext()){
			root = nodeIt.next();
			if(root.getInDegree() == 0){
				break;
			}
		}
		return root;
	}
}
