package adl_2daa.gen.encoder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import adl_2daa.ast.ASTExpression;
import adl_2daa.ast.expression.ASTBinary;
import adl_2daa.ast.expression.ASTUnary;
import adl_2daa.ast.expression.ASTUnary.UnaryOp;
import adl_2daa.ast.expression.And;
import adl_2daa.ast.expression.Arithmetic;
import adl_2daa.ast.expression.Arithmetic.MathOp;
import adl_2daa.ast.expression.Comparison;
import adl_2daa.ast.expression.Comparison.Comp;
import adl_2daa.ast.expression.Function;
import adl_2daa.ast.expression.Or;
import adl_2daa.ast.expression.StringConstant;
import adl_2daa.ast.statement.Action;
import adl_2daa.gen.Utility;
import adl_2daa.gen.generator.ExpressionSkeleton;
import adl_2daa.gen.signature.FunctionMainSignature;
import adl_2daa.gen.signature.GeneratorRegistry;
import adl_2daa.gen.signature.Signature;
import de.parsemis.graph.Edge;
import de.parsemis.graph.Graph;
import de.parsemis.graph.Node;

public class ADLNestingDecoder {

	public static final ADLNestingDecoder instance = new ADLNestingDecoder();
	
	public static boolean isLiteral(int encodedData){
		int maxFunctionID = FunctionMainSignature.getTotalFunctionCount()+EncodeTable.idOffset;
		return Math.abs(encodedData) > maxFunctionID;
	}
	
	public Object decode(Graph<Integer,Integer> graph){
		Node<Integer,Integer> root = Utility.findFirstRoot(graph);
		//Root should be either Action or Function, should not be literal
		if(root.getLabel() == EncodeTable.LITERAL_COLLECTION_ROOT){
			System.out.println("Impossible case : literal collection as root");
			return null;
		}
		if(root.getLabel() < EncodeTable.idOffset && root.getLabel() >= 0){
			//Action
			return decodeAction(root);
		}else{
			//Function
			return decodeExpression(root);
		}
	}
	
	private Action decodeAction(Node<Integer, Integer> root){
		int actionID = root.getLabel();
		String actionName = GeneratorRegistry.getActionName(actionID);
		String choice = null;
		Signature sig = null;
		int choiceSymbolIndex = actionName.indexOf('#');
		if(choiceSymbolIndex > -1){
			choice = actionName.substring(choiceSymbolIndex+1);
			actionName = actionName.substring(0, choiceSymbolIndex);
			sig = GeneratorRegistry.getActionSignature(actionName).getChoiceSignature(choice);
		}else{
			sig = GeneratorRegistry.getActionSignature(actionName).getMainSignature();
		}
		
		List<ASTExpression> paramList = decodeParameter(root, actionName, sig, choice);
		return new Action(actionName, paramList);
	}
	
	private ASTExpression decodeExpression(Node<Integer, Integer> root){
		if(root.getLabel() == EncodeTable.LITERAL_COLLECTION_ROOT){
			return decodeLiteralCollection(root);
		}
		int functionID = root.getLabel();
		if(functionID < 0) functionID = -functionID;
		functionID -= EncodeTable.idOffset;
		String functionName = GeneratorRegistry.getFunctionName(functionID);
		if(!functionName.startsWith("@")){
			return decodeFunction(root);
		}
		if(functionName.startsWith("@U")){
			return parseASTUnary(root);
		}else{
			return parseASTBinary(root);
		}
	}
	
	@SuppressWarnings("rawtypes")
	private NestingLiteralCollectionExp decodeLiteralCollection(Node<Integer, Integer> root){
		List<Integer> literalCollection = new ArrayList<Integer>();
		Iterator<Edge<Integer,Integer>> edgeIt = root.outgoingEdgeIterator();
		while(edgeIt.hasNext()){
			literalCollection.add(edgeIt.next().getLabel());
		}
		if(literalCollection.size() == 0){
			System.out.println("Impossible case : literal collection found without item in collection");
			return null;
		}
		return NestingLiteralCollectionExp.parseEncodedLiteral(literalCollection);
	}
	
	private Function decodeFunction(Node<Integer, Integer> root){
		//Duplicate code but for modularity
		int functionID = root.getLabel();
		boolean hasSingleQuery = false;
		if(functionID < 0){ 
			functionID = -functionID;
			hasSingleQuery = true;
		}
		functionID -= EncodeTable.idOffset;
		String functionName = GeneratorRegistry.getFunctionName(functionID);
		
		String choice = null;
		Signature sig = null;
		int choiceSymbolIndex = functionName.indexOf('#');
		if(choiceSymbolIndex > -1){
			choice = functionName.substring(choiceSymbolIndex+1);
			functionName = functionName.substring(0, choiceSymbolIndex);
			sig = GeneratorRegistry.getFunctionSignature(functionName).getChoiceSignature(choice);
		}else{
			sig = GeneratorRegistry.getFunctionSignature(functionName).getMainSignature();
		}
		
		List<ASTExpression> paramList = decodeParameter(root, functionName, sig, choice);
		return new Function(functionName, paramList, hasSingleQuery);
	}
	
	private ASTBinary parseASTBinary(Node<Integer, Integer> root){
		int functionID = root.getLabel()-EncodeTable.idOffset;
		String op = GeneratorRegistry.getFunctionName(functionID);
		Signature sig = GeneratorRegistry.getFunctionSignature(op).getMainSignature();
		ASTExpression left=null,right=null;
		Iterator<Edge<Integer,Integer>> edgeIt = root.outgoingEdgeIterator();
		while(edgeIt.hasNext()){
			Edge<Integer, Integer> edge = edgeIt.next();
			if(edge.getLabel() == 0){
				left = decodeExpression(edge.getOtherNode(root));
			}else if(edge.getLabel() == 1){
				right = decodeExpression(edge.getOtherNode(root));
			}
		}
		if(left == null){
			left = new ExpressionSkeleton(sig.getParamType()[0]);
		}
		if(right == null){
			right = new ExpressionSkeleton(sig.getParamType()[1]);
		}
		
		switch(op){
		case "@L_and": return new And(left, right);
		case "@L_or": return new Or(left, right);
		case "@C_eq": return new Comparison(left, Comp.EQ, right);
		case "@C_neq": return new Comparison(left, Comp.NEQ, right);
		case "@C_gt": return new Comparison(left, Comp.GT, right);
		case "@C_lt": return new Comparison(left, Comp.LT, right);
		case "@C_ge": return new Comparison(left, Comp.GE, right);
		case "@C_le": return new Comparison(left, Comp.LE, right);
		case "@A_add": return new Arithmetic(left, MathOp.ADD, right);
		case "@A_sub": return new Arithmetic(left, MathOp.SUB, right);
		case "@A_mul": return new Arithmetic(left, MathOp.MUL, right);
		case "@A_div": return new Arithmetic(left, MathOp.DIV, right);
		case "@A_mod": return new Arithmetic(left, MathOp.MOD, right);
		}
		
		//Impossible case
		System.out.println("Reach impossible case for NestingDecode "+op);
		return null;
	}
	
	private ASTUnary parseASTUnary(Node<Integer, Integer> root){
		int functionID = root.getLabel()-EncodeTable.idOffset;
		String op = GeneratorRegistry.getFunctionName(functionID);
		Signature sig = GeneratorRegistry.getFunctionSignature(op).getMainSignature();
		Iterator<Edge<Integer,Integer>> edgeIt = root.outgoingEdgeIterator();
		ASTExpression exp = null;
		if(edgeIt.hasNext()){
			exp = decodeExpression(edgeIt.next().getOtherNode(root));
		}
		if(exp == null){
			exp = new ExpressionSkeleton(sig.getParamType()[0]);
		}
		return new ASTUnary(op.equals("@U_neg") ? UnaryOp.NEG : UnaryOp.NOT, exp);
	}
	
	private List<ASTExpression> decodeParameter(Node<Integer, Integer> root, String functionName, 
			Signature signature, String choice){
		List<ASTExpression> paramList = new ArrayList<ASTExpression>();
		//Function with choice has its first parameter trimmed result in parameter count
		//got from GeneratorRegistry lower than the actual function by 1
		int paramCountOffset = 0; 
		if(choice != null){ 
			paramList.add(new StringConstant(choice));
			paramCountOffset++;
		}
		
		//Fill up param to match the signature
		while(paramList.size() < signature.getParamType().length+paramCountOffset){
			paramList.add(null);
		}
		
		//Replace null with actual args
		Iterator<Edge<Integer,Integer>> edgeIt = root.outgoingEdgeIterator();
		while(edgeIt.hasNext()){
			Edge<Integer, Integer> edge = edgeIt.next();
			if(edge.getLabel() >= paramList.size()){
				System.out.println("Improper edge label " + edge.getLabel() +
						" found! function name="+functionName);
				continue;
			}
			//Datatype expectingType = signature.getParamType()[edge.getLabel()-paramCountOffset];
			ASTExpression exp = decodeExpression(edge.getOtherNode(root));
			/*
			//NOTE:: Compatibility check -- currently not support NestingLiteralCollection
			if(!(new ExpressionSkeleton(expectingType)).isCompatibleWith(exp)){
				StringBuilder strb = new StringBuilder();
				exp.toScript(strb, 0);
				System.out.println("Function " + functionName + "#" + choice + 
						"[" + edge.getLabel() + "] expecting type " + expectingType.name() + 
						" not matched with " + strb.toString());
				continue;
			}
			*/
			paramList.set(edge.getLabel(), exp);
		}
	
		//Trim all tail null until the param length is at minimum
		while(paramList.size() > signature.getMinParamSize()+paramCountOffset){
			if(paramList.get(paramList.size()-1) != null)
				break;
			paramList.remove(paramList.size()-1);
		}
		
		//Change null to ExpressionSkeleton instead
		for(int i=0; i<paramList.size(); i++){
			if(paramList.get(i) == null){
				paramList.set(i, new ExpressionSkeleton(signature.getParamType()[i-paramCountOffset]) );
			}
		}
		
		return paramList;
	}
}
