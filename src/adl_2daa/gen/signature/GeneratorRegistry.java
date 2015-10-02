package adl_2daa.gen.signature;

import java.util.HashMap;
import java.util.Map.Entry;

public class GeneratorRegistry {

	public static final ActionMainSignature dummyAction = new ActionMainSignature();
	
	private static HashMap<String,ActionMainSignature> actionSignatureMap = 
			new HashMap<String,ActionMainSignature>();
	private static HashMap<Integer,String> actionIdNameMap = new HashMap<Integer,String>();
	
	/**
	 * Register action signature to generator. All choices (if any) must be added
	 * to the signature before registering the signature 
	 */
	public static void registerActionSignature(String funcname, ActionMainSignature sig){
		actionSignatureMap.put(funcname, sig);
		actionIdNameMap.put(sig.getMainSignature().getId(), funcname);
		if(sig.hasChoice()){
			for(Entry<String,Signature> choice : sig.choiceSigMap.entrySet()){
				actionIdNameMap.put(choice.getValue().getId(), funcname+"#\""+choice.getKey()+"\"");
			}
		}
	}
	
	public static ActionMainSignature getActionSignature(String funcCode){
		return actionSignatureMap.get(funcCode);
	}
	
	/**
	 * Get action name by signature ID. If it is an ID for choice,
	 * the action name will be in actionName#"choice" format
	 */
	public static String getActionName(int id){
		return actionIdNameMap.get(id);
	}
	
	
	private static HashMap<String,FunctionMainSignature> functionSignatureMap = 
			new HashMap<String,FunctionMainSignature>();
	private static HashMap<Integer,String> functionIdNameMap = new HashMap<Integer,String>();
	
	/**
	 * Register function signature to generator. All choices (if any) must be added
	 * to the signature before registering the signature 
	 */
	public static void registerFunctionSignature(String funcname, FunctionMainSignature sig){
		functionSignatureMap.put(funcname, sig);
		functionIdNameMap.put(sig.getMainSignature().getId(), funcname);
		if(sig.hasChoice()){
			for(Entry<String,Signature> choice : sig.choiceSigMap.entrySet()){
				functionIdNameMap.put(choice.getValue().getId(), funcname+"#\""+choice.getKey()+"\"");
			}
		}
	}
	
	public static FunctionMainSignature getFunctionSignature(String funcCode){
		return functionSignatureMap.get(funcCode);
	}
	
	/**
	 * Get function name by signature ID. If it is an ID for choice,
	 * the function name will be in funtionName#"choice" format
	 */
	public static String getFunctionName(int id){
		return functionIdNameMap.get(id);
	}
	
	/**
	 * Clean up all registered action/function, also reset starting ID
	 */
	public static void cleanup(){
		actionSignatureMap.clear();
		actionIdNameMap.clear();
		ActionMainSignature.resetID();
		functionSignatureMap.clear();
		functionIdNameMap.clear();
		FunctionMainSignature.resetID();
	}
}
