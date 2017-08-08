package adl_2daa.gen.profile;

import java.util.List;

import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.statement.Condition;
import adl_2daa.ast.statement.Loop;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.State;

/**
 * Store profile of individual agent in the dataset
 */
public class AgentProfile {

	private int id;
	private String rootName;
	private String agentName;
	private boolean isComplexAgent; //NOTE:: Unused now
	private boolean isMainAgent;
	private int orderRelationUsage;
	private int interStateGotoRelationUsage;
	private int interStateDespawnRelationUsage;
	private int parallelRelationUsage;
	private int parallelInterEntityRelationUsage;
	private int nestingRelationUsage;
	private AgentProperties properties;
	
	//Structure info
	private boolean hasDes;
	private int[] structureInfo; //structureInfo[i] = #sequence of i-th state 
	
	//Additional info
	private int[] desActionUsageInfo; //[k] k: 0=action, 1=cond, 2=loop
	private int[][][] actionUsageInfo; //actionUsageInfo[i-th state][j-th seq][k] k: 0=action, 1=cond, 2=loop
	
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
	
	public AgentProperties getProperties() {
		return properties;
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
	
	public void setRelationUsage(int order, int gotoRel, int desRel, int parallel, int interEntity){
		this.orderRelationUsage = order;
		this.interStateGotoRelationUsage = gotoRel;
		this.interStateDespawnRelationUsage = desRel;
		this.parallelRelationUsage = parallel;
		this.parallelInterEntityRelationUsage = interEntity;
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
	
	public void setProperties(AgentProperties properties) {
		this.properties = properties;
	}

	public void createStructureProfile(Agent agent){
		this.agentName = agent.getIdentifier();
		this.hasDes = agent.getDes() != null;
		if(this.hasDes){
			desActionUsageInfo = countAction(agent.getDes().getStatements());
		}else{
			desActionUsageInfo = new int[]{0,0,0};
		}
		this.structureInfo = new int[agent.getStates().size()];
		this.actionUsageInfo = new int[agent.getStates().size()][][];
		for(int i=0; i<agent.getStates().size(); i++){
			State state = agent.getStates().get(i);
			this.structureInfo[i] = state.getSequences().size();
			this.actionUsageInfo[i] = new int[state.getSequences().size()][];
			for(int j=0; j<state.getSequences().size(); j++){
				this.actionUsageInfo[i][j] = countAction(state.getSequences().get(j).getStatements());
			}
		}
	}
	
	private int[] countAction(List<ASTStatement> seq){
		int[] counter = new int[3];
		for(ASTStatement st : seq){
			if(st instanceof Action){
				counter[0]++;
			}else if(st instanceof Condition){
				Condition cond = (Condition)st;
				counter[1]++;
				tupleAdd(counter, countAction(cond.getIfblock()));
				if(cond.getElseblock() != null){
					tupleAdd(counter, countAction(cond.getElseblock()));
				}
			}else{
				counter[2]++;
				tupleAdd(counter, countAction(((Loop)st).getContent()));
			}
		}
		return counter;
	}
	
	private void tupleAdd(int[] a, int[] b){
		for(int i=0; i<a.length; i++){
			a[i] += b[i];
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
	
	public int[] getDesActionUsage(){
		return desActionUsageInfo;
	}
	
	public void setDesActionUsage(int[] value){
		desActionUsageInfo = value;
	}
	
	public int[][][] getActionUsageInfo(){
		return actionUsageInfo;
	}
	
	public void setActionUsageInfo(int[][][] value){
		actionUsageInfo = value;
	}
	
	@Override
	public String toString(){
		StringBuilder strb = new StringBuilder();
		strb.append("ID:").append(id).append(" ").append(rootName).append(":").append(agentName);
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
	
	public String toRelationUsageCSV(){
		StringBuilder strb = new StringBuilder();
		strb.append("ID:").append(id).append(" ").append(rootName).append(":").append(agentName);
		strb.append(",");
		strb.append(orderRelationUsage).append(",").append(interStateGotoRelationUsage).append(",");
		strb.append(interStateDespawnRelationUsage).append(",").append(parallelRelationUsage).append(",");
		strb.append(parallelInterEntityRelationUsage).append(",").append(nestingRelationUsage);
		return strb.toString();
	}
}
