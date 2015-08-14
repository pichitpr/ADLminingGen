package adl_2daa.gen.miner;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.State;
import adl_2daa.gen.GeneratorRegistry;
import adl_2daa.gen.testtool.TestInitializer;
import adl_2daa.gen.testtool.TestUtility;
import adl_2daa.tool.Parser;
import de.parsemis.graph.Graph;


public class TestNestingEncoder {

	protected boolean setup = false;
	protected Agent sample, sample2;
	
	@Before
	public void setUp() throws Exception {
		if(setup) return;
		TestInitializer.init();
		String script = TestUtility.readFileAsString("test/ext/Sample.txt");
		Parser parser = new Parser();
		Root agentFile = parser.parse(script);
		sample = agentFile.getRelatedAgents().get(0);
		
		script = TestUtility.readFileAsString("test/ext/Sample_nesting.txt");
		agentFile = parser.parse(script);
		sample2 = agentFile.getRelatedAgents().get(0);
	}
	
	@Test
	public void testEncodeNesting(){
		Collection<Graph<Integer,Integer>> graphs;
		Iterator<Graph<Integer,Integer>> it;
		Graph<Integer,Integer> graph;
		
		
		
		//.init
		graphs = ADLNestingEncoder.instance.parseAsGraphCollection(sample.getInit());
		assertEquals(2, graphs.size());
		it = graphs.iterator();
		graph = it.next();
		assertEquals(
				GeneratorRegistry.getActionSignature("Set").getChoiceSignature("texture").getId(), 
				(int)graph.getNode(0).getLabel()
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("DynamicFilter").getChoiceSignature("this").getId(), 
				(int)graph.getNode(1).getLabel()-ADLNestingEncoder.offset
				);
		assertEquals(1, (int)graph.getEdge(0).getLabel());
		
		graph = it.next();
		assertEquals(
				GeneratorRegistry.getActionSignature("Set").getChoiceSignature("position").getId(), 
				(int)graph.getNode(0).getLabel()
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("DynamicFilter").getChoiceSignature("this").getId(), 
				(int)graph.getNode(1).getLabel()-ADLNestingEncoder.offset
				);
		assertEquals(1, (int)graph.getEdge(0).getLabel());
				
		
		
		//.state1.seq0
		State state = sample.getStates().get(0);
		graphs = ADLNestingEncoder.instance.parseAsGraphCollection(state.getSequences().get(0));
		it = graphs.iterator();
		
		graph = it.next();
		assertEquals(
				GeneratorRegistry.getActionSignature("Wait").getMainSignature().getId(), 
				(int)graph.getNode(0).getLabel()
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("TimePass").getMainSignature().getId(), 
				(int)graph.getNode(1).getLabel()-ADLNestingEncoder.offset
				);
		assertEquals(0, (int)graph.getEdge(0).getLabel());
		
		graph = it.next();
		assertEquals(0, (int)graph.getEdge(0).getLabel());
		
		graph = it.next();
		
		graph = it.next();
		assertEquals(
				GeneratorRegistry.getActionSignature("RunStraight").getMainSignature().getId(), 
				(int)graph.getNode(0).getLabel()
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("Get").getChoiceSignature("direction").getId(), 
				(int)graph.getNode(1).getLabel()-ADLNestingEncoder.offset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("DynamicFilter").getChoiceSignature("this").getId(), 
				(int)graph.getNode(2).getLabel()-ADLNestingEncoder.offset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("TimePass").getMainSignature().getId(), 
				(int)graph.getNode(3).getLabel()-ADLNestingEncoder.offset
				);
		assertEquals(1, (int)graph.getEdge(0).getLabel());
		assertEquals(0, (int)graph.getEdge(1).getLabel());
		assertEquals(2, (int)graph.getEdge(2).getLabel());
		
		
		
		//.state1.seq1
		graphs = ADLNestingEncoder.instance.parseAsGraphCollection(state.getSequences().get(1));
		it = graphs.iterator();
		
		graph = it.next();
		assertEquals(0, (int)graph.getEdge(0).getLabel());
		
		graph = it.next();
		assertEquals(0, graph.getEdgeCount());
		
		
		
		//.state2.seq0
		state = sample.getStates().get(1);
		graphs = ADLNestingEncoder.instance.parseAsGraphCollection(state.getSequences().get(0));
		it = graphs.iterator();
		
		graph = it.next();
		assertEquals(0, (int)graph.getEdge(0).getLabel());
		
		graph = it.next();
		assertEquals(
				GeneratorRegistry.getFunctionSignature("Random").getMainSignature().getId(), 
				(int)graph.getNode(0).getLabel()-ADLNestingEncoder.offset
				);
		assertEquals(0, (int)graph.getEdge(0).getLabel());
		
		
		
		//.des
		graphs = ADLNestingEncoder.instance.parseAsGraphCollection(sample.getDes());
		it = graphs.iterator();
		graph = it.next();
		assertEquals(0, graph.getEdgeCount());
		
		
		
		//Sample_nesting.base.seq0 has a little more complex example
		graphs = ADLNestingEncoder.instance.parseAsGraphCollection(
				sample2.getStates().get(0).getSequences().get(0)
				);
		it = graphs.iterator();
		graph = it.next();
		assertEquals(
				GeneratorRegistry.getActionSignature("RunStraight").getMainSignature().getId(), 
				(int)graph.getNode(0).getLabel()
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("Get").getChoiceSignature("direction").getId(), 
				(int)graph.getNode(1).getLabel()-ADLNestingEncoder.offset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("DynamicFilter").getChoiceSignature("this").getId(), 
				(int)graph.getNode(2).getLabel()-ADLNestingEncoder.offset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("Abs").getMainSignature().getId(), 
				(int)graph.getNode(3).getLabel()-ADLNestingEncoder.offset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("DistanceToPlayer").getChoiceSignature("X").getId(), 
				(int)graph.getNode(4).getLabel()-ADLNestingEncoder.offset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("DistanceToPlayer").getChoiceSignature("Y").getId(), 
				(int)graph.getNode(5).getLabel()-ADLNestingEncoder.offset
				);
		assertEquals(1, (int)graph.getEdge(0).getLabel());
		assertEquals(0, (int)graph.getEdge(1).getLabel());
		assertEquals(0, (int)graph.getEdge(2).getLabel());
		assertEquals(2, (int)graph.getEdge(3).getLabel());
		assertEquals(2, (int)graph.getEdge(4).getLabel());
	}
}
