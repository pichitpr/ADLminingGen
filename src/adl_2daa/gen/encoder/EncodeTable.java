package adl_2daa.gen.encoder;

public class EncodeTable {

	//Unique ID
	public static final byte COND_IF = 127;
	public static final byte COND_IF_IFELSE = 126;
	public static final byte COND_ELSE_IFELSE = 125;
	public static final byte LOOP = 124;
	public static final byte EXP_UNARY_NOT = 123;
	public static final byte EXP_UNARY_NEG = 122;
	public static final byte EXP_BINARY = 121;
	public static final byte EXP_FUNCTION = 120;
	public static final byte EXP_LITERAL = 119;
	
	//EXP_BINARY family
	public static final byte EXP_BINARY_AND = 0;
	public static final byte EXP_BINARY_OR = 1;
	public static final byte EXP_BINARY_COMP_EQ = 10;
	public static final byte EXP_BINARY_COMP_NEQ = 11;
	public static final byte EXP_BINARY_COMP_GT = 12;
	public static final byte EXP_BINARY_COMP_LT = 13;
	public static final byte EXP_BINARY_COMP_GE = 14;
	public static final byte EXP_BINARY_COMP_LE = 15;
	public static final byte EXP_BINARY_ARITH_ADD = 20;
	public static final byte EXP_BINARY_ARITH_SUB = 21;
	public static final byte EXP_BINARY_ARITH_MUL = 22;
	public static final byte EXP_BINARY_ARITH_DIV = 23;
	public static final byte EXP_BINARY_ARITH_MOD = 24;
	
	//ID offset for mixing actionID and functionID under the same order
	public static final int idOffset = 128;
	
	//===========================================
	
	public static final int STATE_SEQUENCE_EDGE = 0;
	public static final int SEQUENCE_ACTION_EDGE = 1;
	public static final int SEQUENCE_ACTION_OTHER_ENTITY_EDGE = 2;
	public static final int TAG_EDGE = -1;
	
	//===========================================
	
	//Support ID up to 127 (0..127)
	public static byte[] encodeSignatureID(int id){
		boolean minus = id < 0;
		byte[] data = new byte[2];
		data[0] = (byte)(minus ? 1 : 0);
		data[1] = (byte)(minus ? -id : id);
		return data;
	}
	
	public static int decodeSignatureID(byte b0, byte b1){
		return b0 == 1 ? -(int)b1 : b1;
	}
}
