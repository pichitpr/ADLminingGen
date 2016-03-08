package adl_2daa.gen.encoder;

import java.util.ArrayList;
import java.util.List;

import adl_2daa.ast.ASTExpression;
import adl_2daa.gen.Utility;
import adl_2daa.gen.signature.Datatype;
import adl_2daa.internal.Instruction;
import adl_2daa.tool.ADLCompiler;

public abstract class NestingLiteralCollectionExp<T> extends ASTExpression {

	protected Datatype type;
	protected List<T> values;
	
	private NestingLiteralCollectionExp(Datatype type) {
		this.type = type;
		this.values = new ArrayList<T>();
	}
	
	/*
	public T get(int index){
		return values.get(index);
	}
	
	public int size(){
		return values.size();
	}
	*/
	
	public abstract void decodeAndAdd(int encodedData);
	
	public static NestingLiteralCollectionExp parseEncodedLiteral(List<java.lang.Integer> literalList){
		NestingLiteralCollectionExp collection = null;
		int type = (literalList.get(0) >> 29) & 7;
		switch(type){
		case 0: collection = new NestingLiteralCollectionExp.Boolean(); break;
		case 1: collection = new NestingLiteralCollectionExp.Integer(); break;
		case 2: collection = new NestingLiteralCollectionExp.Float(); break;
		case 3: collection = new NestingLiteralCollectionExp.Direction(); break;
		case 4: case 5: case 6:
			collection = new NestingLiteralCollectionExp.Position(); break;
		case 7:
			collection = new NestingLiteralCollectionExp.Collider(); break;
		}
		for(int encodedLiteral : literalList){
			collection.decodeAndAdd(encodedLiteral);
		}
		return collection;
	}
	
	private static int getFloatingPointAsInt(float zeroLeadingF, int places){
		zeroLeadingF = Math.round(zeroLeadingF*1000)/1000f;
		while(places > 0){
			zeroLeadingF *= 10f;
			places--;
		}
		return (int)zeroLeadingF;
	}
	
	private static float constructFloat(boolean isNeg, int intPart, int floatingPoint){
		float point = floatingPoint;
		while(point >= 1f){
			point /= 10f;
		}
		point += intPart;
		point = isNeg ? -point : point;
		return point;
	}
	
	@Override
	public void compile(List<Instruction> ins, ADLCompiler compiler) {}

	@Override
	public void toScript(StringBuilder str, int indent) {
		str.append("^"+type.name());
	}
	
	//Encoding scheme : 32bit = [31-29:type] [28-0:value]
	
	public static class Boolean extends NestingLiteralCollectionExp<java.lang.Boolean> {
		//Type 0 [28:bool][27:1][26-0:0]-- Bit allocation 3,1,1,27
		public Boolean(){
			super(Datatype.BOOL);
		}
		
		@Override
		public void decodeAndAdd(int encodedData) {
			boolean value = ((encodedData >> 28) & 0x1) == 1;
			this.values.add(value);
		}
		
		public static int encode(boolean value){
			return (((value ? 1 : 0) & 0x1) << 28) | (1 << 27);
		}
		
	}
	
	public static class Integer extends NestingLiteralCollectionExp<java.lang.Integer>{
		//Type 1 [28:sign 0=add,1=neg] [27-0:value] -- Bit allocation 3,1,28
		public Integer(){
			super(Datatype.INT);
		}
		
		@Override
		public void decodeAndAdd(int encodedData) {
			boolean isNeg = ((encodedData >> 28) & 0x1) == 1;
			int value = encodedData & 0xFFFFFFF;
			this.values.add(isNeg ? -value : value);
		}
		
		public static int encode(int value){
			boolean isNeg = value < 0;
			int absValue = isNeg ? -value : value;
			
			return ((1 & 0x7) << 29) | (((isNeg ? 1 : 0) & 0x1) << 28) | (absValue & 0xFFFFFFF);
		}
	}
	
	public static class Float extends NestingLiteralCollectionExp<java.lang.Float>{
		//Type 2 [28:sign 0=add,1=neg] [27-4:value] [3-0:floating point] -- Bit allocation 3,1,24,4 
		public Float(){
			super(Datatype.DECIMAL);
		}
		
		@Override
		public void decodeAndAdd(int encodedData) {
			boolean isNeg = ((encodedData >> 28) & 0x1) == 1;
			int intPart = ((encodedData >> 4) & 0xFFFFFF);
			int floatingPoint = encodedData & 0xF;
			this.values.add(constructFloat(isNeg, intPart, floatingPoint));
		}
		
		public static int encode(float value){
			boolean isNeg = value < 0;
			float absValue = isNeg ? -value : value;
			int intPart = (int)absValue;
			int floatingPoint =  getFloatingPointAsInt(absValue-intPart, 1);

			return ((2 & 0x7) << 29) | (((isNeg ? 1 : 0) & 0x1) << 28) | ((intPart & 0xFFFFFF) << 4) | (floatingPoint & 0xF);
		}
	}
	
	public static class Direction extends NestingLiteralCollectionExp<String>{
		//Type 3 [28-26: constant {north,east,south,west,v,h,useValue}] [25:sign][24-7:value] [6-0:floating point]
		//Bit allocation 3,3,1,18,7
		public Direction(){
			super(Datatype.DIRECTION);
		}
		
		@Override
		public void decodeAndAdd(int encodedData) {
			String value;
			int constantType = (encodedData >> 26) & 0x7;
			switch(constantType){
			case 0: value = "north"; break;
			case 1: value = "east"; break;
			case 2: value = "south"; break;
			case 3: value = "west"; break;
			case 4: value = "v"; break;
			case 5: value = "h"; break;
			default:
				boolean isNeg = ((encodedData >> 25) & 1) == 1;
				int intPart = ((encodedData >> 7) & Utility.createBitMask(18));
				int floatingPoint = encodedData & Utility.createBitMask(7);
				value = ""+constructFloat(isNeg, intPart, floatingPoint);
				break;
			}
			this.values.add(value);
		}
		
		public static int encode(String value){
			int constantType = 6;
			if(value.equalsIgnoreCase("north")){
				constantType = 0;
			}else if(value.equalsIgnoreCase("east")){
				constantType = 1;
			}else if(value.equalsIgnoreCase("south")){
				constantType = 2;
			}else if(value.equalsIgnoreCase("west")){
				constantType = 3;
			}else if(value.equalsIgnoreCase("v")){
				constantType = 4;
			}else if(value.equalsIgnoreCase("h")){
				constantType = 5;
			}
			if(constantType == 6){
				float degree = java.lang.Float.parseFloat(value);
				boolean isNeg = degree < 0;
				float absValue = isNeg ? -degree : degree;
				int intPart = (int)absValue;
				int floatingPoint =  getFloatingPointAsInt(absValue-intPart, 2);

				return ((3 & 0x7) << 29) | ((6 & 0x7) << 26) | (((isNeg ? 1 : 0) & 0x1) << 25) | 
						((intPart & Utility.createBitMask(18)) << 7) | (floatingPoint & Utility.createBitMask(7));
						
			}else{
				return ((3 & 0x7) << 29) | ((constantType & 0x7) << 26);
			}
		}
	}
	
	public static class Position extends NestingLiteralCollectionExp<String>{
		/*
		 * Type 4 +X cartesian [28-19:value][18-15:floating point] , [14:sign][13-4:value][3-0:floating point]
		 * Type 5 -X cartesian
		 * Type 6 Polar [28-19:value][18-15:floating point] , [14:sign][13-4:value][3-0:floating point]
		 * 
		 * Bit allocation 3,10,4,1,10,4
		 */
		public Position(){
			super(Datatype.POSITION);
		}
		
		@Override
		public void decodeAndAdd(int encodedData) {
			boolean isPolar = ((encodedData >> 29) & 7) == 6;
			boolean isNeg1 = ((encodedData >> 29) & 7) == 5;
			int intPart1 = (encodedData >> 19) & Utility.createBitMask(10);
			int floatingPoint1 = (encodedData >> 15) & 0xF;
			boolean isNeg2 = ((encodedData >> 14) & 1) == 1;
			int intPart2 = (encodedData >> 4) & Utility.createBitMask(10);
			int floatingPoint2 = encodedData & 0xF;
			String value = (isPolar ? "p(" : "c(") + constructFloat(isNeg1, intPart1, floatingPoint1) + "," + 
					constructFloat(isNeg2, intPart2, floatingPoint2) + ")";
			this.values.add(value);
		}
		
		public static int encode(String value){
			boolean isPolar = value.charAt(0) == 'p';
			float p1 = java.lang.Float.parseFloat(value.substring(2, value.indexOf(',')) );
			boolean isNeg1 = p1 < 0;
			float abs1 = isNeg1 ? -p1 : p1;
			int int1 = (int)abs1;
			int floatingPoint1 =  getFloatingPointAsInt(abs1-int1, 1);
			
			float p2 = java.lang.Float.parseFloat(value.substring(value.indexOf(',')+1, value.length()-1));
			boolean isNeg2 = p2 < 0;
			float abs2 = isNeg2 ? -p2 : p2;
			int int2 = (int)abs2;
			int floatingPoint2 =  getFloatingPointAsInt(abs2-int2, 1);
			
			byte type = (byte)(isPolar ? 6 : (isNeg1 ? 5 : 4)); 

			return ((type & 0x7) << 29) | ((int1 & Utility.createBitMask(10)) << 19) | ((floatingPoint1 & 0xF) << 15) |
					(((isNeg2 ? 1 : 0) & 0x1) << 14) | ((int2 & Utility.createBitMask(10)) << 4) | (floatingPoint2 & 0xF);
		}
	}
	
	public static class Collider extends NestingLiteralCollectionExp<String>{
		//Type 7 [28:0][27-14:width][13-0:height] --- Bit allocation 3,1,14,14
		public Collider(){
			super(Datatype.COLLIDER);
		}
		
		@Override
		public void decodeAndAdd(int encodedData) {
			int width = (encodedData >> 14) & Utility.createBitMask(14);
			int height = encodedData & Utility.createBitMask(14);
			this.values.add(width+","+height);
		}
		
		public static int encode(String value){
			String[] info = value.toString().split(",");
			int width = java.lang.Integer.parseInt(info[0]);
			int height = java.lang.Integer.parseInt(info[1]);
			return ((7 & 0x7) << 29) | ((width & Utility.createBitMask(14)) << 14) | (height & Utility.createBitMask(14));
		}
	}
}
