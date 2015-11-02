package adl_2daa.gen.generator;

import java.util.Random;

import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;

public class ASTUtility {

	private static Random rand = new Random(1000);
	
	/**
	 * Return [start, end]
	 */
	private static int randomRange(int start, int end){
		return start+rand.nextInt(end-start+1);
	}
	
	public static Agent randomUniformAgent(Root root){
		return root.getRelatedAgents().get(randomRange(0, root.getRelatedAgents().size()-1));
	}
	
	public static State randomUniformState(Agent agent){
		return agent.getStates().get(randomRange(0, agent.getStates().size()-1));
	}
	
	public static Sequence randomUniformSequence(State state){
		return state.getSequences().get(randomRange(0, state.getSequences().size()-1));
	}
	
	/**
	 * Randomly insert ASTStatement into the given sequence with [startIndex, endIndex]
	 * being possible index. endIndex can be sequence.size() which means "the end of sequence".
	 * The method returns insertion index
	 */
	public static int randomInsertStatementInto(Sequence sequence, 
			int startIndex, int endIndex, ASTStatement statement){
		int index = randomRange(startIndex, endIndex);
		sequence.getStatements().add(index, statement);
		return index;
	}
}
