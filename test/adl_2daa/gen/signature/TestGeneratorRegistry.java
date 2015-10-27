package adl_2daa.gen.signature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import adl_2daa.gen.testtool.TestInitializer;

public class TestGeneratorRegistry {

	private boolean setup = false;
	private Field actionSignatureMap = null;
	private Field functionSignatureMap = null;
	
	@Before
	public void setup(){
		if(setup) return;
		try {
			actionSignatureMap = GeneratorRegistry.class.getDeclaredField("actionSignatureMap");
			actionSignatureMap.setAccessible(true);
			functionSignatureMap = GeneratorRegistry.class.getDeclaredField("functionSignatureMap");
			functionSignatureMap.setAccessible(true);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		TestInitializer.init();
		setup = true;
	}
	
	@Test
	public void testActionIDUniqueness(){
		try {
			HashSet<Integer> idBasket = new HashSet<Integer>();
			int counter = 0;
			@SuppressWarnings({ "rawtypes", "unchecked" })
			HashMap<String,ActionMainSignature> map = (HashMap)actionSignatureMap.get(null);
			for(ActionMainSignature sig : map.values()){
				idBasket.add(sig.mainSignature.id);
				counter++;
				if(sig.hasChoice){
					for(Signature choiceSig : sig.choiceSigMap.values()){
						idBasket.add(choiceSig.id);
						counter++;
					}
				}
			}
			assertEquals(counter, idBasket.size());
			assertTrue(counter <= 127);
		} catch (Exception ex) {
			ex.printStackTrace();
			fail("Error occurs");
		}
	}
	
	@Test
	public void testFunctionIDUniqueness(){
		try {
			HashSet<Integer> idBasket = new HashSet<Integer>();
			int counter = 0;
			@SuppressWarnings({ "rawtypes", "unchecked" })
			HashMap<String,FunctionMainSignature> map = (HashMap)functionSignatureMap.get(null);
			for(FunctionMainSignature sig : map.values()){
				idBasket.add(sig.mainSignature.id);
				counter++;
				if(sig.hasChoice){
					for(Signature choiceSig : sig.choiceSigMap.values()){
						idBasket.add(choiceSig.id);
						counter++;
					}
				}
			}
			assertEquals(counter, idBasket.size());
			assertTrue(counter <= 127);
		} catch (Exception ex) {
			ex.printStackTrace();
			fail("Error occurs");
		}
	}
}
