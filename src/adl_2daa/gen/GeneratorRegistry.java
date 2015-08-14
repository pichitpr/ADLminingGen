package adl_2daa.gen;

import java.util.HashMap;

public class GeneratorRegistry {

	public static final ActionMainSignature dummyAction = new ActionMainSignature();
	
	private static HashMap<String,ActionMainSignature> actionSignatureMap = 
			new HashMap<String,ActionMainSignature>();
	private static HashMap<Integer,String> actionIdNameMap = new HashMap<Integer,String>();
	
	public static void registerActionSignature(String funcname, ActionMainSignature sig){
		actionSignatureMap.put(funcname, sig);
		actionIdNameMap.put(sig.getMainSignature().getId(), funcname);
	}
	
	public static ActionMainSignature getActionSignature(String funcCode){
		return actionSignatureMap.get(funcCode);
	}
	
	public static String getActionName(int id){
		return actionIdNameMap.get(id);
	}
	
	
	private static HashMap<String,FunctionMainSignature> functionSignatureMap = 
			new HashMap<String,FunctionMainSignature>();
	private static HashMap<Integer,String> functionIdNameMap = new HashMap<Integer,String>();
	
	public static void registerFunctionSignature(String funcname, FunctionMainSignature sig){
		functionSignatureMap.put(funcname, sig);
		functionIdNameMap.put(sig.getMainSignature().getId(), funcname);
	}
	
	public static FunctionMainSignature getFunctionSignature(String funcCode){
		return functionSignatureMap.get(funcCode);
	}
	
	public static String getFunctionName(int id){
		return functionIdNameMap.get(id);
	}
}
