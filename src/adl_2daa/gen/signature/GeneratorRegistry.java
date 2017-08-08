package adl_2daa.gen.signature;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import adl_2daa.gen.generator.ASTUtility;

public class GeneratorRegistry {

	private static ActionMainSignature dummyActionSignature;
	public static ActionMainSignature getDummyActionSignature(){
		return dummyActionSignature;
	}
	
	private static HashMap<String,ActionMainSignature> actionSignatureMap = 
			new HashMap<String,ActionMainSignature>();
	private static HashMap<Integer,String> actionIdNameMap = new HashMap<Integer,String>();
	
	/**
	 * Register action signature to generator. All choices (if any) must be added
	 * to the signature before registering the signature 
	 */
	public static void registerActionSignature(String funcname, ActionMainSignature sig){
		sig.funcname = funcname;
		actionSignatureMap.put(funcname, sig);
		actionIdNameMap.put(sig.getMainSignature().getId(), funcname);
		if(sig.hasChoice()){
			for(Entry<String,Signature> choice : sig.choiceSigMap.entrySet()){
				actionIdNameMap.put(choice.getValue().getId(), funcname+"#"+choice.getKey());
			}
		}
	}
	
	/**
	 * Return MainSignature of the given action. A choice parameter (if any, at index 0)
	 * is trimmed from the MainSignature and the remaining parameters are shifted.
	 */
	public static ActionMainSignature getActionSignature(String funcCode){
		ActionMainSignature sig = actionSignatureMap.get(funcCode);
		if(sig == null){
			System.out.println("Action not found : "+funcCode);
		}
		return sig;
	}
	
	public static Signature getRandomActionSignature(){		
		ActionMainSignature mainSig = ASTUtility.randomUniform(actionSignatureMap.values());
		while(mainSig.funcname.equals("@dummy") || mainSig.funcname.equals("@Spawn")){
			mainSig = ASTUtility.randomUniform(actionSignatureMap.values());
		}
		Signature sig = mainSig.getMainSignature();
		if(mainSig.hasChoice()){
			sig = mainSig.getRandomChoiceSignature();
		}
		return sig;
	}
	
	/**
	 * Get action name by signature ID. If it is an ID for choice,
	 * the action name will be in actionName#choice format
	 */
	public static String getActionName(int id){
		String name = actionIdNameMap.get(id);
		if(name == null){
			System.out.println("Action name not found : ID "+id);
		}
		return name;
	}
	
	
	private static HashMap<String,FunctionMainSignature> functionSignatureMap = 
			new HashMap<String,FunctionMainSignature>();
	private static HashMap<Integer,String> functionIdNameMap = new HashMap<Integer,String>();
	
	/**
	 * Register function signature to generator. All choices (if any) must be added
	 * to the signature before registering the signature 
	 */
	public static void registerFunctionSignature(String funcname, FunctionMainSignature sig){
		sig.funcname = funcname;
		functionSignatureMap.put(funcname, sig);
		functionIdNameMap.put(sig.getMainSignature().getId(), funcname);
		if(sig.hasChoice()){
			for(Entry<String,Signature> choice : sig.choiceSigMap.entrySet()){
				functionIdNameMap.put(choice.getValue().getId(), funcname+"#"+choice.getKey());
			}
		}
	}
	
	/**
	 * Return MainSignature of the given function. A choice (if any, at index 0) is trimmed
	 * from the MainSignature and the remaining parameters are shifted.
	 */
	public static FunctionMainSignature getFunctionSignature(String funcCode){
		FunctionMainSignature sig = functionSignatureMap.get(funcCode);
		if(sig == null){
			System.out.println("Function not found : "+funcCode);
		}
		return sig;
	}
	
	public static Signature getRandomFunctionSignature(Datatype desiredReturnType){
		List<Signature> result = new LinkedList<Signature>();
		for(FunctionMainSignature mainSig : functionSignatureMap.values()){
			if(mainSig.hasChoice()){
				for(Signature choiceSig : mainSig.choiceSigMap.values()){
					if(choiceSig.returnType == desiredReturnType){
						result.add(choiceSig);
					}
				}
			}else{
				if(mainSig.getMainSignature().returnType == desiredReturnType){
					result.add(mainSig.getMainSignature());
				}
			}
		}
		if(result.size() == 0)
			return null;
		else
			return ASTUtility.randomUniform(result);
	}
	
	/**
	 * Get function name by signature ID. If it is an ID for choice,
	 * the function name will be in funtionName#"choice" format
	 */
	public static String getFunctionName(int id){
		String name = functionIdNameMap.get(id);
		if(name == null){
			System.out.println("Function name not found : ID "+id);
		}
		return name;
	}
	
	/**
	 * Clean up all registered action/function, also reset starting ID
	 * This method MUST be called once before registering action/function
	 */
	public static void cleanup(){
		actionSignatureMap.clear();
		actionIdNameMap.clear();
		ActionMainSignature.resetID();
		functionSignatureMap.clear();
		functionIdNameMap.clear();
		FunctionMainSignature.resetID();
		
		dummyActionSignature = new ActionMainSignature();
		registerActionSignature("@dummy", dummyActionSignature);
		
		//This will increase function's ID count by 1, so the first function ID is 1
		@SuppressWarnings("unused")
		FunctionMainSignature dummyFunctionSignature = new FunctionMainSignature(Datatype.BOOL);
	}
}
