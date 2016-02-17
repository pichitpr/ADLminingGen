package adl_2daa.gen.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import lcs.LCSSequence;
import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.statement.Condition;
import adl_2daa.ast.statement.Loop;
import adl_2daa.gen.filter.ASTNodeFilter;
import adl_2daa.gen.generator.ASTNode.NestingBlock;

/**
 * A wrapper for sequence, providing <br/>
 * - Slot index for statement insertion anywhere in the sequence <br/>
 * - Action sequence (nearly equivalent to encoded form of the sequence) <br/>
 * - Key action sequence for highest key action matching process <br/>
 */
public class ASTSequenceWrapper implements LCSSequence<ASTNode>{

	private List<ASTStatement> wrappedAST;
	private List<ASTNode> unfoldAST = new ArrayList<ASTNode>();
	private List<Integer> actionIndices = new ArrayList<Integer>();
	private List<Integer> keyActionIndices = new ArrayList<Integer>();
	private List<List<ASTStatement>> insertionList = new ArrayList<List<ASTStatement>>();
	
	/**
	 * Unfold given sequence and create 
	 * - index->AST map for slot index indication <br/>
	 * - an action sequence <br/>
	 * - a key action sequence to be used in key action matching
	 */
	public ASTSequenceWrapper(List<ASTStatement> astRoot){
		this.wrappedAST = astRoot;
		List<NestingBlock> nestingBlocks = new LinkedList<NestingBlock>();
		int mapSize = traverseAST(0, astRoot, null, nestingBlocks);
		assert(unfoldAST.size() > 0);
		assert(mapSize == unfoldAST.size());
		for(int i=0; i<mapSize; i++){
			insertionList.add(new LinkedList<ASTStatement>());
		}
	}
	
	/**
	 * Return AST root wrapped by this wrapper
	 */
	public List<ASTStatement> getWrappedAST(){
		return wrappedAST;
	}
	
	/**
	 * Traverse current AST level "statements" using depth-first strategy with the first
	 * statement index = "startIndex", the level parent = "parent", the nesting block = 
	 * "currentNestingBlocks". During traversal, gather ASTNode info and create
	 * action sequence, key action sequence.
	 */
	private int traverseAST(int startIndex, List<ASTStatement> statements, ASTNode parent,
			List<NestingBlock> currentNestingBlocks){
		ASTNode nodeInfo;
		int index = startIndex;
		int i=0; //local node index
		for(ASTStatement st : statements){
			nodeInfo = new ASTNode(i, statements, parent, currentNestingBlocks);
			unfoldAST.add(index, nodeInfo);
			if(st instanceof Action){
				actionIndices.add(index);
				if(ASTUtility.isKeyAction(st,false)){
					keyActionIndices.add(actionIndices.size()-1);
				}
			}
			index++;
			if(st instanceof Condition){
				Condition cond = (Condition)st;
				currentNestingBlocks.add(new NestingBlock(cond, false));
				index = traverseAST(index, cond.getIfblock(), nodeInfo,
						currentNestingBlocks);
				currentNestingBlocks.remove(currentNestingBlocks.size()-1);
				if(cond.getElseblock() != null){
					currentNestingBlocks.add(new NestingBlock(cond, true));
					index = traverseAST(index, cond.getElseblock(), nodeInfo,
						currentNestingBlocks);
					currentNestingBlocks.remove(currentNestingBlocks.size()-1);
				}
			}else if(st instanceof Loop){
				currentNestingBlocks.add(new NestingBlock(st, true));
				index = traverseAST(index, ((Loop)st).getContent(), nodeInfo,
						currentNestingBlocks);
				currentNestingBlocks.remove(currentNestingBlocks.size()-1);
			}
			i++;
		}
		//End of statements
		nodeInfo = new ASTNode(i, statements, parent, currentNestingBlocks);
		unfoldAST.add(index, nodeInfo);
		index++;
		return index;
	}
	
	/**
	 * Return X where [0...X] is available slot index that statement can be inserted into
	 */
	public int getSlotCount(){
		return unfoldAST.size();
	}
	
	/**
	 * Return a number of action in the sequence
	 */
	public int getActionCount(){
		return actionIndices.size();
	}
	
	/**
	 * Get valid slot index based on given filter from unfold AST
	 */
	public List<Integer> getValidSlots(ASTNodeFilter filter){
		List<Integer> result = new ArrayList<Integer>();
		
		for(int i=0; i<unfoldAST.size(); i++){
			if(filter.isValidNode(unfoldAST.get(i))){
				result.add(i);
			}
		}
		
		return result;
	}
	
	/**
	 * Return ASTNode at the given index. Using slot index referring to a slot before
	 * a statement will return that statement. The index should be retrieved from
	 * getValidSlots. MUST NOT call after finalizeWrapper().
	 */
	public ASTNode getUnfoldNode(int index){
		return unfoldAST.get(index);
	}
	
	/**
	 * Return slot index of the given statement (if found). Statement is compared 
	 * by checking reference. This method returns -1 if no statement found or null is passed.
	 */
	public int slotIndexOf(ASTStatement statement){
		if(statement == null) return -1;
		for(int i=0; i<unfoldAST.size(); i++){
			if(statement == unfoldAST.get(i).getNode())
				return i;
		}
		return -1;
	}
	
	/**
	 * Queue an insertion that will be performed on calling finalizeWrapper().
	 * The index should be retrieved from getValidSlots.<br/>
	 * An insertion of "statement" will be inserted before the node at the "index".
	 * Some nodes are special nodes indicating "end of block". Insertion at the "index"
	 * of those nodes are inserted at the end of block specified by the nodes.
	 */
	public void queueInsertion(int index, ASTStatement statement){		
		insertionList.get(index).add(statement);
	}
	
	/**
	 * Insert all queued insertions into the specified position. The insertion is performed
	 * so that, at the same index, the first statement appears before the others.<br/>
	 * No further method should be called after calling this method except getter method.
	 */
	public void finalizeWrapper(){
		//Adding in reverse order to prevent local index shifting
		for(int i=insertionList.size()-1; i>=0; i--){
			ASTNode insertingNode = unfoldAST.get(i);
			List<ASTStatement> queuedInsertions = insertionList.get(i);
			Collections.reverse(queuedInsertions);
			for(ASTStatement st : queuedInsertions){
				insertingNode.getNodeContainer().add(insertingNode.getLocalIndex(), st);
			}
		}
	}
	
	/**
	 * Convert key action sequence index to action index
	 */
	public int embIndexToActionIndex(int embIndex){
		return keyActionIndices.get(embIndex);
	}
	
	/**
	 * Convert key action sequence index to slot index.
	 */
	public int embIndexToSlotIndex(int embIndex){
		return actionIndices.get(embIndexToActionIndex(embIndex));
	}
	
	/**
	 * Return ASTNode at the specified key action sequence index. 
	 * The returned node is guaranteed to be Action and is key action
	 */
	@Override
	public ASTNode itemAt(int embIndex) {
		return unfoldAST.get(embIndexToSlotIndex(embIndex));
	}

	/**
	 * Return X where [0...X] is available key action sequence indices
	 */
	@Override
	public int size() {
		return keyActionIndices.size();
	}
	
	/**
	 * This comparator assumes that every ASTNodes passed to it is key action
	 */
	public static final Comparator<ASTNode> keyActionComparator = new Comparator<ASTNode>() {
		
		@Override
		public int compare(ASTNode o1, ASTNode o2) {
			if(o1 == o2) return 0;
			Action a1 = (Action)o1.getNode();
			Action a2 = (Action)o2.getNode();
			if(o1.getNestingBlocks().size() == o2.getNestingBlocks().size() && 
					ASTUtility.isKeyActionsMatched(a1, a2)){
				for(int i=0; i<o1.getNestingBlocks().size(); i++){
					//TODO: comparison depth will be changed
					if(!o1.getNestingBlocks().get(i).equals(o2.getNestingBlocks().get(i), 2)){
						return -1;
					}
				}
				return 0;
			}
			return -1;
		}
	};
	
	/**
	 * This comparator assumes that every ASTNodes passed to it is key action
	 */
	public static final Comparator<ASTNode> spawnComparator = new Comparator<ASTNode>() {
		
		@Override
		public int compare(ASTNode o1, ASTNode o2) {
			if(o1 == o2) return 0;
			Action a1 = (Action)o1.getNode();
			Action a2 = (Action)o2.getNode();
			if(o1.getNestingBlocks().size() == o2.getNestingBlocks().size() && 
					ASTUtility.isSpawnMatched(a1, a2)){
				//TODO: comparison depth will be changed
				for(int i=0; i<o1.getNestingBlocks().size(); i++){
					if(!o1.getNestingBlocks().get(i).equals(o2.getNestingBlocks().get(i), 2)){
						return -1;
					}
				}
				return 0;
			}
			return -1;
		}
	};
}
