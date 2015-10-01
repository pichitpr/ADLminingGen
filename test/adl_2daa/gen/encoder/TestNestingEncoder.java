package adl_2daa.gen.encoder;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.State;
import adl_2daa.gen.signature.GeneratorRegistry;
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
	public void testEncodeNesting_sample_init(){
		Collection<Graph<Integer,Integer>> graphs;
		Iterator<Graph<Integer,Integer>> it;
		Graph<Integer,Integer> graph;
		
		graphs = ADLNestingEncoder.instance.parseAsGraphCollection(sample.getInit());
		assertEquals(2, graphs.size());
		it = graphs.iterator();
		
		//Set("texture", DynamicFilter("this"), 4)
		graph = it.next();
		assertEquals(2, graph.getNodeCount());
		assertEquals(1, graph.getEdgeCount());
		assertEquals(
				GeneratorRegistry.getActionSignature("Set").getChoiceSignature("texture").getId(), 
				(int)graph.getNode(0).getLabel()
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("DynamicFilter").getChoiceSignature("this").getId(), 
				(int)graph.getNode(1).getLabel()-EncodeTable.idOffset
				);
		assertEquals(1, (int)graph.getEdge(0).getLabel());
		
		//Set("position", DynamicFilter("this"), "c(400,200)");
		graph = it.next();
		assertEquals(2, graph.getNodeCount());
		assertEquals(1, graph.getEdgeCount());
		assertEquals(
				GeneratorRegistry.getActionSignature("Set").getChoiceSignature("position").getId(), 
				(int)graph.getNode(0).getLabel()
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("DynamicFilter").getChoiceSignature("this").getId(), 
				(int)graph.getNode(1).getLabel()-EncodeTable.idOffset
				);
		assertEquals(1, (int)graph.getEdge(0).getLabel());
	}		
		
	@Test
	public void testEncodeNesting_sample_state1_seq0(){	
		Collection<Graph<Integer,Integer>> graphs;
		Iterator<Graph<Integer,Integer>> it;
		Graph<Integer,Integer> graph;
		
		State state = sample.getStates().get(0);
		graphs = ADLNestingEncoder.instance.parseAsGraphCollection(state.getSequences().get(0));
		assertEquals(11, graphs.size());
		it = graphs.iterator();
		
		//Wait(TimePass() >= 60);
		graph = it.next();
		assertEquals(2, graph.getNodeCount());
		assertEquals(1, graph.getEdgeCount());
		assertEquals(
				GeneratorRegistry.getActionSignature("Wait").getMainSignature().getId(), 
				(int)graph.getNode(0).getLabel()
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("TimePass").getMainSignature().getId(), 
				(int)graph.getNode(1).getLabel()-EncodeTable.idOffset
				);
		assertEquals(0, (int)graph.getEdge(0).getLabel());
		
		//Abs(DistanceToPlayer("Y")) <= 30)
		graph = it.next();
		assertEquals(2, graph.getNodeCount());
		assertEquals(1, graph.getEdgeCount());
		assertEquals(
				GeneratorRegistry.getFunctionSignature("Abs").getMainSignature().getId(), 
				(int)graph.getNode(0).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("DistanceToPlayer").getChoiceSignature("Y").getId(),
				(int)graph.getNode(1).getLabel()-EncodeTable.idOffset
				);
		assertEquals(0, (int)graph.getEdge(0).getLabel());
		
		//Despawn();
		graph = it.next();
		assertEquals(1, graph.getNodeCount());
		assertEquals(0, graph.getEdgeCount());
		assertEquals(
				GeneratorRegistry.getActionSignature("Despawn").getMainSignature().getId(), 
				(int)graph.getNode(0).getLabel()
				);
		
		//RunStraight(Get("direction", DynamicFilter("this")), 2, TimePass() >= 150);
		graph = it.next();
		assertEquals(4, graph.getNodeCount());
		assertEquals(3, graph.getEdgeCount());
		assertEquals(
				GeneratorRegistry.getActionSignature("RunStraight").getMainSignature().getId(), 
				(int)graph.getNode(0).getLabel()
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("Get").getChoiceSignature("direction").getId(), 
				(int)graph.getNode(1).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("DynamicFilter").getChoiceSignature("this").getId(), 
				(int)graph.getNode(2).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("TimePass").getMainSignature().getId(), 
				(int)graph.getNode(3).getLabel()-EncodeTable.idOffset
				);
		assertEquals(1, (int)graph.getEdge(0).getLabel());
		assertEquals(0, (int)graph.getEdge(1).getLabel());
		assertEquals(2, (int)graph.getEdge(2).getLabel());
		
		//Skip some
		graph = it.next();
		graph = it.next();
		graph = it.next();
		graph = it.next();
		
		//Abs(DistanceToPlayer("Y"))$ <= 60
		graph = it.next();
		assertEquals(2, graph.getNodeCount());
		assertEquals(1, graph.getEdgeCount());
		assertEquals(
				GeneratorRegistry.getFunctionSignature("Abs").getMainSignature().getId(), 
				(int)-graph.getNode(0).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("DistanceToPlayer").getChoiceSignature("Y").getId(),
				(int)graph.getNode(1).getLabel()-EncodeTable.idOffset
				);
		assertEquals(0, (int)graph.getEdge(0).getLabel());
	}
		
	@Test
	public void testEncodeNesting_sample_state3_seq0(){
		//Test something in loop
		Collection<Graph<Integer,Integer>> graphs;
		Iterator<Graph<Integer,Integer>> it;
		Graph<Integer,Integer> graph;
		
		State state = sample.getStates().get(2);
		graphs = ADLNestingEncoder.instance.parseAsGraphCollection(state.getSequences().get(0));
		assertEquals(4, graphs.size());
		it = graphs.iterator();
		
		//Random(DecimalSet(1,6,1))
		graph = it.next();
		assertEquals(2, graph.getNodeCount());
		assertEquals(1, graph.getEdgeCount());
		assertEquals(
				GeneratorRegistry.getFunctionSignature("Random").getMainSignature().getId(), 
				(int)graph.getNode(0).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("DecimalSet").getMainSignature().getId(),
				(int)graph.getNode(1).getLabel()-EncodeTable.idOffset
				);
		assertEquals(0, (int)graph.getEdge(0).getLabel());
		
		//Wait(TimePass() >= 60);
		graph = it.next();
		assertEquals(2, graph.getNodeCount());
		assertEquals(1, graph.getEdgeCount());
		assertEquals(
				GeneratorRegistry.getActionSignature("Wait").getMainSignature().getId(), 
				(int)graph.getNode(0).getLabel()
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("TimePass").getMainSignature().getId(), 
				(int)graph.getNode(1).getLabel()-EncodeTable.idOffset
				);
		assertEquals(0, (int)graph.getEdge(0).getLabel());
	}	
		
	@Test
	public void testEncodeNesting_sample2_base_seq0(){	
		Collection<Graph<Integer,Integer>> graphs;
		Iterator<Graph<Integer,Integer>> it;
		Graph<Integer,Integer> graph;
		
		graphs = ADLNestingEncoder.instance.parseAsGraphCollection(
				sample2.getStates().get(0).getSequences().get(0)
				);
		assertEquals(1, graphs.size());
		it = graphs.iterator();
		
		//RunStraight(Get("direction",DynamicFilter("this")), 6, Abs(DistanceToPlayer("X")) + DistanceToPlayer("Y") > 10);
		graph = it.next();
		assertEquals(6, graph.getNodeCount());
		assertEquals(5, graph.getEdgeCount());
		assertEquals(
				GeneratorRegistry.getActionSignature("RunStraight").getMainSignature().getId(), 
				(int)graph.getNode(0).getLabel()
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("Get").getChoiceSignature("direction").getId(), 
				(int)graph.getNode(1).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("DynamicFilter").getChoiceSignature("this").getId(), 
				(int)graph.getNode(2).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("Abs").getMainSignature().getId(), 
				(int)graph.getNode(3).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("DistanceToPlayer").getChoiceSignature("X").getId(), 
				(int)graph.getNode(4).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("DistanceToPlayer").getChoiceSignature("Y").getId(), 
				(int)graph.getNode(5).getLabel()-EncodeTable.idOffset
				);
		assertEquals(1, (int)graph.getEdge(0).getLabel());
		assertEquals(0, (int)graph.getEdge(1).getLabel());
		assertEquals(0, (int)graph.getEdge(2).getLabel());
		assertEquals(2, (int)graph.getEdge(3).getLabel());
		assertEquals(2, (int)graph.getEdge(4).getLabel());
	}
}
