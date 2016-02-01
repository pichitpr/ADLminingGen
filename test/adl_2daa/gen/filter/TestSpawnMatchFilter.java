package adl_2daa.gen.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import adl_2daa.ast.ASTExpression;
import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.expression.Function;
import adl_2daa.ast.expression.Identifier;
import adl_2daa.ast.expression.StringConstant;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.statement.Condition;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.gen.generator.ExpressionSkeleton;
import adl_2daa.gen.signature.Datatype;
import adl_2daa.gen.signature.FunctionMainSignature;
import adl_2daa.gen.signature.GeneratorRegistry;
import adl_2daa.gen.testtool.TestInitializer;
import adl_2daa.gen.testtool.TestUtility;
import adl_2daa.tool.Parser;

public class TestSpawnMatchFilter {

	@BeforeClass
	public static void SetupTest(){
		TestInitializer.init();
	}
	
	private List<ResultAgent> filter(List<List<ASTStatement>> relation) throws Exception{
		List<String> lines = Files.readAllLines(Paths.get("test", "ext", "Sample_interEntity.txt"));
		String script = "";
		for(String line : lines){
			script += line+"\n";
		}
		script = script.trim();
		Parser parser = new Parser();
		Root root = parser.parse(script);
		
		List<ResultAgent> skel = ASTFilterOperator.filterAgent(root.getRelatedAgents(), 
				null,  null, null);
		List<ResultAgent> result = ASTFilterOperator.filterHighestSpawnMatch(skel, relation);
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
	
	//@Test
	public void singleSpawn(){
		List<List<ASTStatement>> relation = new LinkedList<List<ASTStatement>>();
		List<ASTStatement> relSeq = new LinkedList<ASTStatement>();
		relSeq.add(createSpawn(null));
		relation.add(relSeq);
		
		try {
			assertSetEqual(new String[]{
					"Sample.state2.seq0",
					"Sample.state3.seq0",
					"Bullet3.state0.seq0",
					"Bullet3.state0.seq1",
					"Bullet4.state0.seq0",
			}, TestUtility.enumerateResultAgentsAsString( filter(relation) ));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		
		relation.clear();
		relSeq.clear();
		relSeq.add(createSpawn(".Bullet1"));
		relation.add(relSeq);
		try {
			assertSetEqual(new String[]{
					"Sample.state2.seq0",
					"Sample.state3.seq0",
					"Bullet3.state0.seq0",
					"Bullet3.state0.seq1",
			}, TestUtility.enumerateResultAgentsAsString( filter(relation) ));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	//@Test
	public void doubleSpawn(){
		List<List<ASTStatement>> relation = new LinkedList<List<ASTStatement>>();
		List<ASTStatement> relSeq = new LinkedList<ASTStatement>();
		relSeq.add(createSpawn(null));
		relSeq.add(createSpawn(null));
		relation.add(relSeq);
		
		try {
			assertSetEqual(new String[]{
					"Bullet3.state0.seq0",
					"Bullet4.state0.seq0",
			}, TestUtility.enumerateResultAgentsAsString( filter(relation) ));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	//@Test
	public void singleIfNestedSpawn() throws Exception{
		List<List<ASTStatement>> relation = new LinkedList<List<ASTStatement>>();
		
		//if(Random(DecimalSet(1,2,1)) == 1){ Spawn(?) }
		String testScript = 
				".Sample{"
						+ ".state0{"
							+ ".seq0{"
								+ "if(Random(DecimalSet(1,2,1)) == 1){"
									+ "Spawn(.Dummy);"
								+ "}"
							+ "}"
						+ "}"
				+ "}";
		Parser parser = new Parser();
		Root testRoot = parser.parse(testScript);
		Sequence testSeq = testRoot.getRelatedAgents().get(0).getStates().get(0).getSequences().get(0);
		relation.add(testSeq.getStatements());
		//Modify Spawn(IDEN) to Spawn(IDEN_?)
		Condition cond = (Condition)testSeq.getStatements().get(0);
		Action action = (Action)cond.getIfblock().get(0);
		action.getParams()[0] = new ExpressionSkeleton(Datatype.IDENTIFIER);
		
		try {
			assertSetEqual(new String[]{
					"Sample.state2.seq0",
					"Bullet2.state0.seq0",
			}, TestUtility.enumerateResultAgentsAsString( filter(relation) ));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	//@Test
	public void singleElseNestedSpawn() throws Exception{
		List<List<ASTStatement>> relation = new LinkedList<List<ASTStatement>>();
		
		//if(Random(DecimalSet(1,2,1)) == 1){ Spawn(?) }
		String testScript = 
				".Sample{"
						+ ".state0{"
							+ ".seq0{"
								+ "if(Random(DecimalSet(1,2,1)) == 1){"
								+ "}else{"
									+ "Spawn(.Bullet1);"
								+ "}"
							+ "}"
						+ "}"
				+ "}";
		Parser parser = new Parser();
		Root testRoot = parser.parse(testScript);
		Sequence testSeq = testRoot.getRelatedAgents().get(0).getStates().get(0).getSequences().get(0);
		relation.add(testSeq.getStatements());
		
		try {
			assertSetEqual(new String[]{
					"Sample.state2.seq0",
					"Bullet2.state0.seq0",
			}, TestUtility.enumerateResultAgentsAsString( filter(relation) ));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	//@Test
	public void ifElseNestedSpawn() throws Exception{
		List<List<ASTStatement>> relation = new LinkedList<List<ASTStatement>>();
		
		//if(Abs <= Int)else{ if(DistanceToPlayer <= Int){ Spawn(?) } }
		String testScript = 
				".Sample{"
						+ ".state0{"
							+ ".seq0{"
								+ "if(Abs(DistanceToPlayer(\"X\")) <= 5){"
								+ "}else{"
									+ "if(DistanceToPlayer(\"X\") <= 300){"
										+ "Spawn(.Dummy);"
									+ "}"
								+ "}"
							+ "}"
						+ "}"
				+ "}";
		Parser parser = new Parser();
		Root testRoot = parser.parse(testScript);
		Sequence testSeq = testRoot.getRelatedAgents().get(0).getStates().get(0).getSequences().get(0);
		relation.add(testSeq.getStatements());
		//Modify Spawn(IDEN) to Spawn(IDEN_?)
		Condition outerCond = (Condition)testSeq.getStatements().get(0);
		Condition innerCond = (Condition)outerCond.getElseblock().get(0);
		Action action = (Action)innerCond.getIfblock().get(0);
		action.getParams()[0] = new ExpressionSkeleton(Datatype.IDENTIFIER);
		
		try {
			assertSetEqual(new String[]{
					"Sample.state1.seq0",
			}, TestUtility.enumerateResultAgentsAsString( filter(relation) ));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	//@Test
	public void parallelSpawn(){
		List<List<ASTStatement>> relation = new LinkedList<List<ASTStatement>>();
		//Spawn(.Bullet1) || Spawn(IDEN_?)
		List<ASTStatement> relSeq = new LinkedList<ASTStatement>();
		relSeq.add(createSpawn(null));
		relation.add(relSeq);
		relSeq = new LinkedList<ASTStatement>();
		relSeq.add(createSpawn(".Bullet1"));
		relation.add(relSeq);
		
		try {
			assertSetEqual(new String[]{
					"Bullet3.state0.seq0 seq1",
					"Bullet3.state0.seq1 seq0",
			}, TestUtility.enumerateResultAgentsAsString( filter(relation) ));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	@Test
	public void noMatch(){
		List<List<ASTStatement>> relation = new LinkedList<List<ASTStatement>>();
		//{Spawn(.AAA), Spawn(.XXX)} || Spawn(.YYY)
		List<ASTStatement> relSeq = new LinkedList<ASTStatement>();
		relSeq.add(createSpawn(".AAA"));
		relSeq.add(createSpawn(".XXX"));
		relation.add(relSeq);
		relSeq = new LinkedList<ASTStatement>();
		relSeq.add(createSpawn(".YYY"));
		relation.add(relSeq);
		
		try {
			assertSetEqual(new String[]{
			}, TestUtility.enumerateResultAgentsAsString( filter(relation) ));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	public Action createSpawn(String identifier){
		List<ASTExpression> params = new LinkedList<ASTExpression>();
		if(identifier == null)
			params.add(new ExpressionSkeleton(Datatype.IDENTIFIER));
		else
			params.add(new Identifier(identifier));
		return new Action("Spawn", params);
	}
	
	public Function createFunction(String name, String choice){
		FunctionMainSignature fsig = GeneratorRegistry.getFunctionSignature(name);
		List<ASTExpression> params = new LinkedList<ASTExpression>();
		Datatype[] paramType = fsig.getMainSignature().getParamType();
		if(fsig.hasChoice()){
			params.add(new StringConstant(choice));
			paramType = fsig.getChoiceSignature(choice).getParamType();
		}
		for(Datatype type : paramType){
			params.add(new ExpressionSkeleton(type));
		}
		//Omit single query (go with NO single query)
		return new Function(name, params, false);
	}
}
