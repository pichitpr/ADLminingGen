package adl_2daa.gen.signature;

public class FunctionMainSignature extends MainSignature{

	private static int ID_COUNT = 0;
	protected static void resetID(){
		ID_COUNT = 0;
	}
	
	public static int getTotalFunctionCount(){
		return ID_COUNT;
	}
	
	public FunctionMainSignature(Datatype returnType, int minParamSize, int choiceParamIndex,
			Datatype... param){
		super(ID_COUNT, returnType, minParamSize, choiceParamIndex, param);
		ID_COUNT++;
	}
	
	public FunctionMainSignature(Datatype returnType, Datatype... param) {
		this(returnType, param.length, -1, param);
	}
	
	@Override
	public void addChoiceSignature(String choice, Signature signature){
		super.addChoiceSignature(choice, signature);
		signature.id = ID_COUNT;
		ID_COUNT++;
	}
}
