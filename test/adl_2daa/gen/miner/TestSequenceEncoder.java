package adl_2daa.gen.miner;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.State;
import adl_2daa.gen.GeneratorRegistry;
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
	}
	
	@Test
	public void testEncodeSequence() {
		EncodedSequence eSeq;
		EncodedAction eAct;
		String encoded;
		
		//.init
		eSeq = ADLSequenceEncoder.instance.parseAsEncodedSequence(sample.getInit(), false);
		encoded = ""+GeneratorRegistry.getActionSignature("Set").getChoiceSignature("texture").getId();
		eAct = eSeq.eActList.get(0);
		assertEquals(encoded, 
				TestUtility.actionToString(eAct.actionID, eAct.nestingConditions, true)
				);
		encoded = ""+GeneratorRegistry.getActionSignature("Set").getChoiceSignature("position").getId();
		eAct = eSeq.eActList.get(1);
		assertEquals(encoded, 
				TestUtility.actionToString(eAct.actionID, eAct.nestingConditions, true)
				);
		
		
		//.state1 .seq0
		State state = sample.getStates().get(0);
		eSeq = ADLSequenceEncoder.instance.parseAsEncodedSequence(state.getSequences().get(0), false);
		
		//if(Abs(DistanceToPlayer("Y")) <= 30)
		String cond = String.format("d0 Cmp #0 d1 %d #0 d2 %d #0 d3 Lit #1 d1 Lit", 
				GeneratorRegistry.getFunctionSignature("Abs").getMainSignature().getId(),
				GeneratorRegistry.getFunctionSignature("DistanceToPlayer").getChoiceSignature("Y").getId()
				);
		
		//if(DistanceToPlayer("X") <= 30)
		String cond2 = String.format("d0 Cmp #0 d1 %d #0 d2 Lit #1 d1 Lit", 
				GeneratorRegistry.getFunctionSignature("DistanceToPlayer").getChoiceSignature("X").getId()
				);
		
		//Wait
		encoded = ""+GeneratorRegistry.getActionSignature("Wait").getMainSignature().getId();
		eAct = eSeq.eActList.get(0);
		assertEquals(encoded,
				TestUtility.actionToString(eAct.actionID, eAct.nestingConditions, true)
				);
		
		//if(cond) Despawn Else..
		encoded = GeneratorRegistry.getActionSignature("Despawn").getMainSignature().getId()+
				" | If-E | "+cond;
		eAct = eSeq.eActList.get(1);
		assertEquals(encoded,
				TestUtility.actionToString(eAct.actionID, eAct.nestingConditions, true)
				);
		
		//if(cond) Else RunStraight
		encoded = GeneratorRegistry.getActionSignature("RunStraight").getMainSignature().getId()+
				" | Else | "+cond;
		eAct = eSeq.eActList.get(2);
		assertEquals(encoded,
				TestUtility.actionToString(eAct.actionID, eAct.nestingConditions, true)
				);
		
		//if(cond) Else FlipDirection
		encoded = GeneratorRegistry.getActionSignature("FlipDirection").getMainSignature().getId()+
				" | Else | "+cond;
		eAct = eSeq.eActList.get(3);
		assertEquals(encoded,
				TestUtility.actionToString(eAct.actionID, eAct.nestingConditions, true)
				);
		
		//if(cond) Else ... if(cond2) Spawn
		encoded = GeneratorRegistry.getActionSignature("Spawn").getMainSignature().getId()+
				" | Else | "+cond+" | If | "+cond2;
		eAct = eSeq.eActList.get(4);
		assertEquals(encoded,
				TestUtility.actionToString(eAct.actionID, eAct.nestingConditions, true)
				);
		
		//if(cond) Else ... if(cond2) Goto
		encoded = GeneratorRegistry.getActionSignature("Goto").getMainSignature().getId()+
				" | Else | "+cond+" | If | "+cond2;;
		eAct = eSeq.eActList.get(5);
		assertEquals(encoded,
				TestUtility.actionToString(eAct.actionID, eAct.nestingConditions, true)
				);
		
		//if(cond) Spawn
		encoded = GeneratorRegistry.getActionSignature("Spawn").getMainSignature().getId()+
				" | If | "+cond;
		eAct = eSeq.eActList.get(6);
		assertEquals(encoded,
				TestUtility.actionToString(eAct.actionID, eAct.nestingConditions, true)
				);
		
		//if(cond) Despawn
		encoded = GeneratorRegistry.getActionSignature("Despawn").getMainSignature().getId()+
				" | If | "+cond;
		eAct = eSeq.eActList.get(7);
		assertEquals(encoded,
				TestUtility.actionToString(eAct.actionID, eAct.nestingConditions, true)
				);
	}
	
	@Test
	public void testGenerateFlow() {
		//==================================================================
		//.state1 .seq0 : 3 flows
		State state = sample.getStates().get(0);
		EncodedSequence eSeq;
		eSeq = ADLSequenceEncoder.instance.parseAsEncodedSequence(state.getSequences().get(0), true);
		List<EncodedSequence> eSeqs = eSeq.allFlowToTransition;
		assertEquals(3, eSeqs.size());
		
		eSeq = eSeqs.get(0);
		assertEquals(GeneratorRegistry.getActionSignature("Wait").getMainSignature().getId(), 
				eSeq.eActList.get(0).actionID);
		assertEquals(GeneratorRegistry.getActionSignature("Despawn").getMainSignature().getId(), 
				eSeq.eActList.get(1).actionID);
		
		eSeq = eSeqs.get(1);
		assertEquals(GeneratorRegistry.getActionSignature("Wait").getMainSignature().getId(), 
				eSeq.eActList.get(0).actionID);
		assertEquals(GeneratorRegistry.getActionSignature("RunStraight").getMainSignature().getId(), 
				eSeq.eActList.get(1).actionID);
		assertEquals(GeneratorRegistry.getActionSignature("FlipDirection").getMainSignature().getId(), 
				eSeq.eActList.get(2).actionID);
		assertEquals(GeneratorRegistry.getActionSignature("Spawn").getMainSignature().getId(), 
				eSeq.eActList.get(3).actionID);
		assertEquals(GeneratorRegistry.getActionSignature("Goto").getMainSignature().getId(), 
				eSeq.eActList.get(4).actionID);
		
		eSeq = eSeqs.get(2);
		assertEquals(GeneratorRegistry.getActionSignature("Wait").getMainSignature().getId(), 
				eSeq.eActList.get(0).actionID);
		assertEquals(GeneratorRegistry.getActionSignature("RunStraight").getMainSignature().getId(), 
				eSeq.eActList.get(1).actionID);
		assertEquals(GeneratorRegistry.getActionSignature("FlipDirection").getMainSignature().getId(), 
				eSeq.eActList.get(2).actionID);
		assertEquals(GeneratorRegistry.getActionSignature("Spawn").getMainSignature().getId(), 
				eSeq.eActList.get(3).actionID);
		assertEquals(GeneratorRegistry.getActionSignature("Despawn").getMainSignature().getId(), 
				eSeq.eActList.get(4).actionID);
		
		//==================================================================
		//.state1 .seq1 : 0 flow
		eSeq = ADLSequenceEncoder.instance.parseAsEncodedSequence(state.getSequences().get(1), true);
		eSeqs = eSeq.allFlowToTransition;
		assertEquals(0, eSeqs.size());
		
		//==================================================================
		//.state2 .seq0 : 1 flow
		state = sample.getStates().get(1);
		eSeq = ADLSequenceEncoder.instance.parseAsEncodedSequence(state.getSequences().get(0), true);
		eSeqs = eSeq.allFlowToTransition;
		assertEquals(1, eSeqs.size());
		
		eSeq = eSeqs.get(0);
		assertEquals(GeneratorRegistry.getActionSignature("Wait").getMainSignature().getId(), 
				eSeq.eActList.get(0).actionID);
		assertEquals(GeneratorRegistry.getActionSignature("Spawn").getMainSignature().getId(), 
				eSeq.eActList.get(1).actionID);
		assertEquals(GeneratorRegistry.getActionSignature("Goto").getMainSignature().getId(), 
				eSeq.eActList.get(2).actionID);
	}

}
