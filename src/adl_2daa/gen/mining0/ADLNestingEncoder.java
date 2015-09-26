package adl_2daa.gen.mining0;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import parsemis.extension.GraphCreationHelper;
import adl_2daa.ast.ASTExpression;
import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.expression.ASTBinary;
import adl_2daa.ast.expression.ASTUnary;
import adl_2daa.ast.expression.Function;
import adl_2daa.ast.expression.StringConstant;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.statement.Condition;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.gen.ActionMainSignature;
import adl_2daa.gen.GeneratorRegistry;
import adl_2daa.gen.MainSignature;
import adl_2daa.gen.Signature;
import de.parsemis.graph.Graph;

public class ADLNestingEncoder {

	protected static ADLNestingEncoder instance = new ADLNestingEncoder();
	protected static final int offset = 256;
	
	//TODO: Should filter out single node?? (No nested function) -- now count
	
	private List<Graph<Integer,Integer>> graphCollection;
	private GraphCreationHelper<Integer, Integer> graph = new GraphCreationHelper<Integer, Integer>();
	
	/**
	 * Parse a sequence as nesting relation graphs. The function ID used here differs
	 * from sequence encoding scheme (+offset so there is no duplicated ID)
	 */
	protected Collection<Graph<Integer,Integer>> parseAsGraphCollection(Sequence astSequence){
		graphCollection = new LinkedList<Graph<Integer,Integer>>();
		
		for(ASTStatement st : astSequence.getStatements()){
			parseStatement(st);
		}
		
		return graphCollection;
	}
	
	private void parseStatement(ASTStatement st){
		if(st instanceof Action){
			parseAction((Action)st);
		}else{
			parseConditionStruct((Condition)st);
		}
	}
	
	private void parseAction(Action action){
		graph.createNewGraph("action");
		
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
			parseExpression(((ASTBinary)exp).left, parentIndex, edgeLabel);
			parseExpression(((ASTBinary)exp).right, parentIndex, edgeLabel);
		}else if(exp instanceof ASTUnary){
			parseExpression(((ASTUnary)exp).node, parentIndex, edgeLabel);
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
			graph.createNewGraph("Function root");
		}
		
		MainSignature mainSig = GeneratorRegistry.getFunctionSignature(function.getName());
		Signature sig = mainSig.getMainSignature();
		if(mainSig.hasChoice()){
			String choice = ((StringConstant)function.getParams()[0]).getValue();
			sig = mainSig.getChoiceSignature(choice);
		}
		int rootIndex = graph.addNode(sig.getId()+offset);
		
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
