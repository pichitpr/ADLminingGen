package adl_2daa.gen.encoder;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestNestingLiteralEncoding {

	private int binaryStringToDec(String str){
		str = str.replaceAll(" ", "");
		return Integer.parseUnsignedInt(str,2);
	}
	
	@Test
	public void testBoolean(){
		int[] encoded = new int[]{
				binaryStringToDec("0001 1000 0000 0000 0000 0000 0000 0000"),
				binaryStringToDec("0000 1000 0000 0000 0000 0000 0000 0000")
		};
		boolean[] decoded = new boolean[]{
			true,
			false
		};
		NestingLiteralCollectionExp.Boolean dBoolean = new NestingLiteralCollectionExp.Boolean();
		for(int i=0; i<encoded.length; i++){
			assertEquals(encoded[i], NestingLiteralCollectionExp.Boolean.encode(decoded[i]));
			dBoolean.decodeAndAdd(encoded[i]);
			assertEquals(decoded[i], dBoolean.values.get(i));
		}
	}
	
	@Test
	public void testInt(){
		int[] encoded = new int[]{
				binaryStringToDec("0010 0000 0000 0000 0000 0000 0000 0000"),
				binaryStringToDec("0011 0000 0000 0000 0000 0011 1111 1111"),
				binaryStringToDec("0010 0000 0000 0000 0000 0010 0001 1110")
		};
		int[] decoded = new int[]{
			0,
			-1023,
			542
		};
		NestingLiteralCollectionExp.Integer dInteger = new NestingLiteralCollectionExp.Integer();
		for(int i=0; i<encoded.length; i++){
			assertEquals(encoded[i], NestingLiteralCollectionExp.Integer.encode(decoded[i]));
			dInteger.decodeAndAdd(encoded[i]);
			assertEquals(decoded[i], (int)dInteger.values.get(i));
		}
	}
	
	@Test
	public void testFloat(){
		int[] encoded = new int[]{
				binaryStringToDec("0100 0000 0000 0000 0000 0000 1100 0000"),
				binaryStringToDec("0100 0000 0001 1110 0010 0100 0000 0111"),
				binaryStringToDec("0101 0000 0000 0010 0011 1000 1100 0110")
		};
		float[] decoded = new float[]{
			12.0f,
			123456.78f,
			-9100.632f
		};
		NestingLiteralCollectionExp.Float dFloat = new NestingLiteralCollectionExp.Float();
		for(int i=0; i<encoded.length; i++){
			assertEquals(encoded[i], NestingLiteralCollectionExp.Float.encode(decoded[i]));
			dFloat.decodeAndAdd(encoded[i]);
			assertEquals(decoded[i], dFloat.values.get(i), 0.1f); //Value is trimmed until 1 floating point left
		}
		
	}
	
	@Test
	public void testDirection(){
		int[] encoded = new int[]{
				binaryStringToDec("011 110 0 00 0000 0000 0011 1000 100 1011"),
				binaryStringToDec("011 101 0 00 0000 0000 0000 0000 000 0000"),
				binaryStringToDec("011 000 0 00 0000 0000 0000 0000 000 0000"),
				binaryStringToDec("011 110 1 00 0000 0110 0001 0010 000 1100")
		};
		String[] value = new String[]{
				"56.758",
				"h",
				"NoRth",
				"-1554.1213"
		};
		String[] decoded = new String[]{
				"56.75",
				"h",
				"north",
				"-1554.12"
		};
		NestingLiteralCollectionExp.Direction dDirection = new NestingLiteralCollectionExp.Direction();
		for(int i=0; i<encoded.length; i++){
			assertEquals(encoded[i], NestingLiteralCollectionExp.Direction.encode(value[i]));
			dDirection.decodeAndAdd(encoded[i]);
			assertEquals(decoded[i], dDirection.values.get(i));
		}
	}
	
	@Test
	public void testPosition(){
		int[] encoded = new int[]{
				binaryStringToDec("100 00 0110 0100 0110 1 01 1111 0100 0100"),
				binaryStringToDec("101 00 0100 1100 0110 0 00 1101 1100 1000"),
				binaryStringToDec("110 00 1011 0000 0001 0 00 1111 1010 0011"),
				binaryStringToDec("110 00 1011 0000 0000 0 00 1111 1010 0011")
		};
		String[] value = new String[]{
				"c(100.61,-500.49)",
				"c(-1100.61,220.888)",
				"p(1200.1,250.35)",
				"p(-1200,250.35)"
		};
		String[] decoded = new String[]{
			"c(100.6,-500.4)",
			"c(-76.6,220.8)",
			"p(176.1,250.3)",
			"p(176.0,250.3)"
		};
		NestingLiteralCollectionExp.Position dPosition = new NestingLiteralCollectionExp.Position();
		for(int i=0; i<encoded.length; i++){
			assertEquals(encoded[i], NestingLiteralCollectionExp.Position.encode(value[i]));
			dPosition.decodeAndAdd(encoded[i]);
			assertEquals(decoded[i], dPosition.values.get(i));
		}
	}
	
	@Test
	public void testCollider(){
		int value = NestingLiteralCollectionExp.Collider.encode("500,300");
		assertEquals(binaryStringToDec("111 0 00 0001 1111 0100 00 0001 0010 1100"), value);
		NestingLiteralCollectionExp.Collider dCollider = new NestingLiteralCollectionExp.Collider();
		dCollider.decodeAndAdd( binaryStringToDec("111 0 00 0001 1111 0100 00 0001 0010 1100") );
		assertEquals("500,300", dCollider.values.get(0));
	}
}
