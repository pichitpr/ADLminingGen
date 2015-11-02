package adl_2daa.gen.encoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.expression.Comparison;
import adl_2daa.ast.expression.Function;
import adl_2daa.ast.expression.StringConstant;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.statement.Condition;
import adl_2daa.ast.statement.Loop;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.State;
import adl_2daa.gen.generator.ExpressionSkeleton;
import adl_2daa.gen.signature.Datatype;
import adl_2daa.gen.testtool.TestInitializer;
import adl_2daa.gen.testtool.TestUtility;
import adl_2daa.tool.Parser;

public class TestSequenceDecoder {

	protected boolean setup = false;
	protected Agent sample;
	
	@Before
	public void setUp() throws Exception {
		if(setup) return;
		TestInitializer.init();
		String script = TestUtility.readFileAsString("test/ext/Sample.txt");
		Parser parser = new Parser();
		Root agentFile = parser.parse(script);
		sample = agentFile.getRelatedAgents().get(0);
		setup = true;
	}
	
	@Test
	public void testDecode_init(){
		ADLSequenceEncoder.instance.setAnalyzeFlow(false);
		List<String> eSeq = ADLSequenceEncoder.instance.encode(sample.getInit()).encodedSequence;
		List<ASTStatement> statements = ADLSequenceDecoder.instance.decode(eSeq);
			
		//Set("texture")
		Action action = (Action)statements.get(0);
		assertEquals("Set", action.getName());
		assertEquals(3, action.getParams().length);
		assertEquals("texture", ((StringConstant)action.getParams()[0]).getValue());
		assertEquals(Datatype.DYNAMIC, ((ExpressionSkeleton)action.getParams()[1]).getType());
		assertEquals(Datatype.INT, ((ExpressionSkeleton)action.getParams()[2]).getType());
		
		//Set("position")
		action = (Action)statements.get(1);
		assertEquals("Set", action.getName());
		assertEquals(3, action.getParams().length);
		assertEquals("position", ((StringConstant)action.getParams()[0]).getValue());
		assertEquals(Datatype.DYNAMIC, ((ExpressionSkeleton)action.getParams()[1]).getType());
		assertEquals(Datatype.POSITION, ((ExpressionSkeleton)action.getParams()[2]).getType());
	}
	
	@Test
	public void testEncode_state3_seq0(){
		System.out.println("Start case 3");
		ADLSequenceEncoder.instance.setAnalyzeFlow(false);
		State eState = sample.getStates().get(2);
		List<String> eSeq = ADLSequenceEncoder.instance.encode(eState.getSequences().get(0)).encodedSequence;
		List<ASTStatement> statements = ADLSequenceDecoder.instance.decode(eSeq);
		
		//Statement 1
		//loop1 (Random(DecimalSet(1,6,1)))
		Loop loop1 = (Loop)statements.get(0);
		assertEquals("Random", ((Function)loop1.getLoopCount()).getName());
		
		//Wait (loop1)
		Action action = (Action)loop1.getContent().get(0);
		assertEquals("Wait", action.getName());
		assertEquals(1, action.getParams().length);
		assertEquals(Datatype.BOOL, ((ExpressionSkeleton)action.getParams()[0]).getType());
		
		
		//Statement 2
		loop1 = (Loop)statements.get(1);
		
		//cond1 (Random(DecimalSet(1,2,1)) == 1)
		Condition cond1 = (Condition)loop1.getContent().get(0);
		assertTrue(cond1.getCondition() instanceof Comparison);
		assertTrue(cond1.getElseblock() == null);
		
		//FlipDirection (loop1) (if cond1)
		action = (Action)cond1.getIfblock().get(0);
		assertEquals("FlipDirection", action.getName());
		assertEquals(1, action.getParams().length);
		assertEquals(Datatype.DIRECTION, ((ExpressionSkeleton)action.getParams()[0]).getType());
	}
}
