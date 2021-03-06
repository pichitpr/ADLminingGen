package adl_2daa.gen.encoder;

import java.util.LinkedList;
import java.util.List;

import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Sequence;
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
			this.des.encodedSequence = ADLSequenceEncoder.dummyEncodedSequence;
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

	//======================================
	
	public ADLAgent(String identifier, ADLSequence init, ADLSequence des,
			List<ADLState> states) {
		this.identifier = identifier;
		this.init = init;
		this.des = des;
		this.states = states;
	}

	public String getIdentifier() {
		return identifier;
	}

	public ADLSequence getInit() {
		return init;
	}

	public ADLSequence getDes() {
		return des;
	}

	public List<ADLState> getStates() {
		return states;
	}
	
	public Agent toAgent(){
		Sequence initSeq, desSeq;
		if(init == null){
			initSeq = new Sequence("init", new LinkedList<ASTStatement>());
		}else{
			initSeq = init.toSequence();
		}
		if(des.encodedSequence == ADLSequenceEncoder.dummyEncodedSequence){
			desSeq = new Sequence("des", new LinkedList<ASTStatement>());
		}else{
			desSeq = des.toSequence();
		}
		List<State> stateList = new LinkedList<State>();
		for(ADLState eState : states){
			stateList.add(eState.toState());
		}
		return new Agent(identifier, initSeq, desSeq, stateList);
	}
}
