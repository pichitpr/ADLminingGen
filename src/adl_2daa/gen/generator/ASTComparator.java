package adl_2daa.gen.generator;

import adl_2daa.ast.ASTExpression;
import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.expression.ASTBinary;
import adl_2daa.ast.expression.ASTUnary;
import adl_2daa.ast.expression.And;
import adl_2daa.ast.expression.Arithmetic;
import adl_2daa.ast.expression.Bitwise;
import adl_2daa.ast.expression.Comparison;
import adl_2daa.ast.expression.Function;
import adl_2daa.ast.expression.Or;
import adl_2daa.ast.expression.StringConstant;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.statement.Condition;
import adl_2daa.ast.statement.Loop;
import adl_2daa.gen.signature.FunctionMainSignature;
import adl_2daa.gen.signature.GeneratorRegistry;

/**
 * This class provides AST comparator. How the comparator works reflects the encoder/decoder
 */
public class ASTComparator {

	/**
	 * Compare 2 statements WITHOUT comparing the containing block(s) <br/>
	 * - Action : compare action and arguments <br/>
	 * - Condition : compare condition expression only <br/>>
	 * - Loop : compare loop count expression only
	 */
	public static boolean astStatementEquals(ASTStatement st1, ASTStatement st2, int depth){
		if(st1 == st2) return true;
		if(st1.getClass() != st2.getClass()) return false;
		if(st1 instanceof Action){
			Action a1 = (Action)st1;
			Action a2 = (Action)st2;
			if(!a1.getName().equals(a2.getName())) return false;
			assert(a1.getParams().length != a2.getParams().length);
			for(int i=0; i<a1.getParams().length; i++){
				if(!astExpressionEquals(a1.getParams()[i], a2.getParams()[i], depth))
					return false;
			}
			return true;
		}else if(st1 instanceof Condition){
			Condition c1 = (Condition)st1;
			Condition c2 = (Condition)st2;
			return astExpressionEquals(c1.getCondition(), c2.getCondition(), depth);
		}else if(st1 instanceof Loop){
			Loop l1 = (Loop)st1;
			Loop l2 = (Loop)st2;
			return astExpressionEquals(l1.getLoopCount(), l2.getLoopCount(), depth);
		}
		return false;
	}

	/**
	 * Compare 2 expressions for "depth" levels. Negative level will compare the entire tree.<br/>
	 * Example: <br/>
	 * - Given an expression (a+b >= c-d)
	 * - Level 1 : compare >= to another
	 * - Level 2 : compare + on the left side && compare - on the right side
	 * - Level 3 : compare all a,b,c,d literal type and topology respectively
	 */
	public static boolean astExpressionEquals(ASTExpression exp1, ASTExpression exp2, int depth){
		if(depth == 0) return true;
		if(exp1 == exp2) return true;
		if(exp1 instanceof ExpressionSkeleton){
			return ((ExpressionSkeleton) exp1).isCompatibleWith(exp2);
		}else if(exp2 instanceof ExpressionSkeleton){
			return ((ExpressionSkeleton) exp2).isCompatibleWith(exp1);
		}else{
			if(exp1.getClass() != exp2.getClass()) return false;
			if(exp1 instanceof ASTUnary){
				return astUnaryEquals((ASTUnary)exp1, (ASTUnary)exp2, depth-1);
			}
			if(exp1 instanceof ASTBinary){
				return astBinaryEquals((ASTBinary)exp1, (ASTBinary)exp2, depth-1);
			}
			if(exp1 instanceof Function){
				return astFunctionEquals((Function)exp1, (Function)exp2, depth-1);
			}
			//Do not compare literal "value"
			return true;
		}
	}
	
	public static boolean astUnaryEquals(ASTUnary uop1, ASTUnary uop2, int depth){
		return uop1.op == uop2.op && astExpressionEquals(uop1.getNode(), uop2.getNode(),
				depth);
	}
	
	public static boolean astBinaryEquals(ASTBinary bin1, ASTBinary bin2, int depth){
		if(bin1.getClass() != bin2.getClass()) return false;
		if(bin1 instanceof Arithmetic){
			if( ((Arithmetic) bin1).op != ((Arithmetic) bin2).op) return false;
		}else if(bin1 instanceof Bitwise){
			if( ((Bitwise) bin1).op != ((Bitwise) bin2).op) return false;
		}else if(bin1 instanceof Comparison){
			if( ((Comparison) bin1).getOp() != ((Comparison) bin2).getOp()) return false;
		}else if(bin1 instanceof And || bin1 instanceof Or){
		}else{
			//Impossible case!
			return false;
		}
		return astExpressionEquals(bin1.left, bin2.left, depth) && 
				astExpressionEquals(bin1.right, bin2.right, depth);
	}
	
	public static boolean astFunctionEquals(Function f1, Function f2, int depth){
		String fname = f1.getName();
		if(!fname.equals(f2.getName()) || (f1.hasSingleQuery() != f2.hasSingleQuery()) ) return false;
		assert(f1.getParams().length == f2.getParams().length);
		FunctionMainSignature fsig = GeneratorRegistry.getFunctionSignature(fname);
		int startExpIndex = 0;
		if(fsig.hasChoice()){
			assert(f1.getParams().length > 0);
			assert(f1.getParams()[0] instanceof StringConstant && 
					f2.getParams()[0] instanceof StringConstant);
			String choice1 = ((StringConstant)f1.getParams()[0]).getValue();
			String choice2 = ((StringConstant)f2.getParams()[0]).getValue();
			if(!choice1.equals(choice2))
				return false;
			startExpIndex = 1;
		}
		for(; startExpIndex<f1.getParams().length; startExpIndex++){
			if(!astExpressionEquals(f1.getParams()[startExpIndex], 
					f2.getParams()[startExpIndex], depth)){
				return false;
			}
		}
		return true;
	}
	
}
