package adl_2daa.gen.encoder;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.State;
import adl_2daa.gen.Utility;
import adl_2daa.gen.signature.GeneratorRegistry;
import adl_2daa.gen.testtool.TestInitializer;
import adl_2daa.gen.testtool.TestUtility;
import adl_2daa.tool.Parser;

public class TestSequenceEncoder {

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
	public void testEncode_init(){
		ADLSequenceEncoder.instance.setAnalyzeFlow(false);
		List<String> eSeq = ADLSequenceEncoder.instance.encode(sample.getInit()).encodedSequence;
		String eAct;
		List<Byte> expected = new LinkedList<Byte>();
		
		assertEquals(2, eSeq.size());
		
		//Set("texture")
		eAct = eSeq.get(0);
		expected.add((byte)GeneratorRegistry.getActionSignature("Set").getChoiceSignature("texture").getId());
		assertEquals(new String(Utility.toByteArray(expected), StandardCharsets.US_ASCII), eAct);
		
		//Set("position")
		eAct = eSeq.get(1);
		expected.clear();
		expected.add((byte)GeneratorRegistry.getActionSignature("Set").getChoiceSignature("position").getId());
		assertEquals(new String(Utility.toByteArray(expected), StandardCharsets.US_ASCII), eAct);
	}
	
	@Test
	public void testEncode_state1_seq0(){
		ADLSequenceEncoder.instance.setAnalyzeFlow(false);
		State eState = sample.getStates().get(0);
		List<String> eSeq = ADLSequenceEncoder.instance.encode(eState.getSequences().get(0)).encodedSequence;
		String eAct;
		List<Byte> expected = new LinkedList<Byte>();
		Byte[] cond1,cond2;
		byte[] id0, id1;
		
		assertEquals(8, eSeq.size());
		
		//Wait
		eAct = eSeq.get(0);
		expected.add((byte)GeneratorRegistry.getActionSignature("Wait").getMainSignature().getId());
		assertEquals(new String(Utility.toByteArray(expected), StandardCharsets.US_ASCII), eAct);
		
		//cond1 ( Abs(DistanceToPlayer("Y")) <= 30 )
		id0 = EncodeTable.encodeSignatureID(
				GeneratorRegistry.getFunctionSignature("Abs").getMainSignature().getId()
				);
		id1 = EncodeTable.encodeSignatureID(
				GeneratorRegistry.getFunctionSignature("DistanceToPlayer").getChoiceSignature("Y").getId()
				);
		cond1 = new Byte[]{
				EncodeTable.EXP_BINARY,
				EncodeTable.EXP_BINARY_COMP_LE,
				EncodeTable.EXP_FUNCTION,
				id0[0], id0[1],
				EncodeTable.EXP_FUNCTION,
				id1[0], id1[1],
				EncodeTable.EXP_LITERAL
				};
		
		//Despawn (if cond1)
		eAct = eSeq.get(1);
		expected.clear();
		expected.add((byte)GeneratorRegistry.getActionSignature("Despawn").getMainSignature().getId());
		expected.add(EncodeTable.COND_IF_IFELSE);
		Utility.<Byte>addArrayToList(expected, cond1);
		assertEquals(new String(Utility.toByteArray(expected), StandardCharsets.US_ASCII), eAct);
		
		//RunStraight (else cond1)
		eAct = eSeq.get(2);
		expected.clear();
		expected.add((byte)GeneratorRegistry.getActionSignature("RunStraight").getMainSignature().getId());
		expected.add(EncodeTable.COND_ELSE_IFELSE);
		Utility.<Byte>addArrayToList(expected, cond1);
		assertEquals(new String(Utility.toByteArray(expected), StandardCharsets.US_ASCII), eAct);
		
		//FlipDirection (else cond1)
		eAct = eSeq.get(3);
		expected.clear();
		expected.add((byte)GeneratorRegistry.getActionSignature("FlipDirection").getMainSignature().getId());
		expected.add(EncodeTable.COND_ELSE_IFELSE);
		Utility.<Byte>addArrayToList(expected, cond1);
		assertEquals(new String(Utility.toByteArray(expected), StandardCharsets.US_ASCII), eAct);
		
		//cond2 (DistanceToPlayer("X") <= 30)
		id0 = EncodeTable.encodeSignatureID(
				GeneratorRegistry.getFunctionSignature("DistanceToPlayer").getChoiceSignature("X").getId()
				);
		cond2 = new Byte[]{
				EncodeTable.EXP_BINARY,
				EncodeTable.EXP_BINARY_COMP_LE,
				EncodeTable.EXP_FUNCTION,
				id0[0],id0[1],
				EncodeTable.EXP_LITERAL
				};
		
		//Spawn (else cond1) (if cond2)
		eAct = eSeq.get(4);
		expected.clear();
		expected.add((byte)GeneratorRegistry.getActionSignature("Spawn").getMainSignature().getId());
		expected.add(EncodeTable.COND_ELSE_IFELSE);
		Utility.<Byte>addArrayToList(expected, cond1);
		expected.add(EncodeTable.COND_IF);
		Utility.<Byte>addArrayToList(expected, cond2);
		assertEquals(new String(Utility.toByteArray(expected), StandardCharsets.US_ASCII), eAct);
		
		//Goto (else cond1) (if cond2)
		eAct = eSeq.get(5);
		expected.clear();
		expected.add((byte)GeneratorRegistry.getActionSignature("Goto").getMainSignature().getId());
		expected.add(EncodeTable.COND_ELSE_IFELSE);
		Utility.<Byte>addArrayToList(expected, cond1);
		expected.add(EncodeTable.COND_IF);
		Utility.<Byte>addArrayToList(expected, cond2);
		assertEquals(new String(Utility.toByteArray(expected), StandardCharsets.US_ASCII), eAct);
		
		//cond1 (Abs(DistanceToPlayer("Y"))$ <= 60)
		id0 = EncodeTable.encodeSignatureID(
				-GeneratorRegistry.getFunctionSignature("Abs").getMainSignature().getId()
				);
		id1 = EncodeTable.encodeSignatureID(
				GeneratorRegistry.getFunctionSignature("DistanceToPlayer").getChoiceSignature("Y").getId()
				);
		cond1 = new Byte[]{
				EncodeTable.EXP_BINARY,
				EncodeTable.EXP_BINARY_COMP_LE,
				EncodeTable.EXP_FUNCTION,
				id0[0], id0[1],
				EncodeTable.EXP_FUNCTION,
				id1[0], id1[1],
				EncodeTable.EXP_LITERAL
				};
		
		//Spawn (if cond1)
		eAct = eSeq.get(6);
		expected.clear();
		expected.add((byte)GeneratorRegistry.getActionSignature("Spawn").getMainSignature().getId());
		expected.add(EncodeTable.COND_IF);
		Utility.<Byte>addArrayToList(expected, cond1);
		assertEquals(new String(Utility.toByteArray(expected), StandardCharsets.US_ASCII), eAct);
		
		//Despawn (if cond1)
		eAct = eSeq.get(7);
		expected.clear();
		expected.add((byte)GeneratorRegistry.getActionSignature("Despawn").getMainSignature().getId());
		expected.add(EncodeTable.COND_IF);
		Utility.<Byte>addArrayToList(expected, cond1);
		assertEquals(new String(Utility.toByteArray(expected), StandardCharsets.US_ASCII), eAct);
	}
	
	@Test
	public void testEncode_state3_seq0(){
		ADLSequenceEncoder.instance.setAnalyzeFlow(false);
		State eState = sample.getStates().get(2);
		List<String> eSeq = ADLSequenceEncoder.instance.encode(eState.getSequences().get(0)).encodedSequence;
		String eAct;
		List<Byte> expected = new LinkedList<Byte>();
		Byte[] loop1,cond1;
		byte[] id0, id1;
		
		//loop1 (Random(DecimalSet(1,6,1)))
		id0 = EncodeTable.encodeSignatureID(
				GeneratorRegistry.getFunctionSignature("Random").getMainSignature().getId()
				);
		id1 = EncodeTable.encodeSignatureID(
				GeneratorRegistry.getFunctionSignature("DecimalSet").getMainSignature().getId()
				);
		loop1 = new Byte[]{
			EncodeTable.EXP_FUNCTION,
			id0[0], id0[1],
			EncodeTable.EXP_FUNCTION,
			id1[0], id1[1],
			EncodeTable.EXP_LITERAL,
			EncodeTable.EXP_LITERAL,
			EncodeTable.EXP_LITERAL
		};
		
		//Wait (loop1)
		eAct = eSeq.get(0);
		expected.add((byte)GeneratorRegistry.getActionSignature("Wait").getMainSignature().getId());
		expected.add(EncodeTable.LOOP);
		Utility.<Byte>addArrayToList(expected, loop1);
		assertEquals(new String(Utility.toByteArray(expected), StandardCharsets.US_ASCII), eAct);
		
		//cond1 (Random(DecimalSet(1,2,1)) == 1)
		id0 = EncodeTable.encodeSignatureID(
				GeneratorRegistry.getFunctionSignature("Random").getMainSignature().getId()
				);
		id1 = EncodeTable.encodeSignatureID(
				GeneratorRegistry.getFunctionSignature("DecimalSet").getMainSignature().getId()
				);
		cond1 = new Byte[]{
				EncodeTable.EXP_BINARY,
				EncodeTable.EXP_BINARY_COMP_EQ,
				EncodeTable.EXP_FUNCTION,
				id0[0], id0[1],
				EncodeTable.EXP_FUNCTION,
				id1[0], id1[1],
				EncodeTable.EXP_LITERAL,
				EncodeTable.EXP_LITERAL,
				EncodeTable.EXP_LITERAL,
				EncodeTable.EXP_LITERAL
		};
		
		//FlipDirection (loop1) (if cond1)
		eAct = eSeq.get(1);
		expected.clear();
		expected.add((byte)GeneratorRegistry.getActionSignature("FlipDirection").getMainSignature().getId());
		expected.add(EncodeTable.LOOP);
		Utility.<Byte>addArrayToList(expected, loop1);
		expected.add(EncodeTable.COND_IF);
		Utility.<Byte>addArrayToList(expected, cond1);
		assertEquals(new String(Utility.toByteArray(expected), StandardCharsets.US_ASCII), eAct);
	}
	
	@Test
	public void testGenerateFlow_state1_seq0() {
		ADLSequenceEncoder.instance.setAnalyzeFlow(true);
		
		State state = sample.getStates().get(0);
		ADLSequence eSeq;
		eSeq = ADLSequenceEncoder.instance.encode(state.getSequences().get(0));
		List<ADLSequence> eSeqs = eSeq.allFlowToTerminal;
		assertEquals(3, eSeqs.size());
		
		eSeq = eSeqs.get(0);
		assertEquals(GeneratorRegistry.getActionSignature("Wait").getMainSignature().getId(), 
				eSeq.encodedSequence.get(0).charAt(0));
		assertEquals("des", eSeq.identifier);
		
		eSeq = eSeqs.get(1);
		assertEquals(GeneratorRegistry.getActionSignature("Wait").getMainSignature().getId(), 
				eSeq.encodedSequence.get(0).charAt(0));
		assertEquals(GeneratorRegistry.getActionSignature("RunStraight").getMainSignature().getId(), 
				eSeq.encodedSequence.get(1).charAt(0));
		assertEquals(GeneratorRegistry.getActionSignature("FlipDirection").getMainSignature().getId(), 
				eSeq.encodedSequence.get(2).charAt(0));
		assertEquals(GeneratorRegistry.getActionSignature("Spawn").getMainSignature().getId(), 
				eSeq.encodedSequence.get(3).charAt(0));
		/*
		assertEquals(GeneratorRegistry.getActionSignature("Goto").getMainSignature().getId(), 
				eSeq.encodedSequence.get(4).charAt(0));
				*/
		assertEquals("state2", eSeq.identifier);
		
		eSeq = eSeqs.get(2);
		assertEquals(GeneratorRegistry.getActionSignature("Wait").getMainSignature().getId(), 
				eSeq.encodedSequence.get(0).charAt(0));
		assertEquals(GeneratorRegistry.getActionSignature("RunStraight").getMainSignature().getId(), 
				eSeq.encodedSequence.get(1).charAt(0));
		assertEquals(GeneratorRegistry.getActionSignature("FlipDirection").getMainSignature().getId(), 
				eSeq.encodedSequence.get(2).charAt(0));
		assertEquals(GeneratorRegistry.getActionSignature("Spawn").getMainSignature().getId(), 
				eSeq.encodedSequence.get(3).charAt(0));
		/*
		assertEquals(GeneratorRegistry.getActionSignature("Despawn").getMainSignature().getId(), 
				eSeq.encodedSequence.get(4).charAt(0));
				*/
		assertEquals("des", eSeq.identifier);
	}
	
	@Test
	public void testGenerateFlow_state1_seq1() {
		ADLSequenceEncoder.instance.setAnalyzeFlow(true);
		
		State state = sample.getStates().get(0);
		ADLSequence eSeq;
		eSeq = ADLSequenceEncoder.instance.encode(state.getSequences().get(1));
		List<ADLSequence> eSeqs = eSeq.allFlowToTerminal;
		assertEquals(0, eSeqs.size());
	}
	
	@Test
	public void testGenerateFlow_state2_seq0() {
		ADLSequenceEncoder.instance.setAnalyzeFlow(true);
		
		State state = sample.getStates().get(1);
		ADLSequence eSeq;
		eSeq = ADLSequenceEncoder.instance.encode(state.getSequences().get(0));
		List<ADLSequence> eSeqs = eSeq.allFlowToTerminal;
		assertEquals(3, eSeqs.size());
		
		eSeq = eSeqs.get(0);
		assertEquals(GeneratorRegistry.getActionSignature("Wait").getMainSignature().getId(), 
				eSeq.encodedSequence.get(0).charAt(0));
		assertEquals(GeneratorRegistry.getActionSignature("Spawn").getMainSignature().getId(), 
				eSeq.encodedSequence.get(1).charAt(0));
		/*
		assertEquals(GeneratorRegistry.getActionSignature("Goto").getMainSignature().getId(), 
				eSeq.encodedSequence.get(2).charAt(0));
				*/
		assertEquals("state1", eSeq.identifier);
		
		eSeq = eSeqs.get(1);
		assertEquals(GeneratorRegistry.getActionSignature("Wait").getMainSignature().getId(), 
				eSeq.encodedSequence.get(0).charAt(0));
		assertEquals(GeneratorRegistry.getActionSignature("Wait").getMainSignature().getId(), 
				eSeq.encodedSequence.get(1).charAt(0));
		/*
		assertEquals(GeneratorRegistry.getActionSignature("Despawn").getMainSignature().getId(), 
				eSeq.encodedSequence.get(2).charAt(0));
				*/
		assertEquals("des", eSeq.identifier);
		
		eSeq = eSeqs.get(2);
		assertEquals(GeneratorRegistry.getActionSignature("Wait").getMainSignature().getId(), 
				eSeq.encodedSequence.get(0).charAt(0));
		assertEquals(GeneratorRegistry.getActionSignature("Wait").getMainSignature().getId(), 
				eSeq.encodedSequence.get(1).charAt(0));
		assertEquals(GeneratorRegistry.getActionSignature("FlipDirection").getMainSignature().getId(), 
				eSeq.encodedSequence.get(2).charAt(0));
		assertEquals(GeneratorRegistry.getActionSignature("Wait").getMainSignature().getId(), 
				eSeq.encodedSequence.get(3).charAt(0));
		/*
		assertEquals(GeneratorRegistry.getActionSignature("Despawn").getMainSignature().getId(), 
				eSeq.encodedSequence.get(4).charAt(0));
				*/
		assertEquals("des", eSeq.identifier);
	}

}
