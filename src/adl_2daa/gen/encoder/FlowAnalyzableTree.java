package adl_2daa.gen.encoder;

import java.util.ArrayList;
import java.util.List;

/**
 * A tree used to analyze flow from the first action to any terminal action
 * (state transition action). A copy of the actual AST containing only 
 * action and branching. Called FA-tree for short.
 */
public class FlowAnalyzableTree {

	protected FlowAnalyzableTree parent;
	protected int parentBranchingNodeIndex;
	protected List<TreeNode> content;
	
	protected FlowAnalyzableTree(){
		this.parent = null;
		this.parentBranchingNodeIndex = -1;
		this.content = new ArrayList<TreeNode>();
	}
	
	/**
	 * Add action node to FA-Tree. Used during tree construction ONLY.
	 */
	protected void addAction(String action){
		TreeEncodedActionNode node = new TreeEncodedActionNode();
		node.action = action;
		content.add(node);
	}
	
	/**
	 * Add branching node to FA-Tree. Used during tree construction ONLY.
	 */
	protected void addBranching(FlowAnalyzableTree ifBlock, FlowAnalyzableTree elseBlock){
		TreeBranchingNode node = new TreeBranchingNode();
		node.ifBlock = ifBlock;
		node.elseBlock = elseBlock;
		ifBlock.parent = this;
		ifBlock.parentBranchingNodeIndex = content.size();
		if(elseBlock != null){
			elseBlock.parent = this;
			elseBlock.parentBranchingNodeIndex = content.size();
		}
		content.add(node);
	}
	
	/**
	 * Append all actions and branchings of the current flow to the list "backwards".
	 * Also trim this sub-tree from branching node (as there is no other
	 * flow containing this sub-tree).
	 * @param list a list containing only the terminal action
	 */
	protected void createFlowAndTrim(List<String> list){
		addAllActionsTo(list, true);
		//Delete circular reference (this->parent, parent's branchingNode->this)
		if(parent != null){
			((TreeBranchingNode)parent.content.get(parentBranchingNodeIndex)).trimBranch(this);
			parent = null;
		}
	}
	
	private void addAllActionsTo(List<String> list, boolean recursiveParent){
		addAllActionsTo(list, content.size()-1, recursiveParent);
	}
	
	private void addAllActionsTo(List<String> list, int startingIndex,
			boolean recursiveParent){
		for(int i=startingIndex; i>=0; i--){
			content.get(i).addAllActionsTo(list);
		}
		if(recursiveParent && parent != null){
			parent.addAllActionsTo(list, parentBranchingNodeIndex-1, true);
		}
	}
	
	//=========================================================================
	
	private abstract class TreeNode{
		protected abstract void addAllActionsTo(List<String> list);
	}
	
	private class TreeEncodedActionNode extends TreeNode{
		protected String action = null;

		@Override
		protected void addAllActionsTo(List<String> list) {
			list.add(action);
		}
	}
	
	private class TreeBranchingNode extends TreeNode{
		protected FlowAnalyzableTree ifBlock = null, elseBlock = null;
		
		@Override
		protected void addAllActionsTo(List<String> list){
			if(elseBlock != null){
				elseBlock.addAllActionsTo(list, false);
			}
			if(ifBlock != null){
				ifBlock.addAllActionsTo(list, false);
			}
		}
		
		protected void trimBranch(FlowAnalyzableTree child){
			if(ifBlock == child){
				ifBlock = null;
			}
			if(elseBlock == child){
				elseBlock = null;
			}
		}
	}
}
