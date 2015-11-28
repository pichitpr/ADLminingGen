package adl_2daa.gen.generator;

import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;

public class ASTSequenceSelection {
	
	protected Agent agent;
	protected State state;
	protected Sequence sequence;
	
	public ASTSequenceSelection(Agent agent, State state, Sequence sequence) {
		super();
		this.agent = agent;
		this.state = state;
		this.sequence = sequence;
	}
}
