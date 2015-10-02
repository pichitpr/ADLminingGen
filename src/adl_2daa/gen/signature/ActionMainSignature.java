package adl_2daa.gen.signature;

public class ActionMainSignature extends MainSignature{

	private static int ID_COUNT = 0;
	protected static void resetID(){
		ID_COUNT = 0;
	}
	
	public ActionMainSignature(int minParamSize, int choiceParamIndex,
			Datatype... param){
		super(ID_COUNT, Datatype.VOID, minParamSize, choiceParamIndex, param);
		ID_COUNT++;
	}
	
	public ActionMainSignature(Datatype... param) {
		this(param.length, -1, param);
	}
	
	@Override
	public void addChoiceSignature(String choice, Signature signature){
		super.addChoiceSignature(choice, signature);
		signature.id = ID_COUNT;
		ID_COUNT++;
	}
}
