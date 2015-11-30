package adl_2daa.gen.filter;

import java.util.List;
import java.util.function.Predicate;

import adl_2daa.ast.statement.Action;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.gen.generator.ASTSequenceWrapper;

public class ASTFilter {
	
	/*
	public static class EOStransitionSlotFilter implements Predicate<Sequence>{
		private Action eobTransition;
		private boolean tryMatching;
		
		public EOStransitionSlotFilter(Action eobTransition, boolean tryMatching){
			this.eobTransition = eobTransition;
			this.tryMatching = tryMatching;
		}
		
		@Override
		public boolean test(Sequence t) {
			if(t.getStatements().isEmpty()) return true;
			ASTStatement lastStatement = t.getStatements().get(t.getStatements().size()-1);
			return ASTUtility.canPlaceEOBtransitionAfterLastStatement(lastStatement, 
					eobTransition, tryMatching);
		}
	}
	*/
	
	public static class EOBtransitionSlotFilter implements Predicate<Sequence>{
		private Action eobTransition;
		private boolean testForEOSOnly;
		private boolean tryMatching;
		
		public EOBtransitionSlotFilter(Action eobTransition, boolean testForEOSOnly,
				boolean tryMatching){
			this.eobTransition = eobTransition;
			this.testForEOSOnly = testForEOSOnly;
			this.tryMatching = tryMatching;
		}

		@Override
		public boolean test(Sequence t) {
			if(t.getStatements().isEmpty()) return true;
			ASTSequenceWrapper wrappedSequence = new ASTSequenceWrapper(t.getStatements());
			List<Integer> eobSlots = wrappedSequence.getValidSlots(
					ASTNodeFilter.eobTransitionSlotFilter(eobTransition, testForEOSOnly, tryMatching)
					);
			return eobSlots.size() > 0;
		}
	}
}
