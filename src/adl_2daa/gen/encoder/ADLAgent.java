package adl_2daa.gen.encoder;

import java.util.LinkedList;
import java.util.List;

import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.State;

public class ADLAgent {

	protected String identifier;
	protected ADLSequence init,des;
	protected List<ADLState> states;
	
	public ADLAgent(Agent astAgent){
		this.identifier = astAgent.getIdentifier();
		
		if(astAgent.getInit() != null){
			this.init = ADLSequenceEncoder.instance.encode(astAgent.getInit());
		}
		
		if(astAgent.getDes() != null){
			this.des = ADLSequenceEncoder.instance.encode(astAgent.getDes());
		}else{
			this.des = new ADLSequence("des", new LinkedList<String>());
		}
		if(this.des.encodedSequence.size() == 0){
			this.des.encodedSequence = ADLSequence.dummySequence;
		}
		
		states = new LinkedList<ADLState>();
		for(State astState : astAgent.getStates()){
			states.add(new ADLState(astState));
		}
	}
	
	public ADLState getStateByIdentifier(String iden){
		for(ADLState state : states){
			if(state.identifier.equals(iden)){
				return state;
			}
		}
		return null;
	}
}
