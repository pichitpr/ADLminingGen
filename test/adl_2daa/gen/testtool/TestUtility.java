package adl_2daa.gen.testtool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

public class TestUtility {

	public static String byteArrayToString(byte[] ary, boolean decode){
		String s = "";
		for(int i=0; i<ary.length; i++){
			if(decode)
				s += decode(ary[i])+" ";
			else
				s += ary[i]+" ";
		}
		return s.trim();
	}
	
	public static String decode(byte b){
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
	
	public static String actionToString(byte actionID, List<byte[]> nestingConditions,
			boolean decode){
		String s = ""+actionID;
		for(int i=0; i<nestingConditions.size(); i++){
			s += (decode ? " | " : " 127 ")+
					TestUtility.byteArrayToString(nestingConditions.get(i), decode);
		}
		return s;
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
