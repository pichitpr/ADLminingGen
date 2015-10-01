package adl_2daa.gen.signature;

import java.util.List;

public class Utility {

	public static byte[] toByteArray(List<Byte> list){
		byte[] ary = new byte[list.size()];
		for(int i=0; i<ary.length; i++){
			ary[i] = list.get(i);
		}
		return ary;
	}
	
	public static <T> void addArrayToList(List<T> list, T[] ary){
		for(T element : ary){
			list.add(element);
		}
	}
}
