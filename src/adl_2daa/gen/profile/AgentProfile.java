package adl_2daa.gen.profile;

public class AgentProfile {

	private int id;
	private String rootName;
	private boolean isComplexAgent; //TODO: Unused now
	private boolean isMainAgent;
	private int orderRelationUsage;
	private int interStateGotoRelationUsage;
	private int interStateDespawnRelationUsage;
	private int parallelRelationUsage;
	private int parallelInterEntityRelationUsage;
	private int nestingRelationUsage;
	
	public int getId() {
		return id;
	}
	
	public String getRootName() {
		return rootName;
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
	
	
}
