package adl_2daa.gen.filter;

import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.statement.Action;
import adl_2daa.gen.generator.ASTNode;
import adl_2daa.gen.generator.ASTUtility;

@FunctionalInterface
public interface ASTNodeFilter {
	public boolean isValidNode(ASTNode node);
	
	/**
	 * A filter used to filter for EOB-T slot (at the end of ASTNode). 
	 * If tryMatching is used, the transition is tested against the last node in the
	 * container.
	 */
	public static ASTNodeFilter eobTransitionSlotFilter(Action eobTransition, boolean eosOnly,
			boolean tryMatching){
		return new ASTNodeFilter(){
			@Override
			public boolean isValidNode(ASTNode node) {
				if(node.isEndOfStatement()){
					if(eosOnly && node.getParent() != null) return false;
					if(node.getNodeContainer().isEmpty()) return true;
        			ASTStatement lastStatement = node.getNodeContainer().get(node.getNodeContainer().size()-1);
        			return ASTUtility.canPlaceEOBtransitionAfterLastStatement(
        					lastStatement, eobTransition, tryMatching);
        		}else{
        			return false;
        		}
			}
		};
	}
	
	/**
	 * A filter used to filter only transition (literally, either EOB-T or EOS-T)
	 */
	public static ASTNodeFilter existingTransition(boolean eosOnly){
		return new ASTNodeFilter(){
			@Override
			public boolean isValidNode(ASTNode node) {
				ASTStatement st = node.getNode();
				if(st == null) return false;
				if(eosOnly){
					return node.getParent() == null && ASTUtility.isKeyAction(st, true);
				}else{
					return ASTUtility.isKeyAction(st, true);
				}				
			}
		};
	}
}
