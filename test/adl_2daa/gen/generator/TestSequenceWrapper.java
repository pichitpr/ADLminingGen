package adl_2daa.gen.generator;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import lcs.LCSSequenceEmbedding;
import lcs.SimpleLCSEmbedding;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import adl_2daa.ast.statement.Action;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.gen.signature.Datatype;
import adl_2daa.gen.testtool.TestInitializer;
import adl_2daa.tool.Parser;

public class TestSequenceWrapper {

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
	
	//@Test
	public void TestLCSSpawnComparator() throws Exception{
		//.Sample.state2.seq0
		Sequence sampleSeq = root.getRelatedAgents().get(0).getStates().get(1)
				.getSequences().get(0);
		ASTSequenceWrapper wrappedSample = new ASTSequenceWrapper(sampleSeq.getStatements());
		
		//The relation (testSeq) intentionally include if-block to test for wrapper's action index 
		String testScript = 
				".Sample{"
						+ ".state0{"
							+ ".seq0{"
								+ "Spawn(.Bullet1);"
								+ "if(Abs(DistanceToPlayer(\"X\")) <= 10){"
								+ "}"
								+ "Spawn(.Bullet1);"
							+ "}"
						+ "}"
				+ "}";
		Parser parser = new Parser();
		Root testRoot = parser.parse(testScript);
		Sequence testSeq = testRoot.getRelatedAgents().get(0).getStates().get(0)
				.getSequences().get(0);
		//Modify param to expSkel
		Action action = (Action)testSeq.getStatements().get(2);
		action.getParams()[0] = new ExpressionSkeleton(Datatype.IDENTIFIER);
		ASTSequenceWrapper wrappedTest = new ASTSequenceWrapper(testSeq.getStatements());

		Set<LCSSequenceEmbedding<ASTNode>> lcsResult =
				SimpleLCSEmbedding.allLCSEmbeddings(wrappedTest, wrappedSample, 
						ASTSequenceWrapper.spawnComparator);
		assertEquals(2, lcsResult.size());
		assertEquals(1, lcsResult.iterator().next().size());
	}
	
	@Test
	public void TestLCSSpawnComparatorWithNesting() throws Exception{
		//.Sample.state1.seq0
		Sequence sampleSeq = root.getRelatedAgents().get(0).getStates().get(0)
				.getSequences().get(0);
		ASTSequenceWrapper wrappedSample = new ASTSequenceWrapper(sampleSeq.getStatements());
		
		String testScript = 
				".Sample{"
						+ ".state0{"
							+ ".seq0{"
								+ "if(Abs(DistanceToPlayer(\"X\")) <= 10){"
									+ "Spawn(.Bullet1);"
								+ "}"
								+ "Spawn(.Bullet1);"
							+ "}"
						+ "}"
				+ "}";
		Parser parser = new Parser();
		Root testRoot = parser.parse(testScript);
		Sequence testSeq = testRoot.getRelatedAgents().get(0).getStates().get(0)
				.getSequences().get(0);
		ASTSequenceWrapper wrappedTest = new ASTSequenceWrapper(testSeq.getStatements());
		
		Set<LCSSequenceEmbedding<ASTNode>> lcsResult =
				SimpleLCSEmbedding.allLCSEmbeddings(wrappedTest, wrappedSample, 
						ASTSequenceWrapper.spawnComparator);
		assertEquals(1, lcsResult.size());
		assertEquals(1, lcsResult.iterator().next().size());
		
		testScript = 
				".Sample{"
					+ ".state0{"
						+ ".seq0{"
							+ "if(Abs(DistanceToPlayer(\"X\")) <= 10){"
								+ "FlipDirection(\"H\");"
							+ "}else{"
								+ "if(DistanceToPlayer(\"X\") <= 10){"
									+ "Spawn(.Bullet1);"
								+ "}"
							+ "}"
							+ "Spawn(.Bullet1);"
						+ "}"
					+ "}"
				+ "}";
		parser = new Parser();
		testRoot = parser.parse(testScript);
		testSeq = testRoot.getRelatedAgents().get(0).getStates().get(0)
				.getSequences().get(0);
		wrappedTest = new ASTSequenceWrapper(testSeq.getStatements());
		lcsResult = SimpleLCSEmbedding.allLCSEmbeddings(wrappedTest, wrappedSample, 
						ASTSequenceWrapper.spawnComparator);
		assertEquals(1, lcsResult.size());
		assertEquals(1, lcsResult.iterator().next().size());
	}
}
