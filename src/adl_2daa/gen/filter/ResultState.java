package adl_2daa.gen.filter;

import java.util.List;

import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;

public class ResultState {
	private State actualState;
	private List<Sequence> resultSequences;
	
	public ResultState(State actualState, List<Sequence> resultSequences) {
		this.actualState = actualState;
		this.resultSequences = resultSequences;
	}

	public State getActualState() {
		return actualState;
	}

	public List<Sequence> getResultSequences() {
		return resultSequences;
	}
}
