package adl_2daa.gen.mining0;

import java.util.ArrayList;
import java.util.List;

import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.State;
import adl_2daa.gen.signature.GeneratorRegistry;

/**
 * An encoded version of the specified Agent. All flow to transition can be generated
 * and store separately for each state. Creating this will take up extra memory
 */
public class EncodedAgent {

	protected String identifier;
	protected EncodedSequence des = null;
	protected List<EncodedState> states;
	
	protected EncodedAgent(Agent agent, boolean analyzeFlow){
		identifier = agent.getIdentifier();
		if(agent.getDes() != null){
			des = ADLSequenceEncoder.instance.parseAsEncodedSequence(agent.getDes(), false);
		}else{
			des = new EncodedSequence("des", new ArrayList<EncodedAction>());
		}
		//Since despawn can present alone without specifying .des and still have meaning
		//this will ensure that we can still capture "despawn without .des"
		if(des.eActList.size() == 0){
			des.eActList.add(new EncodedAction(
					(byte)GeneratorRegistry.dummyAction.getMainSignature().getId(), null, 0));
		}
		states = new ArrayList<EncodedState>();
		for(State st : agent.getStates()){
			states.add(new EncodedState(st, analyzeFlow));
		}
	}
	
	protected EncodedState getStateByIdentifier(String stateIdentifier){
		for(EncodedState eState : states){
			if(eState.identifier.equals(stateIdentifier)){
				return eState;
			}
		}
		return null;
	}
}
