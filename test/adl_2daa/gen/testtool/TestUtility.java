package adl_2daa.gen.testtool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import adl_2daa.gen.encoder.EncodeTable;

public class TestUtility {

	public static String byteArrayToString0(byte[] ary, boolean decode){
		String s = "";
		for(int i=0; i<ary.length; i++){
			if(decode)
				s += decode0(ary[i])+" ";
			else
				s += ary[i]+" ";
		}
		return s.trim();
	}
	
	public static String decode0(byte b){
		if(b == 127) return "|";
		else if(b >= 121) return "#"+(b-121);
		else if(b >= 101) return "d"+(b-101);
		else if(b == 95) return "Lit";
		else if(b == 94) return "Un";
		else if(b == 85) return "Cmp";
		else if(b == 75) return "Math";
		else if(b == 73) return "Lg";
		else if(b == 72) return "Else";
		else if(b == 71) return "If-E";
		else if(b == 70) return "If";
		else return ""+b;
	}
	
	public static String actionToString0(byte actionID, List<byte[]> nestingConditions,
			boolean decode){
		String s = ""+actionID;
		for(int i=0; i<nestingConditions.size(); i++){
			s += (decode ? " | " : " 127 ")+
					TestUtility.byteArrayToString0(nestingConditions.get(i), decode);
		}
		return s;
	}
	
	//======================================================
	
	public static String stringToByteString(String str){
		String s = "";
		for(byte b : str.getBytes(StandardCharsets.US_ASCII)){
			switch(b){
			case EncodeTable.COND_IF: s += "if "; break;
			case EncodeTable.COND_IF_IFELSE: s += "if-e "; break;
			case EncodeTable.COND_ELSE_IFELSE: s += "else "; break;
			case EncodeTable.EXP_BINARY: s += "bin "; break;
			case EncodeTable.EXP_UNARY_NEG: s += "- "; break;
			case EncodeTable.EXP_UNARY_NOT: s += "! "; break;
			case EncodeTable.EXP_FUNCTION: s += "func "; break;
			case EncodeTable.EXP_LITERAL: s += "lit "; break;
			default:
				s += b+" ";
			}
		}
		return s.trim();
	}
	
	public static String readFileAsString(String dir) throws Exception{
		return readFileAsString(new File(dir));
	}
	
	public static String readFileAsString(File f) throws Exception{
		BufferedReader reader = new BufferedReader(new FileReader(f));
		String str = "";
		String line;
		while((line=reader.readLine())!=null){
			str += line+"\n";
		}
		str = str.trim();
		reader.close();
		return str;
	}
}
