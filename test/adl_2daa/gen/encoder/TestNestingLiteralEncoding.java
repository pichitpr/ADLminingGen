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
		for(int i=0; i<encoded.length; i++){
			assertEquals(encoded[i], NestingLiteralCollectionExp.Boolean.encode(decoded[i]));
			assertEquals(decoded[i], NestingLiteralCollectionExp.Boolean.decode(encoded[i]).isValue());
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
		for(int i=0; i<encoded.length; i++){
			assertEquals(encoded[i], NestingLiteralCollectionExp.Integer.encode(decoded[i]));
			assertEquals(decoded[i], NestingLiteralCollectionExp.Integer.decode(encoded[i]).getValue());
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
		for(int i=0; i<encoded.length; i++){
			assertEquals(encoded[i], NestingLiteralCollectionExp.Float.encode(decoded[i]));
			//Value is trimmed until 1 floating point left
			assertEquals(decoded[i], NestingLiteralCollectionExp.Float.decode(encoded[i]).getValue(), 0.1f);
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
		for(int i=0; i<encoded.length; i++){
			assertEquals(encoded[i], NestingLiteralCollectionExp.Direction.encode(value[i]));
			assertEquals(decoded[i], NestingLiteralCollectionExp.Direction.decode(encoded[i]).getValue());
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
		for(int i=0; i<encoded.length; i++){
			assertEquals(encoded[i], NestingLiteralCollectionExp.Position.encode(value[i]));
			assertEquals(decoded[i], NestingLiteralCollectionExp.Position.decode(encoded[i]).getValue());
		}
	}
	
	@Test
	public void testCollider(){
		int value = NestingLiteralCollectionExp.Collider.encode("500,300");
		assertEquals(binaryStringToDec("111 0 00 0001 1111 0100 00 0001 0010 1100"), value);
		assertEquals("500,300", 
				NestingLiteralCollectionExp.Collider.decode( binaryStringToDec("111 0 00 0001 1111 0100 00 0001 0010 1100") ).getValue()
				);
	}
}
