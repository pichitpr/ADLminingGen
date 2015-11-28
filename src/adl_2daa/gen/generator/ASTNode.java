package adl_2daa.gen.generator;

import java.util.ArrayList;
import java.util.List;

import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.statement.Condition;

public class ASTNode {

	private int localIndex;
	private List<ASTStatement> nodeContainer;
	private ASTNode parent;
	private List<NestingBlock> nestingBlocks;
	
	public ASTNode(int localIndex, List<ASTStatement> nodeContainer, ASTNode parent,
			List<NestingBlock> nestingBlocks) {
		this.localIndex = localIndex;
		this.nodeContainer = nodeContainer;
		this.parent = parent;
		this.nestingBlocks = new ArrayList<NestingBlock>();
		for(NestingBlock nestingBlock : nestingBlocks){
			this.nestingBlocks.add(nestingBlock);
		}
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
	
	public List<NestingBlock> getNestingBlocks() {
		return nestingBlocks;
	}

	public ASTStatement getNode(){
		if(isEndOfStatement()) return null;
		return nodeContainer.get(localIndex);
	}
	
	public boolean isEndOfStatement(){
		return localIndex == nodeContainer.size();
	}
	
	public static class NestingBlock {
		public ASTStatement nestingBlock;
		public boolean underElseBlock = false;
		
		public NestingBlock(ASTStatement nestingBlock, boolean underElseBlock) {
			this.nestingBlock = nestingBlock;
			this.underElseBlock = underElseBlock;
		}

		@Override
		public boolean equals(Object o){
			if(!(o instanceof NestingBlock)) return false;
			NestingBlock other = (NestingBlock)o;
			if(nestingBlock instanceof Condition){
				return nestingBlock == other.nestingBlock && 
						underElseBlock == other.underElseBlock;
			}else{
				return nestingBlock == other.nestingBlock; 
			}
		}
	}
}
