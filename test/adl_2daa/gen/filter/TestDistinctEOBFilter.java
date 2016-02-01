package adl_2daa.gen.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import adl_2daa.ast.ASTExpression;
import adl_2daa.ast.expression.Identifier;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.structure.Root;
import adl_2daa.gen.generator.ExpressionSkeleton;
import adl_2daa.gen.signature.Datatype;
import adl_2daa.gen.testtool.TestUtility;
import adl_2daa.tool.Parser;

public class TestDistinctEOBFilter {

	//Test only "tryMatch=true"
	private List<ResultAgent> filter(List<Action> transitions, boolean eosOnly) throws Exception{
		List<String> lines = Files.readAllLines(Paths.get("test", "ext", "Sample.txt"));
		String script = "";
		for(String line : lines){
			script += line+"\n";
		}
		script = script.trim();
		Parser parser = new Parser();
		Root root = parser.parse(script);
		
		List<ResultAgent> skel = ASTFilterOperator.filterAgent(root.getRelatedAgents(), 
				null,  null, null);
		boolean[] eosOnlyAry = new boolean[transitions.size()];
		for(int i=0; i<eosOnlyAry.length; i++){
			eosOnlyAry[i] = eosOnly;
		}
		List<ResultAgent> result = ASTFilterOperator.filterDistinctEOBTransitionFitting(
				skel, transitions, eosOnlyAry, true);
		return result;
	}
	
	private void assertSetEqual(String[] expectedAry, List<String> actual){
		List<String> expected = new LinkedList<String>();
		for(String str : expectedAry){
			expected.add(str);
		}
		assertEquals(expected.size(), actual.size());
		for(String actualStr : actual){
			assertTrue("NOT FOUND: "+actualStr, expected.contains(actualStr));
			expected.remove(actualStr);
		}
	}
	
	@Test
	public void singleGoto(){
		List<Action> transitions = new LinkedList<Action>();
		transitions.add(createGoto());
		try {
			assertSetEqual(new String[]{
					"Sample.state1.seq0",
					"Sample.state1.seq1",
					"Sample.state3.seq0",
			}, TestUtility.enumerateResultAgentsAsString( filter(transitions, true) ));
			
			assertSetEqual(new String[]{
					"Sample.state1.seq0",
					"Sample.state1.seq1",
					"Sample.state2.seq0",
					"Sample.state3.seq0",
			}, TestUtility.enumerateResultAgentsAsString( filter(transitions, false) ));
		} catch (Exception e) {
			fail();
		}
	}
	
	@Test
	public void doubleGoto(){
		List<Action> transitions = new LinkedList<Action>();
		transitions.add(createGoto());
		transitions.add(createGoto());
		
		try {
			assertSetEqual(new String[]{
					"Sample.state1.seq0 seq1",
					"Sample.state1.seq1 seq0",
			}, TestUtility.enumerateResultAgentsAsString( filter(transitions, true) ));
			
			assertSetEqual(new String[]{
					"Sample.state1.seq0 seq1",
					"Sample.state1.seq1 seq0",
			}, TestUtility.enumerateResultAgentsAsString( filter(transitions, false) ));
		} catch (Exception e) {
			fail();
		}
	}
	
	@Test
	public void singleDespawn(){
		List<Action> transitions = new LinkedList<Action>();
		transitions.add(createDespawn());
		
		try {
			assertSetEqual(new String[]{
					"Sample.state1.seq0",
					"Sample.state1.seq1",
					"Sample.state2.seq0",
					"Sample.state3.seq0",
			}, TestUtility.enumerateResultAgentsAsString( filter(transitions, true) ));
			
			assertSetEqual(new String[]{
					"Sample.state1.seq0",
					"Sample.state1.seq1",
					"Sample.state2.seq0",
					"Sample.state3.seq0",
			}, TestUtility.enumerateResultAgentsAsString( filter(transitions, false) ));
		} catch (Exception e) {
			fail();
		}
	}
	
	@Test
	public void mixed(){
		List<Action> transitions = new LinkedList<Action>();
		transitions.add(createGoto());
		transitions.add(createDespawn());
		
		try {
			assertSetEqual(new String[]{
					"Sample.state1.seq0 seq1",
					"Sample.state1.seq1 seq0",
			}, TestUtility.enumerateResultAgentsAsString( filter(transitions, true) ));
			
			assertSetEqual(new String[]{
					"Sample.state1.seq0 seq1",
					"Sample.state1.seq1 seq0",
			}, TestUtility.enumerateResultAgentsAsString( filter(transitions, false) ));
		} catch (Exception e) {
			fail();
		}
	}
	
	@Test
	public void threeTransition(){
		List<Action> transitions = new LinkedList<Action>();
		transitions.add(createGoto());
		transitions.add(createDespawn());
		transitions.add(createGoto());
		
		try {
			assertSetEqual(new String[]{
					"Sample.state1.seq0 seq1 null",
					"Sample.state1.seq0 null seq1",
					"Sample.state1.seq1 seq0 null",
					"Sample.state1.seq1 null seq0",
					"Sample.state1.null seq0 seq1",
					"Sample.state1.null seq1 seq0",
			}, TestUtility.enumerateResultAgentsAsString( filter(transitions, true) ));
			
			assertSetEqual(new String[]{
					"Sample.state1.seq0 seq1 null",
					"Sample.state1.seq0 null seq1",
					"Sample.state1.seq1 seq0 null",
					"Sample.state1.seq1 null seq0",
					"Sample.state1.null seq0 seq1",
					"Sample.state1.null seq1 seq0",
			}, TestUtility.enumerateResultAgentsAsString( filter(transitions, false) ));
		} catch (Exception e) {
			fail();
		}
	}
	
	@Test
	public void noSolution() throws Exception{
		List<Action> transitions = new LinkedList<Action>();
		List<ASTExpression> params = new LinkedList<ASTExpression>();
		params.add(new Identifier(".UnknownState"));
		transitions.add(new Action("Goto", params));
		
		String sampleScript = 
				".Sample{"
					+ ".state0{"
						+ ".seq0{"
							+ "Goto(.state0);"
						+ "}"
					+ "}"
					+ ".state1{"
						+ ".seq0{"
							+ "Goto(.state0);"
						+ "}"
					+ "}"
					+ ".state2{"
						+ ".seq0{"
							+ "Goto(.state0);"
						+ "}"
					+ "}"
				+ "}";
		Parser parser = new Parser();
		Root sampleRoot = parser.parse(sampleScript);
		
		List<ResultAgent> skel = ASTFilterOperator.filterAgent(sampleRoot.getRelatedAgents(), 
				null,  null, null);
		List<ResultAgent> sampleResult = ASTFilterOperator.filterDistinctEOBTransitionFitting(
				skel, transitions, new boolean[]{true}, true);
		
		try {
			assertSetEqual(new String[]{
			}, TestUtility.enumerateResultAgentsAsString( sampleResult ));
		} catch (Exception e) {
			fail();
		}
		
		sampleResult = ASTFilterOperator.filterDistinctEOBTransitionFitting(
				skel, transitions, new boolean[]{false}, true);
		
		try {
			assertSetEqual(new String[]{
			}, TestUtility.enumerateResultAgentsAsString( sampleResult ));
		} catch (Exception e) {
			fail();
		}
	}
	
	private static Action createGoto(){
		List<ASTExpression> params = new LinkedList<ASTExpression>();
		params.add(new ExpressionSkeleton(Datatype.IDENTIFIER));
		return new Action("Goto", params);
	}
	
	private static Action createDespawn(){
		List<ASTExpression> params = new LinkedList<ASTExpression>();
		return new Action("Despawn", params);
	}
}
