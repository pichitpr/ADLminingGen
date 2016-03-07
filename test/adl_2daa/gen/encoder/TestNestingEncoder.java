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
		
		setup = true;
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
		assertEquals(3, graph.getNodeCount());
		assertEquals(2, graph.getEdgeCount());
		assertEquals(
				GeneratorRegistry.getActionSignature("Set").getChoiceSignature("texture").getId(), 
				(int)graph.getNode(0).getLabel()
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("DynamicFilter").getChoiceSignature("this").getId(), 
				(int)graph.getNode(1).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				NestingLiteralCollectionExp.Integer.encode(4),
				(int)graph.getNode(2).getLabel()
				);
		assertEquals(1, (int)graph.getEdge(0).getLabel());
		assertEquals(2, (int)graph.getEdge(1).getLabel());
		
		//Set("position", DynamicFilter("this"), "c(400,200)");
		graph = it.next();
		assertEquals(3, graph.getNodeCount());
		assertEquals(2, graph.getEdgeCount());
		assertEquals(
				GeneratorRegistry.getActionSignature("Set").getChoiceSignature("position").getId(), 
				(int)graph.getNode(0).getLabel()
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("DynamicFilter").getChoiceSignature("this").getId(), 
				(int)graph.getNode(1).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				NestingLiteralCollectionExp.Position.encode("c(400,200)"),
				(int)graph.getNode(2).getLabel()
				);
		assertEquals(1, (int)graph.getEdge(0).getLabel());
		assertEquals(2, (int)graph.getEdge(1).getLabel());
	}		
		
	@Test
	public void testEncodeNesting_sample_state1_seq0(){	
		Collection<Graph<Integer,Integer>> graphs;
		Iterator<Graph<Integer,Integer>> it;
		Graph<Integer,Integer> graph;
		
		State state = sample.getStates().get(0);
		graphs = ADLNestingEncoder.instance.parseAsGraphCollection(state.getSequences().get(0));
		assertEquals(6, graphs.size());
		it = graphs.iterator();
		
		//Wait(TimePass() >= 60);
		graph = it.next();
		assertEquals(4, graph.getNodeCount());
		assertEquals(3, graph.getEdgeCount());
		assertEquals(
				GeneratorRegistry.getActionSignature("Wait").getMainSignature().getId(), 
				(int)graph.getNode(0).getLabel()
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("@C_ge").getMainSignature().getId(), 
				(int)graph.getNode(1).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("TimePass").getMainSignature().getId(), 
				(int)graph.getNode(2).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				NestingLiteralCollectionExp.Integer.encode(60), 
				(int)graph.getNode(3).getLabel()
				);
		assertEquals(0, (int)graph.getEdge(0).getLabel());
		assertEquals(1, (int)graph.getEdge(1).getLabel());
		assertEquals(0, (int)graph.getEdge(2).getLabel());
		
		//Abs(DistanceToPlayer("Y")) <= 30
		graph = it.next();
		assertEquals(4, graph.getNodeCount());
		assertEquals(3, graph.getEdgeCount());
		assertEquals(
				GeneratorRegistry.getFunctionSignature("@C_le").getMainSignature().getId(), 
				(int)graph.getNode(0).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("Abs").getMainSignature().getId(), 
				(int)graph.getNode(1).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("DistanceToPlayer").getChoiceSignature("Y").getId(),
				(int)graph.getNode(2).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				NestingLiteralCollectionExp.Integer.encode(30), 
				(int)graph.getNode(3).getLabel()
				);
		assertEquals(0, (int)graph.getEdge(0).getLabel());
		assertEquals(0, (int)graph.getEdge(1).getLabel());
		assertEquals(1, (int)graph.getEdge(2).getLabel());
		
		//RunStraight(Get("direction", DynamicFilter("this")), 2, TimePass() >= 150);
		graph = it.next();
		assertEquals(7, graph.getNodeCount());
		assertEquals(6, graph.getEdgeCount());
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
				NestingLiteralCollectionExp.Integer.encode(2), 
				(int)graph.getNode(3).getLabel()
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("@C_ge").getMainSignature().getId(), 
				(int)graph.getNode(4).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("TimePass").getMainSignature().getId(), 
				(int)graph.getNode(5).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				NestingLiteralCollectionExp.Integer.encode(150), 
				(int)graph.getNode(6).getLabel()
				);
		assertEquals(1, (int)graph.getEdge(0).getLabel());
		assertEquals(0, (int)graph.getEdge(1).getLabel());
		assertEquals(1, (int)graph.getEdge(2).getLabel());
		assertEquals(0, (int)graph.getEdge(3).getLabel());
		assertEquals(1, (int)graph.getEdge(4).getLabel());
		assertEquals(2, (int)graph.getEdge(5).getLabel());
		
		//FlipDirection("H")
		graph = it.next();
		assertEquals(2, graph.getNodeCount());
		assertEquals(1, graph.getEdgeCount());
		assertEquals(
				GeneratorRegistry.getActionSignature("FlipDirection").getMainSignature().getId(), 
				(int)graph.getNode(0).getLabel()
				);
		assertEquals(
				NestingLiteralCollectionExp.Direction.encode("H"), 
				(int)graph.getNode(1).getLabel()
				);
		assertEquals(0, (int)graph.getEdge(0).getLabel());
		
		//Skip DistanceToPlayer("X") <= 30
		graph = it.next();
		
		//Abs(DistanceToPlayer("Y"))$ <= 60
		graph = it.next();
		assertEquals(4, graph.getNodeCount());
		assertEquals(3, graph.getEdgeCount());
		assertEquals(
				GeneratorRegistry.getFunctionSignature("@C_le").getMainSignature().getId(), 
				(int)graph.getNode(0).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("Abs").getMainSignature().getId(), 
				(int)-graph.getNode(1).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("DistanceToPlayer").getChoiceSignature("Y").getId(),
				(int)graph.getNode(2).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				NestingLiteralCollectionExp.Integer.encode(60), 
				(int)graph.getNode(3).getLabel()
				);
		assertEquals(0, (int)graph.getEdge(0).getLabel());
		assertEquals(0, (int)graph.getEdge(1).getLabel());
		assertEquals(1, (int)graph.getEdge(2).getLabel());
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
		assertEquals(5, graph.getNodeCount());
		assertEquals(4, graph.getEdgeCount());
		assertEquals(
				GeneratorRegistry.getFunctionSignature("Random").getMainSignature().getId(), 
				(int)graph.getNode(0).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("DecimalSet").getMainSignature().getId(),
				(int)graph.getNode(1).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				NestingLiteralCollectionExp.Integer.encode(1), 
				(int)graph.getNode(2).getLabel()
				);
		assertEquals(
				NestingLiteralCollectionExp.Integer.encode(6), 
				(int)graph.getNode(3).getLabel()
				);
		assertEquals(
				NestingLiteralCollectionExp.Integer.encode(1), 
				(int)graph.getNode(4).getLabel()
				);
		assertEquals(0, (int)graph.getEdge(0).getLabel());
		assertEquals(1, (int)graph.getEdge(1).getLabel());
		assertEquals(2, (int)graph.getEdge(2).getLabel());
		assertEquals(0, (int)graph.getEdge(3).getLabel());
		
		//Wait(TimePass() >= 60);
		graph = it.next();
		assertEquals(4, graph.getNodeCount());
		assertEquals(3, graph.getEdgeCount());
		assertEquals(
				GeneratorRegistry.getActionSignature("Wait").getMainSignature().getId(), 
				(int)graph.getNode(0).getLabel()
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("@C_ge").getMainSignature().getId(), 
				(int)graph.getNode(1).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("TimePass").getMainSignature().getId(), 
				(int)graph.getNode(2).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				NestingLiteralCollectionExp.Integer.encode(60), 
				(int)graph.getNode(3).getLabel()
				);
		assertEquals(0, (int)graph.getEdge(0).getLabel());
		assertEquals(1, (int)graph.getEdge(1).getLabel());
		assertEquals(0, (int)graph.getEdge(2).getLabel());
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
		assertEquals(10, graph.getNodeCount());
		assertEquals(9, graph.getEdgeCount());
		//Pre-order depth first node creation
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
				NestingLiteralCollectionExp.Integer.encode(6), 
				(int)graph.getNode(3).getLabel()
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("@C_gt").getMainSignature().getId(), 
				(int)graph.getNode(4).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("@A_add").getMainSignature().getId(), 
				(int)graph.getNode(5).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("Abs").getMainSignature().getId(), 
				(int)graph.getNode(6).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("DistanceToPlayer").getChoiceSignature("X").getId(), 
				(int)graph.getNode(7).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				GeneratorRegistry.getFunctionSignature("DistanceToPlayer").getChoiceSignature("Y").getId(), 
				(int)graph.getNode(8).getLabel()-EncodeTable.idOffset
				);
		assertEquals(
				NestingLiteralCollectionExp.Integer.encode(10), 
				(int)graph.getNode(9).getLabel()
				);
		//Post-order depth first edge creation
		assertEquals(1, (int)graph.getEdge(0).getLabel());
		assertEquals(0, (int)graph.getEdge(1).getLabel());
		assertEquals(1, (int)graph.getEdge(2).getLabel());
		assertEquals(0, (int)graph.getEdge(3).getLabel());
		assertEquals(0, (int)graph.getEdge(4).getLabel());
		assertEquals(1, (int)graph.getEdge(5).getLabel());
		assertEquals(0, (int)graph.getEdge(6).getLabel());
		assertEquals(1, (int)graph.getEdge(7).getLabel());
		assertEquals(2, (int)graph.getEdge(8).getLabel());
	}
}
