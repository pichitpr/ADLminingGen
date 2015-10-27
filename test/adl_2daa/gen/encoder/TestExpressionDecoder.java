package adl_2daa.gen.encoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import adl_2daa.ast.ASTExpression;
import adl_2daa.ast.expression.Comparison;
import adl_2daa.ast.expression.Function;
import adl_2daa.ast.expression.StringConstant;
import adl_2daa.ast.statement.Condition;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.gen.generator.LiteralSkeleton;
import adl_2daa.gen.signature.Datatype;
import adl_2daa.gen.testtool.TestInitializer;
import adl_2daa.gen.testtool.TestUtility;
import adl_2daa.tool.Parser;

public class TestExpressionDecoder {

	private boolean setup = false;
	private Agent sample;
	
	@Before
	public void setup() throws Exception{
		if(setup) return;
		TestInitializer.init();
		String script = TestUtility.readFileAsString("test/ext/Sample.txt");
		Parser parser = new Parser();
		Root agentFile = parser.parse(script);
		sample = agentFile.getRelatedAgents().get(0);
		setup = true;
	}
	
	@Test
	public void testDecode(){
		Sequence seq = sample.getStates().get(0).getSequences().get(0);
		ASTExpression cond;
		String eExp;
		
		//(Abs(DistanceToPlayer("Y")) <= 30)
		cond = ((Condition)seq.getStatements().get(1)).getCondition();
		eExp = ADLExpressionEncoder.instance.encode(cond);
		cond = ADLExpressionDecoder.instance.decode(eExp, Datatype.BOOL);
		assertTrue(cond instanceof Comparison);
		Comparison comp = (Comparison)cond;
		assertTrue(comp.right instanceof LiteralSkeleton);
		assertEquals(Datatype.DECIMAL, ((LiteralSkeleton)comp.right).getType());
		assertTrue(comp.left instanceof Function);
		Function f1 = (Function)comp.left;
		assertEquals("Abs", f1.getName());
		assertTrue(!f1.hasSingleQuery());
		assertTrue(f1.getParams()[0] instanceof Function);
		Function f2 = (Function)f1.getParams()[0];
		assertEquals("DistanceToPlayer", f2.getName());
		assertTrue(!f2.hasSingleQuery());
		assertTrue(f2.getParams()[0] instanceof StringConstant);
		assertEquals("Y", ((StringConstant)f2.getParams()[0]).getValue());
		
		//Abs(DistanceToPlayer("Y"))$ <= 60
		cond = ((Condition)seq.getStatements().get(2)).getCondition();
		eExp = ADLExpressionEncoder.instance.encode(cond);
		cond = ADLExpressionDecoder.instance.decode(eExp, Datatype.BOOL);
		assertTrue(cond instanceof Comparison);
		comp = (Comparison)cond;
		assertTrue(comp.left instanceof Function);
		f1 = (Function)comp.left;
		assertEquals("Abs", f1.getName());
		assertTrue(f1.hasSingleQuery());
	}
}
