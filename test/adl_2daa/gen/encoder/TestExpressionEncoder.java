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
import adl_2daa.gen.signature.GeneratorRegistry;
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
		setup = true;
	}
	
	@Test
	public void testEncode(){
		Sequence seq = sample.getStates().get(0).getSequences().get(0);
		ASTExpression cond;
		String eExp;
		byte[] expectedCond;
		byte[] id0, id1;
		
		//(Abs(DistanceToPlayer("Y")) <= 30)
		cond = ((Condition)seq.getStatements().get(1)).getCondition();
		eExp = ADLExpressionEncoder.instance.encode(cond);
		id0 = EncodeTable.encodeSignatureID(
				GeneratorRegistry.getFunctionSignature("Abs").getMainSignature().getId()
				);
		id1 = EncodeTable.encodeSignatureID(
				GeneratorRegistry.getFunctionSignature("DistanceToPlayer").getChoiceSignature("Y").getId()
				);
		expectedCond = new byte[]{
				EncodeTable.EXP_BINARY,
				EncodeTable.EXP_BINARY_COMP_LE,
				EncodeTable.EXP_FUNCTION,
				id0[0], id0[1],
				EncodeTable.EXP_FUNCTION,
				id1[0], id1[1],
				EncodeTable.EXP_LITERAL
				};
		assertEquals(new String(expectedCond,StandardCharsets.US_ASCII), eExp);
		
		//Abs(DistanceToPlayer("Y"))$ <= 60
		cond = ((Condition)seq.getStatements().get(2)).getCondition();
		eExp = ADLExpressionEncoder.instance.encode(cond);
		id0 = EncodeTable.encodeSignatureID(
				-GeneratorRegistry.getFunctionSignature("Abs").getMainSignature().getId()
				);
		id1 = EncodeTable.encodeSignatureID(
				GeneratorRegistry.getFunctionSignature("DistanceToPlayer").getChoiceSignature("Y").getId()
				);
		expectedCond = new byte[]{
				EncodeTable.EXP_BINARY,
				EncodeTable.EXP_BINARY_COMP_LE,
				EncodeTable.EXP_FUNCTION,
				id0[0], id0[1],
				EncodeTable.EXP_FUNCTION,
				id1[0], id1[1],
				EncodeTable.EXP_LITERAL
				};
		assertEquals(new String(expectedCond,StandardCharsets.US_ASCII), eExp);
	}
}
