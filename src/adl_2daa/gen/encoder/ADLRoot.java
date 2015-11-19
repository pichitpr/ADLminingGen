package adl_2daa.gen.encoder;

import java.util.LinkedList;
import java.util.List;

import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;

public class ADLRoot {

	protected List<ADLAgent> agents;
	
	public ADLRoot(Root astRoot){
		agents = new LinkedList<ADLAgent>();
		for(Agent agent : astRoot.getRelatedAgents()){
			agents.add(new ADLAgent(agent));
		}
	}
	
	public ADLAgent getAgentByIdentifier(String iden){
		for(ADLAgent agent : agents){
			if(agent.identifier.equals(iden)){
				return agent;
			}
		}
		return null;
	}
	
	//==============================
	
	public ADLRoot(List<ADLAgent> agents) {
		this.agents = agents;
	}

	public List<ADLAgent> getAgents() {
		return agents;
	}
}
