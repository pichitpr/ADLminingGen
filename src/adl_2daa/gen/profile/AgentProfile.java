package adl_2daa.gen.profile;

import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.State;

/**
 * Store profile of individual agent in the dataset
 */
public class AgentProfile {

	private int id;
	private String rootName;
	private String agentName;
	private boolean isComplexAgent; //TODO: Unused now
	private boolean isMainAgent;
	private int orderRelationUsage;
	private int interStateGotoRelationUsage;
	private int interStateDespawnRelationUsage;
	private int parallelRelationUsage;
	private int parallelInterEntityRelationUsage;
	private int nestingRelationUsage;
	
	//Structure info
	private boolean hasDes;
	private int[] structureInfo; //structureInfo[i] = #sequence of i-th state 
	
	public int getId() {
		return id;
	}
	
	public String getRootName() {
		return rootName;
	}
	
	public String getAgentName() {
		return agentName;
	}
	
	public boolean isComplexAgent() {
		return isComplexAgent;
	}
	
	public boolean isMainAgent() {
		return isMainAgent;
	}
	
	public int getOrderRelationUsage() {
		return orderRelationUsage;
	}
	
	public int getInterStateGotoRelationUsage() {
		return interStateGotoRelationUsage;
	}
	
	public int getInterStateDespawnRelationUsage() {
		return interStateDespawnRelationUsage;
	}
	
	public int getParallelRelationUsage() {
		return parallelRelationUsage;
	}
	
	public int getParallelInterEntityRelationUsage() {
		return parallelInterEntityRelationUsage;
	}
	
	public int getNestingRelationUsage() {
		return nestingRelationUsage;
	}
	
	public void setId(int id) {
		this.id = id;
	}

	public void setRootName(String rootName) {
		this.rootName = rootName;
	}
	
	public void setComplexAgent(boolean isComplexAgent) {
		this.isComplexAgent = isComplexAgent;
	}

	public void setMainAgent(boolean isMainAgent) {
		this.isMainAgent = isMainAgent;
	}

	public void addOrderRelationUsage() {
		this.orderRelationUsage++;
	}

	public void addInterStateGotoRelationUsage() {
		this.interStateGotoRelationUsage++;
	}

	public void addInterStateDespawnRelationUsage() {
		this.interStateDespawnRelationUsage++;
	}

	public void addParallelRelationUsage() {
		this.parallelRelationUsage++;
	}

	public void addInterEntityParallelRelationUsage() {
		this.parallelInterEntityRelationUsage++;
	}

	public void addNestingRelationUsage() {
		this.nestingRelationUsage++;
	}
	
	public void createStructureProfile(Agent agent){
		this.agentName = agent.getIdentifier();
		this.hasDes = agent.getDes() != null;
		this.structureInfo = new int[agent.getStates().size()];
		for(int i=0; i<agent.getStates().size(); i++){
			State state = agent.getStates().get(i);
			this.structureInfo[i] = state.getSequences().size();
		}
	}
	
	public void setStructureInfo(boolean hasDes, int[] info){
		this.hasDes = hasDes;
		this.structureInfo = info;
	}

	public boolean hasDes() {
		return hasDes;
	}

	public int[] getStructureInfo() {
		return structureInfo;
	}
	
	@Override
	public String toString(){
		StringBuilder strb = new StringBuilder();
		strb.append("ID:").append(id).append(" ").append(rootName).append(".").append(agentName);
		if(isMainAgent) strb.append(" [MAIN]");
		strb.append(" [INIT]");
		if(hasDes) strb.append(" [DES]");
		for(int info : structureInfo){
			strb.append(" ").append(info);
		}
		strb.append("\r\n");
		strb.append("Order relation count:").append(orderRelationUsage).append("\r\n");
		strb.append("InterState(Goto) relation count:").append(interStateGotoRelationUsage).append("\r\n");
		strb.append("InterState(Des) relation count:").append(interStateDespawnRelationUsage).append("\r\n");
		strb.append("Parallel relation count:").append(parallelRelationUsage).append("\r\n");
		strb.append("InterEntity relation count:").append(parallelInterEntityRelationUsage).append("\r\n");
		strb.append("Nesting relation count:").append(nestingRelationUsage).append("\r\n");
		return strb.toString();
	}
}
