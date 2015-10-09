package adl_2daa.gen.encoder;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import adl_2daa.gen.testtool.TestInitializer;
import de.parsemis.graph.Graph;

public class TestDatabaseCreator {

	private DatabaseCreator dbCreator = null;
	
	@Before
	public void setUp() {
		if(dbCreator == null){
			TestInitializer.init();
			dbCreator = new DatabaseCreator();
			dbCreator.load("test/ext/minidb");
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCreateDBforOrder() {
		Object[] dbCreatorResult = dbCreator.createDatabaseForOrder(0);
		List<List<String>>[] db = (List[])dbCreatorResult[0];
		List<Integer> agentIDMap = (List<Integer>)dbCreatorResult[1];
		assertEquals(14, db.length);
		for(int i=0; i<14; i++){
			assertEquals(i, (int)agentIDMap.get(i));
		}
		
		dbCreatorResult = dbCreator.createDatabaseForOrder(1);
		db = (List[])dbCreatorResult[0];
		agentIDMap = (List<Integer>)dbCreatorResult[1];
		assertEquals(3, db.length);
		assertEquals(3, (int)agentIDMap.get(0));
		assertEquals(4, (int)agentIDMap.get(1));
		assertEquals(7, (int)agentIDMap.get(2));
		
		dbCreatorResult = dbCreator.createDatabaseForOrder(2);
		db = (List[])dbCreatorResult[0];
		agentIDMap = (List<Integer>)dbCreatorResult[1];
		assertEquals(24, db.length);
		int[] lastSequenceIndex = new int[]{3,4,5, 7,8,11, 13,14,15, 19,20,21,22,23};
		for(int i=0; i<14; i++){
			assertEquals(i, (int)agentIDMap.get(lastSequenceIndex[i]));
		}
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testCreateDBForInterStateOrder(){
		Object[] db = dbCreator.createDatabaseForInterStateOrder();
		
		/*
		 * minidb inter-state profile
		 * FlameMan
		 * .base(1) | .seq0 -> {.flame, .bullet}   
		 * .flame(1) | .seq0 -> {.base}
		 * .bullet(2) | .seq0 -> {.base}
		 * 
		 * FlameShield
		 * .state0(1) | .seq0 -> {.des}
		 * 
		 * FlameShot
		 * .state0(1)
		 * --------------------------------
		 * ShellKoopa .state0(2)
		 * Koopa .state0(1)
		 * 
		 * KoopaShell
		 * .state0(1) | .seq0 -> {.state1}
		 * .state1(2) | .seq0 -> {.state0}
		 * --------------------------------
		 * Lakitu .state0(2)
		 * 
		 * SpinyEgg 
		 * .state0(1) | .seq0 -> {.des}
		 *
		 * Spiny .state0(1)
		 * --------------------------------
		 * TomahawkMan
		 * .base(2) | .seq0 -> {.feather, .tomahawk}
		 * .feather(1) | .seq0 -> {.base}
		 * .tomahawk(1) | .seq0 -> {.base}
		 * 
		 * FeatherBullet1 .state0(1)
		 * FeatherBullet2 .state0(1)
		 * FeatherBullet3 .state0(1)
		 * Tomahawk .state0(1)
		 */
		
		//Transition with "Goto"
		assertEquals(14, ((List[])db[0]).length);
		assertEquals(14, ((List[])db[1]).length);
		assertEquals(14, ((Integer[])db[2]).length);
		
		Integer[] tag = (Integer[])db[2];
		assertEquals(3, (int)tag[0]); //FlameMan 3 states
		assertEquals(2, (int)tag[5]); //KoopaShell 2 states
		assertEquals(3, (int)tag[8]); //Tomahawk 3 states
		
		List<Integer> agentIDMap = (List<Integer>)db[3];
		//Sampling at the bound
		assertEquals(0, (int)agentIDMap.get(4));
		assertEquals(5, (int)agentIDMap.get(7));
		assertEquals(9, (int)agentIDMap.get(13));
		
		//Transition with "Despawn"
		assertEquals(2, ((List[])db[4]).length);
		assertEquals(2, ((List[])db[5]).length);
		assertEquals(2, ((Integer[])db[6]).length);
		
		tag = (Integer[])db[6];
		assertEquals(1, (int)tag[0]); //FlameShield 1 state
		assertEquals(1, (int)tag[1]); //SpinyEgg 1 state
		
		agentIDMap = (List<Integer>)db[7];
		assertEquals(1, (int)agentIDMap.get(0));
		assertEquals(7, (int)agentIDMap.get(1));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCreateDBForParallel(){
		Object[] dbCreatorResult = dbCreator.createDatabaseForParallel();
		Collection<Graph<String,Integer>> db = (Collection<Graph<String,Integer>>)dbCreatorResult[0];
		List<Integer> agentIDMap = (List<Integer>)dbCreatorResult[1];
		
		//19 states across 4 files
		assertEquals(19, db.size());
		assertTrue(isGraphIDConsistent(db));
		
		//Agent files are converted to graph in lexicographical order
		Iterator<Graph<String,Integer>> it = db.iterator();
		while(it.hasNext()){
			Graph<String,Integer> graph = it.next();
			switch(graph.getName()){
			case "0": //FlameMan.base
				assertEquals(1+1+5, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				assertEquals(0, (int)agentIDMap.get(0));
				break;
			case "1": //FlameMan.flame
				assertEquals(1+1+14, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				assertEquals(0, (int)agentIDMap.get(1));
				break;
			case "2": //FlameMan.bullet
				assertEquals(1+2+3+1, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				assertEquals(0, (int)agentIDMap.get(2));
				break;
			case "3": //FlameShield.state0
				assertEquals(1+1+6, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				assertEquals(1, (int)agentIDMap.get(3));
				break;
			case "4": //FlameShot.state0
				assertEquals(1+1+1, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				assertEquals(2, (int)agentIDMap.get(4));
				break;
			case "5": //ShellKoopa.state0
				assertEquals(1+2+1+1, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				assertEquals(3, (int)agentIDMap.get(5));
				break;
			case "6": //Koopa.state0
				assertEquals(1+1+2, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				assertEquals(4, (int)agentIDMap.get(6));
				break;
			case "7": //KoopaShell.state0
				assertEquals(1+1+2, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				assertEquals(5, (int)agentIDMap.get(7));
				break;
			case "8": //KoopaShell.state1
				assertEquals(1+2+2+2, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				assertEquals(5, (int)agentIDMap.get(8));
				break;
			case "9": //Lakitu.state0
				assertEquals(1+2+2+2, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				assertEquals(6, (int)agentIDMap.get(9));
				break;
			case "10": //SpinyEgg.state0
				assertEquals(1+1+1, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				assertEquals(7, (int)agentIDMap.get(10));
				break;
			case "11": //Spiny.state0
				assertEquals(1+1+2, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				assertEquals(8, (int)agentIDMap.get(11));
				break;
			case "12": //TomahawkMan.base
				assertEquals(1+2+6+1, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				assertEquals(9, (int)agentIDMap.get(12));
				break;
			case "13": //TomahawkMan.feather
				assertEquals(1+1+5, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				assertEquals(9, (int)agentIDMap.get(13));
				break;
			case "14": //TomahawkMan.tomahawk
				assertEquals(1+1+3, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				assertEquals(9, (int)agentIDMap.get(14));
				break;
			case "15": //FeatherBullet1.state0
				assertEquals(1+1+1, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				assertEquals(10, (int)agentIDMap.get(15));
				break;
			case "16": //FeatherBullet2.state0
				assertEquals(1+1+1, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				assertEquals(11, (int)agentIDMap.get(16));
				break;
			case "17": //FeatherBullet3.state0
				assertEquals(1+1+1, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				assertEquals(12, (int)agentIDMap.get(17));
				break;
			case "18": //Tomahawk.state0
				assertEquals(1+1+1, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				assertEquals(13, (int)agentIDMap.get(18));
				break;
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testCreateDBForInterEntityParallel(){
		Object[] dbCreatorResult = dbCreator.createDatabaseForInterEntityParallel();
		Collection<Graph<String,Integer>> db = (Collection<Graph<String,Integer>>)dbCreatorResult[0];
		List<Integer> agentIDMap = (List<Integer>)dbCreatorResult[1];
		
		/*
		 * minidb inter-entity profile
		 * 
		 * FlameMan
		 * .flame(1) -> Spawn .FlameShield(1) *6
		 * .bullet(1) -> Spawn .FlameShot(1) *1
		 * 
		 * ShellKoopa
		 * .des -> Spawn .Koopa(1) *1
		 * 
		 * Koopa
		 * .des -> Spawn .KoopaShell(1) *1
		 * 
		 * Lakitu
		 * .state0(2) -> Spawn .SpinyEgg(1) *1
		 * 
		 * SpinyEgg
		 * .des -> Spawn .Spiny(1) *1
		 * 
		 * Tomahawk
		 * .feather -> Spawn .FeatherBullet{1,2,3} (1,1,1)
		 * .tomahawk -> Spawn .Tomahawk(1) *1
		 */
		
		//14 inter-entity including duplication, excluding spawn in .des
		assertEquals(12, db.size());
		assertTrue(isGraphIDConsistent(db));
		
		//Agent files are converted to graph in lexicographical order
		Iterator<Graph<String,Integer>> it = db.iterator();
		while(it.hasNext()){
			Graph<String,Integer> graph = it.next();
			switch(graph.getID()){
			case 0: case 1: case 2: case 3: case 4: case 5:
				//FlameMan.flame(1) -> Spawn .FlameShield(1) *6
				assertEquals(2+15+7, graph.getNodeCount());
				assertEquals(0, (int)agentIDMap.get(graph.getID()));
				break;
			case 6:
				//FlameMan.bullet(1) -> Spawn .FlameShot(1) *1
				assertEquals(2+8+2+2, graph.getNodeCount());
				assertEquals(0, (int)agentIDMap.get(graph.getID()));
				break;
			case 7:
				//Lakitu.state0(2) -> Spawn .SpinyEgg(1)
				assertEquals(2+3+3+2, graph.getNodeCount());
				assertEquals(6, (int)agentIDMap.get(graph.getID()));
				break;
			case 8:
				//TomahawkMan.feather -> Spawn .FeatherBullet1 (1)
				assertEquals(2+6+2, graph.getNodeCount());
				assertEquals(9, (int)agentIDMap.get(graph.getID()));
				break;
			case 9:
				//TomahawkMan.feather -> Spawn .FeatherBullet2 (1)
				assertEquals(2+6+2, graph.getNodeCount());
				assertEquals(9, (int)agentIDMap.get(graph.getID()));
				break;
			case 10:
				//TomahawkMan.feather -> Spawn .FeatherBullet3 (1)
				assertEquals(2+6+2, graph.getNodeCount());
				assertEquals(9, (int)agentIDMap.get(graph.getID()));
				break;
			case 11:
				//TomahawkMan.tomahawk -> Spawn .Tomahawk(1) *1
				assertEquals(2+4+2, graph.getNodeCount());
				assertEquals(9, (int)agentIDMap.get(graph.getID()));
				break;
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testCreateDBforNesting(){
		Object[] dbCreatorResult = dbCreator.createDatabaseForNesting();
		Collection<Graph<String,Integer>> db = (Collection<Graph<String,Integer>>)dbCreatorResult[0];
		List<Integer> agentIDMap = (List<Integer>)dbCreatorResult[1];
		
		int flameManGraph = (9+7+14+3+1)+(8+6)+(7+1);
		assertEquals(0, (int)agentIDMap.get(0));
		int koopaGraph = (9+3+1)+(7+2+1)+(7+3+5);
		assertEquals(3, (int)agentIDMap.get(flameManGraph));
		int lakituGraph = (9+5)+(3+2+1)+(8+2);
		assertEquals(6, (int)agentIDMap.get(flameManGraph+koopaGraph));
		int tomahawkManGraph = (9+9+5+3)+(6+1)+(6+1)+(6+1)+(7+1);
		assertEquals(9, (int)agentIDMap.get(flameManGraph+koopaGraph+lakituGraph));
		assertEquals(flameManGraph+koopaGraph+lakituGraph+tomahawkManGraph, db.size());
		assertTrue(isGraphIDConsistent(db));
	}
	
	private <N,E> boolean isGraphIDConsistent(Collection<Graph<N,E>> graphDB){
		int id = 0;
		int fault = 0;
		for(Graph<N,E> g : graphDB){
			if(!g.getName().equals(""+id)) fault++;
			id++;
		}
		return fault == 0;
	}
}
