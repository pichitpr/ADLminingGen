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
		int value = NestingLiteralCollectionExp.Boolean.encode(true);
		assertEquals(binaryStringToDec("0001 1000 0000 0000 0000 0000 0000 0000"), value);
		value = NestingLiteralCollectionExp.Boolean.encode(false);
		assertEquals(binaryStringToDec("0000 1000 0000 0000 0000 0000 0000 0000"), value);
	}
	
	@Test
	public void testInt(){
		int value = NestingLiteralCollectionExp.Integer.encode(0);
		assertEquals(binaryStringToDec("0010 0000 0000 0000 0000 0000 0000 0000"), value);
		value = NestingLiteralCollectionExp.Integer.encode(-1023);
		assertEquals(binaryStringToDec("0011 0000 0000 0000 0000 0011 1111 1111"), value);
		value = NestingLiteralCollectionExp.Integer.encode(542);
		assertEquals(binaryStringToDec("0010 0000 0000 0000 0000 0010 0001 1110"), value);
	}
	
	@Test
	public void testFloat(){
		int value = NestingLiteralCollectionExp.Float.encode(12.0f);
		assertEquals(binaryStringToDec("0100 0000 0000 0000 0000 0000 1100 0000"), value);
		value = NestingLiteralCollectionExp.Float.encode(123456.78f);
		assertEquals(binaryStringToDec("0100 0000 0001 1110 0010 0100 0000 0111"), value);
		value = NestingLiteralCollectionExp.Float.encode(-9100.632f);
		assertEquals(binaryStringToDec("0101 0000 0000 0010 0011 1000 1100 0110"), value);
	}
	
	@Test
	public void testDirection(){
		int value = NestingLiteralCollectionExp.Direction.encode("56.758");
		assertEquals(binaryStringToDec("011 110 0 00 0000 0000 0011 1000 100 1011"), value);
		value = NestingLiteralCollectionExp.Direction.encode("h");
		assertEquals(binaryStringToDec("011 101 0 00 0000 0000 0000 0000 000 0000"), value);
		value = NestingLiteralCollectionExp.Direction.encode("NoRth");
		assertEquals(binaryStringToDec("011 000 0 00 0000 0000 0000 0000 000 0000"), value);
		value = NestingLiteralCollectionExp.Direction.encode("-1554.1213");
		assertEquals(binaryStringToDec("011 110 1 00 0000 0110 0001 0010 000 1100"), value);
	}
	
	@Test
	public void testPosition(){
		int value = NestingLiteralCollectionExp.Position.encode("c(100.61,-500.49)");
		assertEquals(binaryStringToDec("100 00 0110 0100 0110 1 01 1111 0100 0100"), value);
		value = NestingLiteralCollectionExp.Position.encode("c(-1100.61,220.888)");
		assertEquals(binaryStringToDec("101 00 0100 1100 0110 0 00 1101 1100 1000"), value);
		value = NestingLiteralCollectionExp.Position.encode("p(1200.1,250.35)");
		assertEquals(binaryStringToDec("110 00 1011 0000 0001 0 00 1111 1010 0011"), value);
		value = NestingLiteralCollectionExp.Position.encode("p(-1200,250.35)");
		assertEquals(binaryStringToDec("110 00 1011 0000 0000 0 00 1111 1010 0011"), value);
	}
	
	@Test
	public void testCollider(){
		int value = NestingLiteralCollectionExp.Collider.encode("500,300");
		assertEquals(binaryStringToDec("111 0 00 0001 1111 0100 00 0001 0010 1100"), value);
	}
}
