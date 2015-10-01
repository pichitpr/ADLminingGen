package adl_2daa.gen.signature;

public class Signature {

	protected int id;
	protected Datatype returnType;
	protected Datatype[] param;
	protected int minParamSize;
	
	public Signature(Datatype returnType, int minParamSize, Datatype... param){
		this.returnType = returnType;
		this.minParamSize = minParamSize;
		this.param = param;
	}
	
	public Signature(Datatype returnType, Datatype... param){
		this(returnType, param.length, param);
	}
	
	public int getId(){
		return id;
	}
	
	public Datatype getReturnType(){
		return returnType;
	}
	
	public Datatype[] getParamType(){
		return param;
	}
}
