package adl_2daa.gen.signature;

import java.util.HashMap;

public abstract class MainSignature {
	
	protected Signature mainSignature;
	protected boolean hasChoice;
	protected HashMap<String, Signature> choiceSigMap;
	
	public MainSignature(int id, Datatype returnType, int minParamSize, int choiceParamIndex,
			Datatype... param){
		this.hasChoice = choiceParamIndex > -1;
		if(hasChoice){
			Datatype[] trimmedParam = new Datatype[param.length-1];
			for(int i=1; i<param.length; i++)
				trimmedParam[i-1] = param[i];
			this.mainSignature = new Signature(returnType, minParamSize-1, trimmedParam);
		}else{
			this.mainSignature = new Signature(returnType, minParamSize, param);
		}
		this.mainSignature.id = id;
	}
	
	/*
	public int getId(){
		return mainSignature.id;
	}
	*/
	
	public Signature getMainSignature(){
		return mainSignature;
	}
	
	public boolean hasChoice(){
		return hasChoice;
	}
	
	/**
	 * Add choice signature to main signature. The added signature must omit
	 * the first parameter (choice) from signature
	 */
	public void addChoiceSignature(String choice, Signature signature){
		if(choiceSigMap == null){
			choiceSigMap = new HashMap<String, Signature>();
		}
		choiceSigMap.put(choice, signature);
	}
	
	/**
	 * Return choice signature. The choice signature will have its first parameter
	 * (the choice) omitted thus the parameters length will be shorter than what
	 * appears in code. If the signature has no choice, this method returns mainSignature  
	 */
	public Signature getChoiceSignature(String choice){
		Signature sig = choiceSigMap.get(choice);
		if(sig == null){
			return mainSignature;
		}
		return sig;
	}
}
