package adl_2daa.gen.generator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import adl_2daa.ast.ASTExpression;
import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.expression.ASTBinary;
import adl_2daa.ast.expression.ASTUnary;
import adl_2daa.ast.expression.And;
import adl_2daa.ast.expression.Arithmetic;
import adl_2daa.ast.expression.BooleanConstant;
import adl_2daa.ast.expression.Comparison;
import adl_2daa.ast.expression.FloatConstant;
import adl_2daa.ast.expression.Function;
import adl_2daa.ast.expression.Identifier;
import adl_2daa.ast.expression.IntConstant;
import adl_2daa.ast.expression.Or;
import adl_2daa.ast.expression.StringConstant;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;

public class ASTUtility {
	
	private static Random random = new Random(1000);
	
	public static void resetRandomizer(){
		random = new Random();
	}

	/**
	 * Return [start, end]
	 */
	public static int randomRange(int start, int end){
		//return start+random.nextInt(end-start+1);
		//return start+(new Random()).nextInt(end-start+1);
		return (int) ((random.nextLong() & Long.MAX_VALUE) % (1L + end - start)) + start;
	}
	
	public static boolean randomBool(){
		return randomRange(0,1) == 0;
	}
	
	public static float randomFloat(boolean posOnly){
		if(posOnly){
			return random.nextFloat() * Float.MAX_VALUE;
		}else{
			float sgn = randomBool() ? -1 : 1;
			return random.nextFloat() * Float.MAX_VALUE * sgn;
		}
	}
	
	public static float randomFloatFromZero(float bound){
		return random.nextFloat() * bound; 
	}
	
	/**
	 * Random 1 agent from root. If no agent found, create one
	 */
	public static Agent randomUniformAgentOrCreate(Root root){
		if(root.getRelatedAgents().isEmpty()){
			Agent agent = createEmptyAgent("main");
			root.getRelatedAgents().add(agent);
			return agent;
		}
		return randomUniform(root.getRelatedAgents());
	}
	
	/**
	 * Random 1 state from agent. If no state found, create one
	 */
	public static State randomUniformStateOrCreate(Agent agent){
		if(agent.getStates().isEmpty()){
			State state = createEmptyState("state0");
			agent.getStates().add(state);
			return state;
		}
		return randomUniform(agent.getStates());
	}
	
	/**
	 * Random 1 sequence. If no sequence found, create one
	 */
	public static Sequence randomUniformSequenceOrCreate(State state){
		if(state.getSequences().isEmpty()){
			Sequence sequence = createEmptySequence("seq0");
			state.getSequences().add(sequence);
			return sequence;
		}
		return randomUniform(state.getSequences());
	}
	
	/**
	 * Precondition: non-empty list
	 */
	public static <T> T randomUniform(Collection<T> collection){
		int index = randomRange(0, collection.size()-1);
		int i=0;
		for(T item : collection){
			if(i == index)
				return item;
			i++;
		}
		return null;
	}
	
	/**
	 * Shuffle a list using internal randomness
	 */
	public static <T> void shuffle(List<T> list){
		Collections.shuffle(list, random);
	}
	
	/**
	 * Return if the statement is key action or not
	 */
	public static boolean isKeyAction(ASTStatement st, boolean transitionOnly){
		if(!(st instanceof Action)) return false;
		String actionName = ((Action)st).getName();
		return actionName.equals("Goto") || actionName.equals("Despawn") || 
				(actionName.equals("Spawn") && !transitionOnly);
	}
	
	/**
	 * Return if 2 provided "key actions" matched or not.
	 */
	public static boolean isKeyActionsMatched(Action a1, Action a2){
		if(!a1.getName().equals(a2.getName())) return false;
		if(a1.getName().equals("Despawn")) return true;
		if(a1.getParams()[0] instanceof Identifier && 
				a2.getParams()[0] instanceof Identifier){
			//IDEN_X VS IDEN_X
			Identifier iden1 = (Identifier)a1.getParams()[0];
			Identifier iden2 = (Identifier)a2.getParams()[0];
			return iden1.getValue().equals(iden2.getValue());
		}
		//? VS any
		return true;
	}
	
	/**
	 * Return if 2 provided "key actions" are Spawn and matched or not.
	 * (More specific version of isKeyActionMatched)
	 */
	public static boolean isSpawnMatched(Action a1, Action a2){
		if(!a1.getName().equals("Spawn") || !a1.getName().equals(a2.getName())) return false;
		if(a1.getParams()[0] instanceof Identifier && 
				a2.getParams()[0] instanceof Identifier){
			//IDEN_? VS IDEN_?
			Identifier iden1 = (Identifier)a1.getParams()[0];
			Identifier iden2 = (Identifier)a2.getParams()[0];
			return iden1.getValue().equals(iden2.getValue());
		}
		//? VS any
		return true;
	}
	
	/**
	 * Return if given statement is eob-transition (transition without condition)
	 */
	public static boolean isEOBtransition(ASTStatement st){
		if(!(st instanceof Action)) return false;
		String actionName = ((Action)st).getName();
		return actionName.equals("Goto") || actionName.equals("Despawn");
	}
	
	/*
	public static boolean isStatementListEndWithEOBTransition(List<ASTStatement> stList){
		if(stList.size() == 0) return false;
		return isEOBtransition(stList.get(stList.size()-1));
	}
	*/
	
	/**
	 * Return true if provided EOB-Transition can be placed after (or match)
	 * the specified last statement of the block
	 */
	public static boolean canPlaceEOBtransitionAfterLastStatement(ASTStatement lastStatement,
			Action eobTransition, boolean tryMatching){
		if(!isEOBtransition(lastStatement)) return true;
		if(tryMatching){
			Action existingEOBtransition = (Action)lastStatement;
			return isKeyActionsMatched(existingEOBtransition, eobTransition);
		}else{
			return false;
		}
	}
	
	/**
	 * Remove all eob-transitions from given list and return the last one found.
	 * Return null if no eob-transition found. Due to nature of the behavior data,
	 * the list should contain no more than 1 eob-transition (more than 1 eob-transition
	 * means dead code, is strange and there must be something goes wrong!)
	 */
	public static Action removeAllEOBTransition(List<ASTStatement> list){
		Action removed = null;
		for(int i=list.size()-1; i>=0; i--){
			if(isEOBtransition(list.get(i))){
				removed = (Action)list.remove(i);
			}
		}
		return removed;
	}
	
	
	/**
	 * Filter valid agents from a given root based on given filters. 
	 */
	/*
	public static List<Agent> filterAgent(List<Agent> agentList, 
			BiPredicate<Agent, List<State>> agentFilter, 
			BiPredicate<State, List<Sequence>> stateFilter, 
			Predicate<Sequence> sequenceFilter){
		List<Agent> validAgents = new LinkedList<Agent>();
		for(Agent agent : agentList){
			List<State> filteredState = filterState(agent.getStates(), stateFilter, sequenceFilter);
			if(agentFilter != null && !agentFilter.test(agent, filteredState)) 
				continue;
			validAgents.add(agent);
		}
		return validAgents;
	}
	*/
	
	/**
	 * Filter valid states from a given agent based on given filters
	 */
	/*
	public static List<State> filterState(List<State> stateList, 
			BiPredicate<State, List<Sequence>> stateFilter, 
			Predicate<Sequence> sequenceFilter){
		List<State> validStates = new LinkedList<State>();
		for(State state : stateList){
			List<Sequence> filteredSequence = filterSequence(state.getSequences(), sequenceFilter);
			if(stateFilter != null && !stateFilter.test(state, filteredSequence))
				continue;
			validStates.add(state);
		}
		return validStates;
	}
	*/
	
	/**
	 * Filter valid sequences from a given state based on a given filter.
	 */
	/*
	public static List<Sequence> filterSequence(List<Sequence> sequenceList, 
			Predicate<Sequence> sequenceFilter){
		List<Sequence> validSequences = new LinkedList<Sequence>();
		for(Sequence sequence : sequenceList){
			if(sequenceFilter == null || sequenceFilter.test(sequence)){
				validSequences.add(sequence);
			}
		}
		return validSequences;
	}
	*/
	
	/**
	 * Random 1 agent which satisfies minimum requiredStateCount from root (uniform)
	 * If no agent satisfies the requirement, 1 agent will be randomly picked, then,
	 * states (with 1 empty sequence) will be generated and added to the agent until
	 * requirement satisfied<br/>
	 * Precondition: root with at least 1 agent
	 */
	public static Agent randomAgentAndGenEmptyState(Root root, int requiredStateCount){
		List<Integer> agentWithRequiredStateCount = new ArrayList<Integer>();
		int i=0;
		for(Agent agent : root.getRelatedAgents()){
			if(agent.getStates().size() >= requiredStateCount){
				agentWithRequiredStateCount.add(i);
			}
			i++;
		}
		if(agentWithRequiredStateCount.isEmpty()){
			Agent agent = randomUniform(root.getRelatedAgents());
			while(agent.getStates().size() < requiredStateCount){
				agent.getStates().add(createEmptyState(
						"state"+agent.getStates().size()
						));
			}
			return agent;
		}else{
			int index = randomRange(0, agentWithRequiredStateCount.size()-1);
			return root.getRelatedAgents().get(index);
		}
	}
	
	/**
	 * Create an agent named identifier with: empty init, enpty des,
	 * state0 with empty seq0 inside.
	 */
	public static Agent createEmptyAgent(String identifier){
		List<State> states = new LinkedList<State>();
		states.add(createEmptyState("state0"));
		Agent agent = new Agent(identifier, 
				createEmptySequence("init"), 
				createEmptySequence("des"), 
				states);
		return agent;
	}
	
	/**
	 * Create a state named identifier with empty seq0 inside
	 */
	public static State createEmptyState(String identifier){
		List<Sequence> sequences = new LinkedList<Sequence>();
		sequences.add(createEmptySequence("seq0"));
		State state = new State(identifier, sequences);
		return state;
	}
	
	/**
	 * Create an empty sequence named identifier
	 */
	public static Sequence createEmptySequence(String identifier){
		List<ASTStatement> statement = new LinkedList<ASTStatement>();
		Sequence seq = new Sequence(identifier, statement);
		return seq;
	}
	
	/**
	 * Create a copy of provided exp
	 */
	public static ASTExpression copy(ASTExpression exp){
		if(exp instanceof ASTUnary){
			ASTUnary unary = (ASTUnary)exp;
			return new ASTUnary(unary.op, copy(unary.node));
		}else if(exp instanceof ASTBinary){
			ASTBinary bin = (ASTBinary)exp;
			if(exp instanceof And)
				return new And(copy(bin.left), copy(bin.right));
			else if(exp instanceof Or)
				return new Or(copy(bin.left), copy(bin.right));
			else if(exp instanceof Comparison)
				return new Comparison(copy(bin.left), ((Comparison)exp).getOp(), copy(bin.right));
			else if(exp instanceof Arithmetic)
				return new Arithmetic(copy(bin.left), ((Arithmetic)exp).getOp(), copy(bin.right));
		}else if(exp instanceof Function){
			Function function = (Function)exp;
			List<ASTExpression> params = new ArrayList<ASTExpression>();
			for(ASTExpression arg : function.getParams()){
				params.add(copy(arg));
			}
			return new Function(function.getName(), params, function.hasSingleQuery());
		}else{
			if(exp instanceof BooleanConstant){
				return new BooleanConstant(""+((BooleanConstant)exp).isValue());
			}else if(exp instanceof FloatConstant){
				return new FloatConstant(""+((FloatConstant)exp).getValue());
			}else if(exp instanceof IntConstant){
				return new IntConstant(""+((IntConstant)exp).getValue());
			}else if(exp instanceof StringConstant){
				return new StringConstant(((StringConstant)exp).getValue());
			}else if(exp instanceof Identifier){
				return new Identifier("."+((Identifier)exp).getValue());
			}else if(exp instanceof ExpressionSkeleton){
				return new ExpressionSkeleton(((ExpressionSkeleton)exp).getType());
			}
		}
		
		//Impossible case
		System.out.println("Reach impossible case for ASTExpression copy");
		StringBuilder strb = new StringBuilder();
		exp.toScript(strb, 2);
		System.out.println(strb);
		return null; 
	}
}
