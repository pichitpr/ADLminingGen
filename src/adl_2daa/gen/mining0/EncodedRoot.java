package adl_2daa.gen.mining0;

import java.util.LinkedList;
import java.util.List;

import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;

public class EncodedRoot {

	protected String agentFile;
	protected List<EncodedAgent> relatedAgents;
	
	protected EncodedRoot(Root astRoot){
		relatedAgents = new LinkedList<EncodedAgent>();
		for(Agent agent : astRoot.getRelatedAgents()){
			relatedAgents.add(new EncodedAgent(agent, false));
		}
	}
	
	protected EncodedAgent getAgentByIdentifier(String agentIdentifier){
		for(EncodedAgent eAgent : relatedAgents){
			if(eAgent.identifier.equals(agentIdentifier)){
				return eAgent;
			}
		}
		return null;
	}
}
