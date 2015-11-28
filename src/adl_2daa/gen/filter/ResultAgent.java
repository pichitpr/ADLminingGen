package adl_2daa.gen.filter;

import java.util.List;

import adl_2daa.ast.structure.Agent;

public class ResultAgent {
	
	private Agent actualAgent;
	private List<ResultState> resultStates;
	
	public ResultAgent(Agent actualAgent, List<ResultState> resultStates) {
		this.actualAgent = actualAgent;
		this.resultStates = resultStates;
	}

	public Agent getActualAgent() {
		return actualAgent;
	}

	public List<ResultState> getResultStates() {
		return resultStates;
	}
}
