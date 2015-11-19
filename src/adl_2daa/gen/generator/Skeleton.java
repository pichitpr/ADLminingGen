package adl_2daa.gen.generator;

import java.util.LinkedList;
import java.util.List;

import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;
import adl_2daa.gen.encoder.ADLRoot;
import adl_2daa.gen.profile.AgentProfile;

public class Skeleton {

	private String identifier;
	private ADLRoot skel;
	
	public void generateInitialSkeleton(AgentProfile[] profiles){
		List<Agent> agents = new LinkedList<Agent>();
		identifier = profiles[0].getRootName();
		skel = new Root(agents);
		
		for(int i=0; i<profiles.length; i++){
			List<ASTStatement> init = new LinkedList<ASTStatement>();
			List<ASTStatement> des = new LinkedList<ASTStatement>();
			List<State> states = new LinkedList<State>();
			Agent agent = new Agent("agent"+i, new Sequence("init", init), 
					new Sequence("des", des), states);
			
			for(int j=0; j<profiles[i].getStructureInfo().length; j++){
				List<Sequence> sequences = new LinkedList<Sequence>();
				State state = new State("state"+j, sequences);
				int seqCount = profiles[i].getStructureInfo()[j];
				
				for(int k=0; k<seqCount; k++){
					sequences.add(new Sequence("seq"+k, new LinkedList<ASTStatement>()));
				}
				
				states.add(state);
			}
			
			agents.add(agent);
		}
	}
}
