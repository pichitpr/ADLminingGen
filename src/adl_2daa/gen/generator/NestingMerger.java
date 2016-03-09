package adl_2daa.gen.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import parsemis.extension.GraphPattern;
import adl_2daa.ast.ASTExpression;
import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.Reversible;
import adl_2daa.ast.expression.ASTUnary;
import adl_2daa.ast.expression.ASTUnary.UnaryOp;
import adl_2daa.ast.expression.And;
import adl_2daa.ast.expression.Arithmetic;
import adl_2daa.ast.expression.Arithmetic.MathOp;
import adl_2daa.ast.expression.Comparison;
import adl_2daa.ast.expression.Comparison.Comp;
import adl_2daa.ast.expression.Function;
import adl_2daa.ast.expression.Or;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.statement.Condition;
import adl_2daa.ast.statement.Loop;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;
import adl_2daa.gen.encoder.ADLNestingDecoder;
import adl_2daa.gen.encoder.NestingLiteralCollectionExp;

public class NestingMerger {

	public static NestingMerger instance = new NestingMerger();
	
	private List<And> andUsage; 
	private List<Or> orUsage;
	private List<ASTUnary> negUsage, notUsage;
	private List<Comparison>[] compUsage; 
	private List<Arithmetic>[] arithUsage;
	private HashMap<String, List<Action>> actionUsage;
	private HashMap<String, List<Function>> functionUsage;
	
	public void merge(Root root, List<GraphPattern<Integer,Integer>> relation){
		generateUsageMap(relation);
		fill(root);
	}
	
	public void decodeAndDumpRelation(List<GraphPattern<Integer,Integer>> relation, StringBuilder strb){
		for(GraphPattern<Integer,Integer> graph : relation){
			Reversible reversible = (Reversible)ADLNestingDecoder.instance.decode(graph.getGraph());
			reversible.toScript(strb, 0);
			strb.append('\n').append('\n');
		}
	}
	
	@SuppressWarnings("unchecked")
	private void generateUsageMap(List<GraphPattern<Integer,Integer>> relation){
		//Subgraph is not explored. The input assumes that all subgraphs are explored
		//(mining non-closed pattern)
		andUsage = new ArrayList<And>();
		orUsage = new ArrayList<Or>();
		negUsage = new ArrayList<ASTUnary>();
		notUsage = new ArrayList<ASTUnary>();
		compUsage = new List[Comp.values().length];
		arithUsage = new List[MathOp.values().length];
		actionUsage = new HashMap<String, List<Action>>();
		functionUsage = new HashMap<String, List<Function>>();
		
		for(GraphPattern<Integer,Integer> graph : relation){
			Object obj = ADLNestingDecoder.instance.decode(graph.getGraph());
			if(obj instanceof Action){
				Action action = (Action)obj;
				if(!actionUsage.containsKey(action.getName())){
					actionUsage.put(action.getName(), new ArrayList<Action>());
				}
				actionUsage.get(action.getName()).add(action);
			}else if(obj instanceof Function){
				Function function = (Function)obj;
				if(!functionUsage.containsKey(function.getName())){
					functionUsage.put(function.getName(), new ArrayList<Function>());
				}
				functionUsage.get(function.getName()).add(function);
			}else if(obj instanceof And){
				andUsage.add((And)obj);
			}else if(obj instanceof Or){
				orUsage.add((Or)obj);
			}else if(obj instanceof Comparison){
				Comparison comp = (Comparison)obj;
				if(compUsage[comp.getOp().ordinal()] == null){
					compUsage[comp.getOp().ordinal()] = new ArrayList<Comparison>();
				}
				compUsage[comp.getOp().ordinal()].add(comp);
			}else if(obj instanceof Arithmetic){
				Arithmetic arith = (Arithmetic)obj;
				if(arithUsage[arith.getOp().ordinal()] == null){
					arithUsage[arith.getOp().ordinal()] = new ArrayList<Arithmetic>();
				}
				arithUsage[arith.getOp().ordinal()].add(arith);
			}else if(obj instanceof ASTUnary){
				ASTUnary unary = (ASTUnary)obj;
				if(unary.op == UnaryOp.NEG){
					negUsage.add(unary);
				}else{
					notUsage.add(unary);
				}
			}
		}
	}
	
	private void fill(Root root){
		for(Agent agent : root.getRelatedAgents()){
			if(agent.getInit() != null) fillStatementBlock(agent.getInit().getStatements());
			if(agent.getDes() != null) fillStatementBlock(agent.getDes().getStatements());
			for(State state : agent.getStates()){
				for(Sequence seq : state.getSequences()){
					fillStatementBlock(seq.getStatements());
				}
			}
		}
	}
	
	private void fillStatementBlock(List<ASTStatement> block){
		//NOTE:: no type checking for cond/loop since generator does not generate empty expression
		for(ASTStatement st : block){
			if(st instanceof Action){
				Action action = (Action)st;
				if(actionUsage.containsKey(action.getName())){
					Action template = ASTUtility.randomUniform(actionUsage.get(action.getName()) );
					fillParameters(action.getParams(), template.getParams());
				}
			}else if(st instanceof Condition){
				Condition cond = (Condition)st;
				fillExpression(cond.getCondition());
				fillStatementBlock(cond.getIfblock());
				if(cond.getElseblock() != null){
					fillStatementBlock(cond.getElseblock());
				}
			}else{
				Loop loop = (Loop)st;
				fillExpression(loop.getLoopCount());
				fillStatementBlock(loop.getContent());
			}
		}
	}
	
	private void fillExpression(ASTExpression exp){
		//TODO: Finish nest merge
		if(exp instanceof And){
			
		}else if(exp instanceof Or){
			
		}else if(exp instanceof Comparison){
			
		}else if(exp instanceof Arithmetic){
			
		}else if(exp instanceof ASTUnary){
			
		}else if(exp instanceof Function){
			
		}
		
		//Do nothing for literal
	}
	
	private void fillParameters(ASTExpression[] params, ASTExpression[] template){
		int fillingLength = (params.length > template.length) ? template.length : params.length;
		for(int i=0; i<fillingLength; i++){
			if(params[i] instanceof ExpressionSkeleton){
				if(template[i] instanceof NestingLiteralCollectionExp){
					@SuppressWarnings("rawtypes")
					NestingLiteralCollectionExp exp = (NestingLiteralCollectionExp)template[i];
					params[i] = exp.getAsExpression(ASTUtility.randomRange(0, exp.size()-1));
				}else{
					params[i] = ASTUtility.copy(template[i]);
				}
			}
		}
	}
}
