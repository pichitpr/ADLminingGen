package adl_2daa.gen.encoder;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import parsemis.extension.GraphCreationHelper;
import adl_2daa.ast.ASTExpression;
import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.expression.ASTBinary;
import adl_2daa.ast.expression.ASTUnary;
import adl_2daa.ast.expression.And;
import adl_2daa.ast.expression.Arithmetic;
import adl_2daa.ast.expression.Comparison;
import adl_2daa.ast.expression.Function;
import adl_2daa.ast.expression.Or;
import adl_2daa.ast.expression.StringConstant;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.statement.Condition;
import adl_2daa.ast.statement.Loop;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.gen.signature.ActionMainSignature;
import adl_2daa.gen.signature.FunctionMainSignature;
import adl_2daa.gen.signature.GeneratorRegistry;
import adl_2daa.gen.signature.MainSignature;
import adl_2daa.gen.signature.Signature;
import de.parsemis.graph.Graph;

public class ADLNestingEncoder {

	protected static ADLNestingEncoder instance = new ADLNestingEncoder();
	
	private List<Graph<Integer,Integer>> graphCollection;
	private GraphCreationHelper<Integer, Integer> graph; 
	{
		graph = new GraphCreationHelper<Integer, Integer>();
	}
	
	/**
	 * Parse a sequence as nesting relation graphs. The function ID used here differs
	 * from sequence encoding scheme (+offset so there is no duplicated ID)
	 */
	protected Collection<Graph<Integer,Integer>> parseAsGraphCollection(Sequence astSequence){
		graphCollection = new LinkedList<Graph<Integer,Integer>>();
		
		for(ASTStatement st : astSequence.getStatements()){
			parseStatement(st);
		}
		
		//Remove single node graph (node of this type does not contribute to mining/generation)
		graphCollection.removeIf(graph -> graph.getNodeCount() <= 1);
		
		return graphCollection;
	}
	
	private void parseStatement(ASTStatement st){
		if(st instanceof Action){
			parseAction((Action)st);
		}else if(st instanceof Condition){
			parseConditionStruct((Condition)st);
		}else{
			parseLoop((Loop)st);
		}
	}
	
	private void parseAction(Action action){
		graph.createNewGraph(GraphCreationHelper.getID());
		
		ActionMainSignature mainSig = GeneratorRegistry.getActionSignature(action.getName());
		Signature sig = mainSig.getMainSignature();
		if(mainSig.hasChoice()){
			String choice = ((StringConstant)action.getParams()[0]).getValue();
			sig = mainSig.getChoiceSignature(choice);
		}
		int rootIndex = graph.addNode(sig.getId());
		
		for(int i=0; i<action.getParams().length; i++){
			if(i == 0 && mainSig.hasChoice()) continue;
			parseExpression(action.getParams()[i], rootIndex, i);
			/*
			Function func = getFirstFunctionInExpression(action.getParams()[i]);
			if(func != null){
				parseFunction(func, graph, rootIndex, i);
			}
			*/
		}
		
		graphCollection.add(graph.finishGraph());
	}
	
	private void parseConditionStruct(Condition conditionStruct){
		parseExpression(conditionStruct.getCondition(), -1, -1);
		for(ASTStatement st : conditionStruct.getIfblock()){
			parseStatement(st);
		}
		if(conditionStruct.getElseblock() != null){
			for(ASTStatement st : conditionStruct.getElseblock()){
				parseStatement(st);
			}
		}
	}
	
	private void parseLoop(Loop loop){
		parseExpression(loop.getLoopCount(), -1, -1);
		for(ASTStatement st : loop.getContent()){
			parseStatement(st);
		}
	}
	
	/**
	 * Parse expression into multiple subgraphs of function linked to the provided parent.
	 * If provided expression is not nested inside other
	 * "function", provides the remaining arguments as: <br/>
	 * parentIndex = -1 <br/>
	 * edgeLabel = any <br/> 
	 */
	private void parseExpression(ASTExpression exp, int parentIndex, int edgeLabel){
		if(exp instanceof Function){
			parseFunction((Function)exp, parentIndex, edgeLabel);
		}else if(exp instanceof ASTBinary){
			parseASTBinary((ASTBinary)exp, parentIndex, edgeLabel);
		}else if(exp instanceof ASTUnary){
			parseASTUnary((ASTUnary)exp, parentIndex, edgeLabel);
		}
		//Do nothing for Literal
	}
	
	/**
	 * Parse function as a subgraph. If provided function is not nested inside other
	 * function, provides the remaining arguments as: <br/>
	 * graph = null <br/>
	 * parentIndex = -1 <br/>
	 * edgeLabel = any <br/> 
	 */
	private void parseFunction(Function function, int parentIndex, int edgeLabel){
		if(parentIndex == -1){
			graph.createNewGraph(GraphCreationHelper.getID());
		}
		
		MainSignature mainSig = GeneratorRegistry.getFunctionSignature(function.getName());
		Signature sig = mainSig.getMainSignature();
		if(mainSig.hasChoice()){
			String choice = ((StringConstant)function.getParams()[0]).getValue();
			sig = mainSig.getChoiceSignature(choice);
		}
		int functionID = sig.getId()+EncodeTable.idOffset;
		if(function.hasSingleQuery()) functionID = -functionID;
		int rootIndex = graph.addNode(functionID);
		
		for(int i=0; i<function.getParams().length; i++){
			if(i == 0 && mainSig.hasChoice()) continue;
			parseExpression(function.getParams()[i], rootIndex, i);
			/*
			Function func = getFirstFunctionInExpression(function.getParams()[i]);
			if(func != null){
				parseFunction(func, graph, rootIndex, i);
			}
			*/
		}
		
		if(parentIndex == -1){
			graphCollection.add(graph.finishGraph());
		}else{
			graph.addEdge(parentIndex, rootIndex, edgeLabel, true);
		}
	}
	
	/**
	 * For parameters see {@link ADLNestingEncoder#parseFunction(Function, int, int)}
	 */
	private void parseASTBinary(ASTBinary astBinary, int parentIndex, int edgeLabel){
		if(parentIndex == -1){
			graph.createNewGraph(GraphCreationHelper.getID());
		}
		
		FunctionMainSignature func = null;
		if(astBinary instanceof And){
			func = GeneratorRegistry.getFunctionSignature("@L_and");
		}else if(astBinary instanceof Or){
			func = GeneratorRegistry.getFunctionSignature("@L_or");
		}else if(astBinary instanceof Comparison){
			switch(((Comparison)astBinary).getOp()){
			case EQ:
				func = GeneratorRegistry.getFunctionSignature("@C_eq");
				break;
			case NEQ:
				func = GeneratorRegistry.getFunctionSignature("@C_neq");
				break;
			case GT:
				func = GeneratorRegistry.getFunctionSignature("@C_gt");
				break;
			case GE:
				func = GeneratorRegistry.getFunctionSignature("@C_ge");
				break;
			case LT:
				func = GeneratorRegistry.getFunctionSignature("@C_lt");
				break;
			case LE:
				func = GeneratorRegistry.getFunctionSignature("@C_le");
				break;
			}
		}else if(astBinary instanceof Arithmetic){
			switch(((Arithmetic)astBinary).getOp()){
			case ADD:
				func = GeneratorRegistry.getFunctionSignature("@A_add");
				break;
			case SUB:
				func = GeneratorRegistry.getFunctionSignature("@A_sub");
				break;
			case MUL:
				func = GeneratorRegistry.getFunctionSignature("@A_mul");
				break;
			case DIV:
				func = GeneratorRegistry.getFunctionSignature("@A_div");
				break;
			case MOD:
				func = GeneratorRegistry.getFunctionSignature("@A_mod");
				break;
			}
		}
		
		int rootIndex = graph.addNode(func.getMainSignature().getId());
		parseExpression(astBinary.left, rootIndex, 0);
		parseExpression(astBinary.right, rootIndex, 1);
		
		if(parentIndex == -1){
			graphCollection.add(graph.finishGraph());
		}else{
			graph.addEdge(parentIndex, rootIndex, edgeLabel, true);
		}
	}
	
	/**
	 * For parameters see {@link ADLNestingEncoder#parseFunction(Function, int, int)}
	 */
	private void parseASTUnary(ASTUnary astUnary, int parentIndex, int edgeLabel){
		if(parentIndex == -1){
			graph.createNewGraph(GraphCreationHelper.getID());
		}
		
		FunctionMainSignature func = null;
		if(astUnary.op == ASTUnary.UnaryOp.NOT){
			func = GeneratorRegistry.getFunctionSignature("@U_not");
		}else{
			func = GeneratorRegistry.getFunctionSignature("@U_neg");
		}
		
		int rootIndex = graph.addNode(func.getMainSignature().getId());
		parseExpression(astUnary.getNode(), rootIndex, 0);
		
		if(parentIndex == -1){
			graphCollection.add(graph.finishGraph());
		}else{
			graph.addEdge(parentIndex, rootIndex, edgeLabel, true);
		}
	}
	
	/**
	 * Find first function that appears in the expression using DFS. 
	 * Return null if no function found
	 */
	/*private Function getFirstFunctionInExpression(ASTExpression exp){
		if(exp instanceof Function){
			return (Function)exp;
		}else if(exp instanceof ASTBinary){
			Function func =  getFirstFunctionInExpression( ((ASTBinary)exp).left );
			if(func == null){
				func = getFirstFunctionInExpression( ((ASTBinary)exp).right );
			}
			return func;
		}else if(exp instanceof ASTUnary){
			return getFirstFunctionInExpression( ((ASTUnary)exp).node );
		}
		//Null for Literal
		return null;
	}*/
}
