package adl_2daa.gen.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.statement.Condition;
import adl_2daa.ast.statement.Loop;

public class ASTSlotManager {
	
	private TreeMap<Integer, ASTNode> map = new TreeMap<Integer, ASTNode>();
	private HashMap<Integer, List<ASTStatement>> insertionMap = 
			new HashMap<Integer, List<ASTStatement>>();
	
	/**
	 * Labeling each node in AST with unique "index" using depth-first strategy.
	 * This method must be called before using other available methods
	 */
	public void label(List<ASTStatement> rootStatements){
		map.clear();
		insertionMap.clear();
		generateLabelMap(0, rootStatements, null);
	}
	
	private int generateLabelMap(int startIndex, List<ASTStatement> statements, ASTNode parent){
		ASTNode nodeInfo;
		int index = startIndex;
		int i=0;
		for(ASTStatement st : statements){
			nodeInfo = new ASTNode(i, statements, parent);
			map.put(index, nodeInfo);
			index++;
			if(st instanceof Condition){
				Condition cond = (Condition)st;
				index = generateLabelMap(index, cond.getIfblock(), nodeInfo);
				if(cond.getElseblock() != null){
					index = generateLabelMap(index, cond.getElseblock(), nodeInfo);
				}
			}else if(st instanceof Loop){
				index = generateLabelMap(index, ((Loop)st).getContent(), nodeInfo);
			}
			i++;
		}
		//End of statements
		nodeInfo = new ASTNode(i, statements, parent);
		map.put(index, nodeInfo);
		index++;
		return index;
	}
	
	/**
	 * Get valid slot index based on given filter from labeled AST
	 */
	public List<Integer> getValidSlots(ASTFilter filter){
		List<Integer> result = new ArrayList<Integer>();
		
		for(Entry<Integer, ASTNode> entry : map.entrySet()){
			if(filter.isValidNode(entry.getValue())){
				result.add(entry.getKey());
			}
		}
		
		return result;
	}
	
	/**
	 * Queue ASTStatement insertion at the slotIndex
	 */
	public void insert(int slotIndex, ASTStatement statement){
		List<ASTStatement> stList = null;
		if(insertionMap.containsKey(slotIndex)){
			stList = insertionMap.get(slotIndex);
		}else{
			stList = new LinkedList<ASTStatement>();
			insertionMap.put(slotIndex, stList);
		}
		stList.add(statement);
	}
	
	/**
	 * Insert all queued insertions into the actual AST. The AST must be relabeled
	 * before further use after being finalized.
	 */
	public void finalize(){
		List<Integer> reversedIndexList = new LinkedList<Integer>(map.keySet());
		Collections.reverse(reversedIndexList);
		//Adding in reverse order to prevent local index shifting
		for(Integer slotIndex : reversedIndexList){
			List<ASTStatement> stList = insertionMap.get(slotIndex);
			if(stList != null){
				ASTNode node = map.get(slotIndex);
				Collections.reverse(stList);
				for(ASTStatement st : stList){
					node.nodeContainer.add(node.localIndex, st);
				}
			}
		}
	}
	
	public class ASTNode{
		private int localIndex;
		private List<ASTStatement> nodeContainer;
		private ASTNode parent;
		
		private ASTNode(int localIndex, List<ASTStatement> nodeContainer, ASTNode parent) {
			this.localIndex = localIndex;
			this.nodeContainer = nodeContainer;
			this.parent = parent;
		}

		public int getLocalIndex() {
			return localIndex;
		}

		public List<ASTStatement> getNodeContainer() {
			return nodeContainer;
		}

		public ASTNode getParent() {
			return parent;
		}
		
		public ASTStatement getNode(){
			if(isEndOfStatement()) return null;
			return nodeContainer.get(localIndex);
		}
		
		public boolean isEndOfStatement(){
			return localIndex == nodeContainer.size();
		}
	}
	
	@FunctionalInterface
	public interface ASTFilter{
		public boolean isValidNode(ASTNode node);
	}
}
