package adl_2daa.gen.generator;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import spmf.extension.algorithm.seqgen.SequentialPatternGen;
import spmf.extension.patterns.itemset_list_generic.ItemsetGen;
import adl_2daa.ast.ASTExpression;
import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.expression.Comparison;
import adl_2daa.ast.expression.Function;
import adl_2daa.ast.expression.IntConstant;
import adl_2daa.ast.expression.StringConstant;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.statement.Condition;
import adl_2daa.ast.statement.Loop;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;
import adl_2daa.gen.Miner;
import adl_2daa.gen.encoder.ADLSequenceDecoder;

/**
 * This class contains utility to improve generated result
 */
public class PostGenProcessor {

	/*
	 * Movement action:
	 * AddExtraVelocityToPlayer
	 * FloorStun
	 * Jump
	 * RunCircling
	 * RunHarmonic
	 * RunStraight
	 * RunTo
	 * Spawn
	 * Set("position", "this")
	 * Set(X, "player")
	 */
	private static String[] movementAction = {
		"AddExtraVelocityToPlayer", "FloorStun", "Jump", "RunCircling", "RunHarmonic", "RunStraight", "RunTo", "Spawn"
	};
	
	private static boolean isDynamicFilterWithTag(ASTExpression exp, String tag){
		if(!(exp instanceof Function)) return false;
		Function func = (Function)exp;
		if(!func.getName().equals("DynamicFilter")) return false;
		if(func.getParams().length == 0 || !(func.getParams()[0] instanceof StringConstant)) return false;
		String currentTag = ((StringConstant)func.getParams()[0]).getValue();
		return currentTag.equals(tag);
	}
	
	public static boolean isMovementAction(Action action){
		if(action.getName().equals("Set")){
			if(isDynamicFilterWithTag(action.getParams()[1], "player")){
				return true;
			}else{
				return ((StringConstant)action.getParams()[0]).getValue().equals("position");
			}
		}else{
			for(String str : movementAction){
				if(action.getName().equals(str)){
					return true;
				}
			}
			return false;
		}
	}
	
	private static boolean containMovementAction(List<ASTStatement> seq){
		for(ASTStatement st : seq){
			if(st instanceof Action){
				if(isMovementAction((Action)st)){
					return true;
				}
			}else if(st instanceof Condition){
				Condition cond = (Condition)st;
				if(cond.getElseblock() != null && containMovementAction(cond.getIfblock()) && containMovementAction(cond.getElseblock())){
					return true;
				}
			}else if(st instanceof Loop){
				if(containMovementAction(((Loop)st).getContent()) ){
					return true;
				}
			}
		}
		return false;
	}
	
	public static boolean isStaticEnemy(Skeleton skel){
		Root root = skel.getRoot();
		Agent mainAgent = root.getRelatedAgents().get(0);
		for(State state : mainAgent.getStates()){
			for(Sequence seq : state.getSequences()){
				if(containMovementAction(seq.getStatements())){
					return false;
				}
			}
		}
		return true;
	}
	
	public static void filterOutStaticRelation(Miner miner){
		//Only filter order
		miner.getFrequentOrder().removeIf(relation -> {
			List<String> eSeq = new LinkedList<String>();
			for(ItemsetGen<String> iset : relation.getItemsets()){
				eSeq.add(iset.get(0));
			}
			for(String eAct : eSeq){
				Action action = ADLSequenceDecoder.decodeActionOnly(eAct);
				if(PostGenProcessor.isMovementAction(action)){
					return false;
				}
			}
			return true;
		});
	}
	
	public static void improveSkeleton(Skeleton skel, Miner filteredMiner){
		List<SequentialPatternGen<String>> orderList = filteredMiner.getFrequentOrder();
		//Agent without sub agent must not add Spawn() to the script as Spawn() will be removed afterward due to
		//having no target anyway
		List<SequentialPatternGen<String>> orderListNoSpawn = null;
		if(skel.getRoot().getRelatedAgents().size() == 1){
			orderListNoSpawn = new LinkedList<SequentialPatternGen<String>>(orderList);
			orderListNoSpawn.removeIf(relation -> {
				List<String> eSeq = new LinkedList<String>();
				for(ItemsetGen<String> iset : relation.getItemsets()){
					eSeq.add(iset.get(0));
				}
				for(String eAct : eSeq){
					Action action = ADLSequenceDecoder.decodeActionOnly(eAct);
					if(action.getName().equals("Spawn")){
						return true;
					}
				}
				return false;
			});
		}
		int additionalOrderUsage = 0;
		while(additionalOrderUsage == 0){
			additionalOrderUsage = filteredMiner.generateAgentProfile()[0].getOrderRelationUsage();
		}
		for(int i=1; i<=additionalOrderUsage; i++){
			SequentialPatternGen<String> relation = ASTUtility.randomUniform(orderListNoSpawn == null ? orderList : orderListNoSpawn);
			skel.mergeOrder(relation);
			/*
			List<String> eSeq = new LinkedList<String>();
			for(ItemsetGen<String> iset : relation.getItemsets()){
				eSeq.add(iset.get(0));
			}
			for(String eAct : eSeq){
				Action action = ADLSequenceDecoder.decodeActionOnly(eAct);
				System.out.println(action.getName());
			}
			System.out.println("-=-=-=-=-=-=-");
			*/
		}
		skel.mergeNesting(filteredMiner.getFrequentNesting());
		skel.finalizeSkeleton();
		skel.reduceWait();
	}
	
	/*
	 * Only count long duration action (some span action only last 1 frame e.g. Despawn, Goto).
	 * Also not counting Wait
	 */
	private static String[] spanActionForWaitImprovement = {
		"AddExtraVelocityToPlayer", "ChangeDirectionToPlayerByStep", "Jump", "RunCircling", "RunHarmonic", "RunStraight", "RunTo"
	};
	
	private static boolean isSpanActionForWaitImprovement(Action action){
		for(String str : spanActionForWaitImprovement){
			if(action.getName().equals(str)){
				return true;
			}
		}
		return false;
	}
	
	private static void fixWaitDelay(ASTExpression exp, float multiplier){
		if(!(exp instanceof Comparison))
			return;
		Comparison cmp = (Comparison)exp;
		if(cmp.getOp() != Comparison.Comp.GE && cmp.getOp() != Comparison.Comp.GT)
			return;
		if(!(cmp.left instanceof Function) || !((Function)cmp.left).getName().equals("TimePass") || 
				!(cmp.right instanceof IntConstant))
			return;
		int newDelay = (int)( ((IntConstant)cmp.right).getValue()*multiplier);
		if(newDelay <= 0){
			newDelay = 1;
		}
		cmp.right = new IntConstant(""+ newDelay );
	}
	
	private static boolean findImprovementRequiredWait(List<ASTStatement> seq, boolean hasPrecedingSpawn, 
			HashSet<Action> improvementRequired){
		for(ASTStatement st : seq){
			if(st instanceof Action){
				String actionName = ((Action)st).getName();
				if(actionName.equals("Wait")){
					if(hasPrecedingSpawn){
						improvementRequired.add((Action)st);
					}
				}else if(actionName.equals("Spawn")){
					hasPrecedingSpawn = true;
				}else if(actionName.equals("Goto") || actionName.equals("Despawn") || 
						isSpanActionForWaitImprovement((Action)st) ){
					hasPrecedingSpawn = false;
				}
			}else if(st instanceof Condition){
				Condition cond = (Condition)st;
				boolean precedingSpawnIf = findImprovementRequiredWait(cond.getIfblock(), hasPrecedingSpawn, improvementRequired);
				boolean precedingSpawnElse = false;
				if(cond.getElseblock() != null){
					precedingSpawnElse = findImprovementRequiredWait(cond.getElseblock(), hasPrecedingSpawn, improvementRequired);
				}
				hasPrecedingSpawn = precedingSpawnIf || precedingSpawnElse;
			}else if(st instanceof Loop){
				//May require 2 passes improvement as first pass may not cover Wait() that comes before Spawn()
				//when there is no preceding Spawn() before the loop
				boolean require2ndPass = !hasPrecedingSpawn;
				hasPrecedingSpawn = findImprovementRequiredWait(((Loop)st).getContent(), hasPrecedingSpawn, improvementRequired);
				if(require2ndPass)
					findImprovementRequiredWait(((Loop)st).getContent(), hasPrecedingSpawn, improvementRequired);
			}
		}
		return hasPrecedingSpawn;
	}
	
	private static void improveWaitDelayInSequence(Sequence seq, float multiplier, boolean requireSecondPass){
		HashSet<Action> improvementRequiredWait = new HashSet<Action>();
		boolean endWithSpawn = findImprovementRequiredWait(seq.getStatements(), false, improvementRequiredWait);
		if(requireSecondPass && endWithSpawn){
			findImprovementRequiredWait(seq.getStatements(), true, improvementRequiredWait);
		}
		for(Action wait : improvementRequiredWait){
			fixWaitDelay(wait.getParams()[0], multiplier);
		}
	}
	
	public static void improveWaitDelay(Root root, float multiplier){
		for(Agent agent : root.getRelatedAgents()){
			if(agent.getInit() != null){
				improveWaitDelayInSequence(agent.getInit(), multiplier, false);
			}
			if(agent.getDes() != null){
				improveWaitDelayInSequence(agent.getDes(), multiplier, false);
			}
			for(State state : agent.getStates()){
				for(Sequence seq : state.getSequences()){
					//Sequence may require 2 passes as it is literally a loop with no preceding Spawn()
					improveWaitDelayInSequence(seq, multiplier, true);
				}
			}
		}
	}
	
	/*
	//Return true if the next statement after this block is considered preceded by Spawn()
		private static boolean improveWaitDelay(List<ASTStatement> seq, boolean hasPrecedingSpawn, float multiplier,
				boolean isSecondPass){
			for(ASTStatement st : seq){
				if(st instanceof Action){
					String actionName = ((Action)st).getName();
					if(actionName.equals("Wait")){
						if(hasPrecedingSpawn){
							fixWaitDelay(((Action)st).getParams()[0], multiplier);
						}
					}else if(actionName.equals("Spawn")){
						hasPrecedingSpawn = true;
					}else if(actionName.equals("Goto") || actionName.equals("Despawn") || 
							isSpanActionForWaitImprovement((Action)st) ){
						hasPrecedingSpawn = false;
					}
				}else if(st instanceof Condition){
					Condition cond = (Condition)st;
					boolean precedingSpawnIf = improveWaitDelay(cond.getIfblock(), hasPrecedingSpawn, multiplier, false);
					boolean precedingSpawnElse = false;
					if(cond.getElseblock() != null){
						precedingSpawnElse = improveWaitDelay(cond.getElseblock(), hasPrecedingSpawn, multiplier, false);
					}
					hasPrecedingSpawn = precedingSpawnIf || precedingSpawnElse;
				}else if(st instanceof Loop){
					//May require 2 passes improvement as first pass may not cover Wait() that comes before Spawn()
					//when there is no preceding Spawn() before the loop
					boolean require2ndPass = !hasPrecedingSpawn;
					hasPrecedingSpawn = improveWaitDelay(((Loop)st).getContent(), hasPrecedingSpawn, multiplier, false);
					if(require2ndPass)
						improveWaitDelay(((Loop)st).getContent(), hasPrecedingSpawn, multiplier,true);
				}
			}
			return hasPrecedingSpawn;
		}
		
		public static void improveWaitDelay(Root root, float multiplier, boolean isSecondPass){
			for(Agent agent : root.getRelatedAgents()){
				if(agent.getInit() != null){
					improveWaitDelay(agent.getInit().getStatements(), false, multiplier, false);
				}
				if(agent.getDes() != null){
					improveWaitDelay(agent.getDes().getStatements(), false, multiplier, false);
				}
				for(State state : agent.getStates()){
					for(Sequence seq : state.getSequences()){
						//Sequence may require 2 passes as it is literally a loop with no preceding Spawn()
						boolean endWithSpawn = improveWaitDelay(seq.getStatements(), false, multiplier, false);
						if(endWithSpawn){
							improveWaitDelay(seq.getStatements(), true, multiplier, true);
						}
					}
				}
			}
		}
		*/
}
