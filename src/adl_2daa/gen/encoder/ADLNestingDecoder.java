package adl_2daa.gen.encoder;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import adl_2daa.ast.ASTExpression;
import adl_2daa.ast.expression.StringConstant;
import adl_2daa.ast.statement.Action;
import adl_2daa.gen.signature.GeneratorRegistry;
import adl_2daa.gen.signature.Signature;
import de.parsemis.graph.Edge;
import de.parsemis.graph.Graph;
import de.parsemis.graph.Node;

public class ADLNestingDecoder {

	public static final ADLNestingDecoder instance = new ADLNestingDecoder();
	
	public Object decode(Graph<Integer,Integer> graph){
		Node<Integer,Integer> root = null;
		Iterator<Node<Integer,Integer>> nodeIt = graph.nodeIterator();
		while(nodeIt.hasNext()){
			root = nodeIt.next();
			if(root.getInDegree() == 0){
				break;
			}
		}
		if(root.getLabel() < EncodeTable.idOffset){
			//Action
			return decodeAction(root);
		}else{
			//Function
			return decodeExpression(root);
		}
	}
	
	public Action decodeAction(Node<Integer, Integer> root){
		List<ASTExpression> paramList = new LinkedList<ASTExpression>();
		int actionID = root.getLabel();
		String actionName = GeneratorRegistry.getActionName(actionID);
		String choice = null;
		Signature sig = null;
		int choiceSymbolIndex = actionName.indexOf('#');
		if(choiceSymbolIndex > -1){
			choice = actionName.substring(choiceSymbolIndex+1);
			actionName = actionName.substring(0, choiceSymbolIndex);
			sig = GeneratorRegistry.getActionSignature(actionName).getChoiceSignature(choice);
			//First param is a choice
			paramList.add(new StringConstant(choice));
		}else{
			sig = GeneratorRegistry.getActionSignature(actionName).getMainSignature();
		}
		
		//Fill up param to match the signature
		while(paramList.size() < sig.getParamType().length){
			paramList.add(null);
		}
		
		Iterator<Edge<Integer,Integer>> edgeIt = root.outgoingEdgeIterator();
		Edge<Integer, Integer> edge;
		while(edgeIt.hasNext()){
			edge = edgeIt.next();
			if(edge.getLabel() >= paramList.size()){
				System.out.println("Improper edge label found! action name="+actionName);
				continue;
			}
			
		}
		
		//Trim all tail null until the param length is at minimum
		
		//Change null to ExpressionSkeleton instead
		
		return new Action(actionName, paramList);
	}
	
	public ASTExpression decodeExpression(Node<Integer, Integer> root){
		return null;
	}
}
