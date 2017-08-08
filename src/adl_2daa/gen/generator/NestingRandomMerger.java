package adl_2daa.gen.generator;

import java.util.LinkedList;
import java.util.List;

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
import adl_2daa.ast.statement.Condition;
import adl_2daa.ast.statement.Loop;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;
import adl_2daa.gen.signature.Datatype;
import adl_2daa.gen.signature.GeneratorRegistry;
import adl_2daa.gen.signature.Signature;

public class NestingRandomMerger {

	public static NestingRandomMerger instance = new NestingRandomMerger();
	
	private Root root;
	private int expressionDepthLimit;
	
	//Special flag for filling identifier
	private Agent fillingAgent;
	private boolean spawnFill;
	
	public void merge(Root root, int expressionDepthLimit){
		this.root = root;
		this.expressionDepthLimit = expressionDepthLimit;
		fill(root);
	}
	
	private void fill(Root root){
		for(Agent agent : root.getRelatedAgents()){
			fillingAgent = agent;
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
		for(int b=0; b<block.size(); b++){
			ASTStatement st = block.get(b);
			if(st instanceof Action){
				Action action = (Action)st;
				spawnFill = action.getName().equals("Spawn");
				for(int i=0; i<action.getParams().length; i++){
					action.getParams()[i] = fillExpression(action.getParams()[i], 0);
				}
			}else if(st instanceof Condition){
				Condition cond = (Condition)st;
				ASTExpression newExp = fillExpression(cond.getCondition(),0);
				fillStatementBlock(cond.getIfblock());
				if(cond.getElseblock() != null){
					fillStatementBlock(cond.getElseblock());
				}
				block.set(b, new Condition(newExp, cond.getIfblock(), cond.getElseblock()));
			}else{
				Loop loop = (Loop)st;
				ASTExpression newExp = fillExpression(loop.getLoopCount(),0);
				fillStatementBlock(loop.getContent());
				block.set(b, new Loop(newExp, loop.getContent()));
			}
		}
	}
	
	private ASTExpression fillExpression(ASTExpression target, int depth){
		if(!(target instanceof ExpressionSkeleton)){
			return target;
		}
		ExpressionSkeleton exp = (ExpressionSkeleton)target;
		ASTExpression addingExp = randomExpression(exp.getType(), depth >= expressionDepthLimit);
		if(addingExp instanceof ASTUnary){
			ASTUnary unary = (ASTUnary)addingExp;
			unary.node = fillExpression(unary.node, depth+1);
		}else if(addingExp instanceof ASTBinary){
			ASTBinary binary = (ASTBinary)addingExp;
			binary.left = fillExpression(binary.left, depth+1);
			binary.right = fillExpression(binary.right, depth+1);
		}else if(addingExp instanceof Function){
			Function func = (Function)addingExp;
			//Special "Random"
			if(func.getName().startsWith("_Random")){
				List<ASTExpression> params = new LinkedList<ASTExpression>();
				for(ASTExpression arg : func.getParams()){
					params.add(arg);
				}
				addingExp = new Function("Random", params, func.hasSingleQuery());
				func = (Function)addingExp;
			}
			for(int i=0; i<func.getParams().length; i++){
				func.getParams()[i] = fillExpression(func.getParams()[i], depth+1);
			}
		}
		//If it is literal, just return
		return addingExp;
	}
	
	private ASTExpression randomExpression(Datatype datatype, boolean prioritizeLiteral){
		switch(datatype){
		case ABSTRACT:
		case ABSTRACT_SET:
			//Impossible case, will be converted to typed data
			return null;
		case BOOL:
			return randomBool(prioritizeLiteral);
		case CHOICE:
			//Impossible case, but we will just handle it
			return new StringConstant("choice");
		case COLLIDER:
			return randomCollider(prioritizeLiteral);
		case DECIMAL:
			return randomDecimal(prioritizeLiteral);
		case DECIMAL_SET:
			return randomDecimalSet();
		case DIRECTION:
			return randomDirection(prioritizeLiteral);
		case DIRECTION_SET:
			return randomDirectionSet();
		case DYNAMIC:
			return randomDynamic();
		case DYNAMIC_SET:
			//Impossible case, we already removed child/parent properties
			return null;
		case IDENTIFIER:
			return randomIdentifier();
		case INT:
			return randomInt(prioritizeLiteral);
		case POSITION:
			return randomPosition(prioritizeLiteral);
		case VOID:
			break;
		}
		return null;
	}
	
	//Need special case for handling Comp/L_and/L_or/U_not
	private ASTExpression randomBool(boolean prioritizeLiteral){
		if(prioritizeLiteral || ASTUtility.randomBool()){
			return new BooleanConstant(ASTUtility.randomBool() ? "true" : "false");
		}else{
			Function randomFunction = getRandomFunctionOfType(Datatype.BOOL);
			if(randomFunction == null){
				return new BooleanConstant(ASTUtility.randomBool() ? "true" : "false");
			}else{
				String name = randomFunction.getName();
				
				if(name.startsWith("@")){
					//Special case
					 if(name.equals("@L_and")){
						return new And(new ExpressionSkeleton(Datatype.BOOL), new ExpressionSkeleton(Datatype.BOOL));
					}else if(name.equals("@L_or")){
						return new Or(new ExpressionSkeleton(Datatype.BOOL), new ExpressionSkeleton(Datatype.BOOL));
					}else if(name.equals("@C_eq")){
						if(ASTUtility.randomBool()){
							return new Comparison(new ExpressionSkeleton(Datatype.BOOL), Comparison.Comp.EQ, new ExpressionSkeleton(Datatype.BOOL));
						}else{
							return new Comparison(new ExpressionSkeleton(Datatype.DECIMAL), Comparison.Comp.EQ, new ExpressionSkeleton(Datatype.DECIMAL));
						}
					}else if(name.equals("@C_neq")){
						if(ASTUtility.randomBool()){
							return new Comparison(new ExpressionSkeleton(Datatype.BOOL), Comparison.Comp.NEQ, new ExpressionSkeleton(Datatype.BOOL));
						}else{
							return new Comparison(new ExpressionSkeleton(Datatype.DECIMAL), Comparison.Comp.NEQ, new ExpressionSkeleton(Datatype.DECIMAL));
						}
					}else if(name.equals("@C_gt")){
						return new Comparison(new ExpressionSkeleton(Datatype.DECIMAL), Comparison.Comp.GT, new ExpressionSkeleton(Datatype.DECIMAL));
					}else if(name.equals("@C_lt")){
						return new Comparison(new ExpressionSkeleton(Datatype.DECIMAL), Comparison.Comp.LT, new ExpressionSkeleton(Datatype.DECIMAL));
					}else if(name.equals("@C_ge")){
						return new Comparison(new ExpressionSkeleton(Datatype.DECIMAL), Comparison.Comp.GE, new ExpressionSkeleton(Datatype.DECIMAL));
					}else if(name.equals("@C_le")){
						return new Comparison(new ExpressionSkeleton(Datatype.DECIMAL), Comparison.Comp.LE, new ExpressionSkeleton(Datatype.DECIMAL));
					}else{
						//U_not as fallback case
						return new ASTUnary(ASTUnary.UnaryOp.NOT, new ExpressionSkeleton(Datatype.BOOL));
					}
					 
				}else{
					return randomFunction;
				}
			}
		}
	}
	
	private ASTExpression randomCollider(boolean prioritizeLiteral){
		if(prioritizeLiteral || ASTUtility.randomBool()){
			return new StringConstant(ASTUtility.randomRange(1, Integer.MAX_VALUE)+","+ASTUtility.randomRange(1, Integer.MAX_VALUE));
		}else{
			Function randomFunction = getRandomFunctionOfType(Datatype.COLLIDER);
			if(randomFunction == null){
				return new StringConstant(ASTUtility.randomRange(1, Integer.MAX_VALUE)+","+ASTUtility.randomRange(1, Integer.MAX_VALUE));
			}else{
				return randomFunction;
			}
		}
	}
	
	//Need special case for handling MathOp/U_neg
	private ASTExpression randomDecimal(boolean prioritizeLiteral){
		if(prioritizeLiteral || ASTUtility.randomBool()){
			return new FloatConstant(""+ASTUtility.randomFloat(false));
		}else{
			if(ASTUtility.randomBool()){
				return randomInt(prioritizeLiteral);
			}
			Function randomFunction = getRandomFunctionOfType(Datatype.DECIMAL);
			if(randomFunction == null){
				return new FloatConstant(""+ASTUtility.randomFloat(false));
			}else{
				String name = randomFunction.getName();
				
				if(name.startsWith("@")){
					//Special case
					if(name.equals("@A_add")){
						return new Arithmetic(new ExpressionSkeleton(Datatype.DECIMAL), Arithmetic.MathOp.ADD, new ExpressionSkeleton(Datatype.DECIMAL));
					}else if(name.equals("@A_sub")){
						return new Arithmetic(new ExpressionSkeleton(Datatype.DECIMAL), Arithmetic.MathOp.SUB, new ExpressionSkeleton(Datatype.DECIMAL));
					}else if(name.equals("@A_mul")){
						return new Arithmetic(new ExpressionSkeleton(Datatype.DECIMAL), Arithmetic.MathOp.MUL, new ExpressionSkeleton(Datatype.DECIMAL));
					}else if(name.equals("@A_div")){
						return new Arithmetic(new ExpressionSkeleton(Datatype.DECIMAL), Arithmetic.MathOp.DIV, new ExpressionSkeleton(Datatype.DECIMAL));
					}else if(name.equals("@A_mod")){
						return new Arithmetic(new ExpressionSkeleton(Datatype.DECIMAL), Arithmetic.MathOp.MOD, new ExpressionSkeleton(Datatype.DECIMAL));
					}else{
						//U_neg as fallback
						return new ASTUnary(ASTUnary.UnaryOp.NEG, new ExpressionSkeleton(Datatype.DECIMAL));
					}
				}else{
					return randomFunction;
				}
			}
		}
	}
	
	private ASTExpression randomDecimalSet(){
		Function func = getRandomFunctionOfType(Datatype.DECIMAL_SET);
		if(func == null){
			//Fallback
			List<ASTExpression> params = new LinkedList<ASTExpression>();
			for(int c=1; c<=3; c++){
				params.add(new ExpressionSkeleton(Datatype.DECIMAL));
			}
			return new Function("DecimalSet", params, ASTUtility.randomBool());
		}else{
			return func;
		}
	}
	
	private ASTExpression randomDirection(boolean prioritizeLiteral){
		if(prioritizeLiteral || ASTUtility.randomBool()){
			return new StringConstant(""+ASTUtility.randomFloatFromZero(360));
		}else{
			Function func = getRandomFunctionOfType(Datatype.DIRECTION);
			if(func == null){
				return new StringConstant(""+ASTUtility.randomFloatFromZero(360));
			}else{
				return func;
			}
		}
	}
	
	private ASTExpression randomDirectionSet(){
		Function func = getRandomFunctionOfType(Datatype.DIRECTION_SET);
		if(func == null){
			//Fallback
			List<ASTExpression> params = new LinkedList<ASTExpression>();
			params.add(new ExpressionSkeleton(Datatype.DIRECTION));
			return new Function("DirectionSet", params, ASTUtility.randomBool());
		}else{
			return func;
		}
	}
	
	private ASTExpression randomDynamic(){
		Function func = getRandomFunctionOfType(Datatype.DYNAMIC);
		if(func == null){
			//Fallback
			List<ASTExpression> params = new LinkedList<ASTExpression>();
			params.add(new StringConstant("this"));
			return new Function("DynamicFilter", params, ASTUtility.randomBool());
		}else{
			return func;
		}
	}
	
	private ASTExpression randomIdentifier(){
		if(spawnFill){
			return new Identifier("."+ASTUtility.randomUniform(root.getRelatedAgents()).getIdentifier());
		}else{
			return new Identifier("."+ASTUtility.randomUniform(fillingAgent.getStates()).getIdentifier());
		}
	}
	
	private ASTExpression randomInt(boolean prioritizeLiteral){
		if(prioritizeLiteral || ASTUtility.randomBool()){
			return new IntConstant(""+ASTUtility.randomRange(Integer.MIN_VALUE, Integer.MAX_VALUE));
		}else{
			Function func = getRandomFunctionOfType(Datatype.INT);
			if(func == null){
				return new IntConstant(""+ASTUtility.randomRange(Integer.MIN_VALUE, Integer.MAX_VALUE));
			}else{
				return func;
			}
		}
	}
	
	private ASTExpression randomPosition(boolean prioritizeLiteral){
		if(prioritizeLiteral || ASTUtility.randomBool()){
			return new StringConstant("c("+ASTUtility.randomFloat(false)+","+ASTUtility.randomFloat(false)+")");
		}else{
			Function func = getRandomFunctionOfType(Datatype.POSITION);
			if(func == null){
				return new StringConstant("c("+ASTUtility.randomFloat(false)+","+ASTUtility.randomFloat(false)+")");
			}else{
				return func;
			}
		}
	}
	
	private Function getRandomFunctionOfType(Datatype desiredReturnType){
		Signature sig = GeneratorRegistry.getRandomFunctionSignature(desiredReturnType);
		if(sig == null)
			return null;
		String functionName = GeneratorRegistry.getFunctionName(sig.getId());
		List<ASTExpression> paramList = new LinkedList<ASTExpression>();
		int choiceSymbolIndex = functionName.indexOf('#');
		if(choiceSymbolIndex > -1){
			String choice = functionName.substring(choiceSymbolIndex+1);
			functionName = functionName.substring(0, choiceSymbolIndex);
			paramList.add(new StringConstant(choice));
		}
		for(Datatype expectingType : sig.getParamType()){
			paramList.add(new ExpressionSkeleton(expectingType));
		}
		return new Function(functionName, paramList, ASTUtility.randomBool());
	}
}
