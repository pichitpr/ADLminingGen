package adl_2daa.gen.generator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.statement.Condition;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.gen.testtool.TestInitializer;
import adl_2daa.tool.Parser;

public class TestASTComparator {

	private static Root root;
	
	@BeforeClass
	public static void setup() throws Exception{
		TestInitializer.init();
		
		//Setup sample root
		List<String> lines = Files.readAllLines(Paths.get("test", "ext", "Sample_interEntity.txt"));
		String script = "";
		for(String line : lines){
			script += line+"\n";
		}
		script = script.trim();
		Parser parser = new Parser();
		root = parser.parse(script);
	}
	
	@Test
	public void TestCondition() throws Exception{
		String testScript = 
				".Sample{"
						+ ".state0{"
							+ ".seq0{"
								+ "if(Abs(DistanceToPlayer(\"X\")) <= 10){"
								+ "}"
								+ "if(Abs(DistanceToPlayer(\"X\")) <= DistanceToPlayer(\"X\")){"
								+ "}"
								+ "if(DistanceToPlayer(\"X\") <= 10){"
								+ "}"
							+ "}"
						+ "}"
				+ "}";
		Parser parser = new Parser();
		Root testRoot = parser.parse(testScript);
		Sequence testSeq = testRoot.getRelatedAgents().get(0).getStates().get(0).getSequences().get(0);
		Sequence sampleSeq = root.getRelatedAgents().get(0).getStates().get(0).getSequences().get(0);
		ASTStatement sampleStatement = sampleSeq.getStatements().get(1);
		assertTrue(
				ASTComparator.astStatementEquals(testSeq.getStatements().get(0), 
						sampleStatement, 2)
				);
		assertFalse(
				ASTComparator.astStatementEquals(testSeq.getStatements().get(0), 
						sampleStatement, 3)
				);
		assertFalse(
				ASTComparator.astStatementEquals(testSeq.getStatements().get(1), 
						sampleStatement, -1)
				);
		
		sampleStatement = ((Condition)sampleStatement).getElseblock().get(3);
		assertTrue(
				ASTComparator.astStatementEquals(testSeq.getStatements().get(2), 
						sampleStatement, 2)
				);
	}
	
	@Test
	public void TestLoop() throws Exception{
		String testScript = 
				".Sample{"
						+ ".state0{"
							+ ".seq0{"
								+ "loop(Random(DecimalSet(1,6,1))){"
								+ "}"
								+ "loop(Random(DecimalSet(1,DistanceToPlayer(\"Y\"),1))){"
								+ "}"
							+ "}"
						+ "}"
				+ "}";
		Parser parser = new Parser();
		Root testRoot = parser.parse(testScript);
		Sequence testSeq = testRoot.getRelatedAgents().get(0).getStates().get(0).getSequences().get(0);
		Sequence sampleSeq = root.getRelatedAgents().get(0).getStates().get(2).getSequences().get(0);
		assertTrue(
				ASTComparator.astStatementEquals(testSeq.getStatements().get(0), 
						sampleSeq.getStatements().get(0), 2)
				);
		assertTrue(
				ASTComparator.astStatementEquals(testSeq.getStatements().get(0), 
						sampleSeq.getStatements().get(0), 3)
				);
		assertTrue(
				ASTComparator.astStatementEquals(testSeq.getStatements().get(0), 
						sampleSeq.getStatements().get(0), -1)
				);
		assertFalse(
				ASTComparator.astStatementEquals(testSeq.getStatements().get(1), 
						sampleSeq.getStatements().get(0), -1)
				);
	}
}
