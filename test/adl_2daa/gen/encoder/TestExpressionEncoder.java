package adl_2daa.gen.encoder;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;

import adl_2daa.ast.ASTExpression;
import adl_2daa.ast.statement.Condition;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.gen.GeneratorRegistry;
import adl_2daa.gen.testtool.TestInitializer;
import adl_2daa.gen.testtool.TestUtility;
import adl_2daa.tool.Parser;

public class TestExpressionEncoder {

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
	}
	
	@Test
	public void testEncode(){
		Sequence seq = sample.getStates().get(0).getSequences().get(0);
		ASTExpression cond;
		String eExp;
		byte[] expectedCond;
		
		//(Abs(DistanceToPlayer("Y")) <= 30)
		cond = ((Condition)seq.getStatements().get(1)).getCondition();
		eExp = ADLExpressionEncoder.instance.encode(cond);
		expectedCond = new byte[]{
				EncodeTable.EXP_BINARY,
				EncodeTable.EXP_BINARY_COMP_LE,
				EncodeTable.EXP_FUNCTION,
				(byte)GeneratorRegistry.getFunctionSignature("Abs").getMainSignature().getId(),
				1,
				EncodeTable.EXP_FUNCTION,
				(byte)GeneratorRegistry.getFunctionSignature("DistanceToPlayer").getChoiceSignature("Y").getId(),
				0,
				EncodeTable.EXP_LITERAL
				};
		assertEquals(new String(expectedCond,StandardCharsets.US_ASCII), eExp);
		
		//Abs(DistanceToPlayer("Y"))$ <= 60
		cond = ((Condition)seq.getStatements().get(2)).getCondition();
		eExp = ADLExpressionEncoder.instance.encode(cond);
		expectedCond = new byte[]{
				EncodeTable.EXP_BINARY,
				EncodeTable.EXP_BINARY_COMP_LE,
				EncodeTable.EXP_FUNCTION,
				(byte)-GeneratorRegistry.getFunctionSignature("Abs").getMainSignature().getId(),
				1,
				EncodeTable.EXP_FUNCTION,
				(byte)GeneratorRegistry.getFunctionSignature("DistanceToPlayer").getChoiceSignature("Y").getId(),
				0,
				EncodeTable.EXP_LITERAL
				};
		assertEquals(new String(expectedCond,StandardCharsets.US_ASCII), eExp);
	}
}
