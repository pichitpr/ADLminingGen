package adl_2daa.gen.encoder;

import static org.junit.Assert.assertEquals;

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
			dbCreator.load("test/ext/minidb", true);
		}
	}

	@Test
	public void testCreateDBforOrder() {
		List<List<String>>[] db = dbCreator.createDatabaseForOrder(0);
		assertEquals(14, db.length);
		
		db = dbCreator.createDatabaseForOrder(1);
		assertEquals(3, db.length);
		
		db = dbCreator.createDatabaseForOrder(2);
		assertEquals(24, db.length);
	}
	
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
		
		//Transition with "Despawn"
		assertEquals(2, ((List[])db[3]).length);
		assertEquals(2, ((List[])db[4]).length);
		assertEquals(2, ((Integer[])db[5]).length);
		
		tag = (Integer[])db[5];
		assertEquals(1, (int)tag[0]); //FlameShield 1 state
		assertEquals(1, (int)tag[1]); //SpinyEgg 1 state
	}

	@Test
	public void testCreateDBForParallel(){
		Collection<Graph<String,Integer>> db = dbCreator.createDatabaseForParallel();
		
		//19 states across 4 files
		assertEquals(19, db.size());
		
		//Agent files are converted to graph in lexicographical order
		Iterator<Graph<String,Integer>> it = db.iterator();
		while(it.hasNext()){
			Graph<String,Integer> graph = it.next();
			switch(graph.getID()){
			case 0: //FlameMan.base
				assertEquals(1+1+5, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				break;
			case 1: //FlameMan.flame
				assertEquals(1+1+14, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				break;
			case 2: //FlameMan.bullet
				assertEquals(1+2+3+1, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				break;
			case 3: //FlameShield.state0
				assertEquals(1+1+6, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				break;
			case 4: //FlameShot.state0
				assertEquals(1+1+1, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				break;
			case 5: //ShellKoopa.state0
				assertEquals(1+2+1+1, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				break;
			case 6: //Koopa.state0
				assertEquals(1+1+2, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				break;
			case 7: //KoopaShell.state0
				assertEquals(1+1+2, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				break;
			case 8: //KoopaShell.state1
				assertEquals(1+2+2+2, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				break;
			case 9: //Lakitu.state0
				assertEquals(1+2+2+2, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				break;
			case 10: //SpinyEgg.state0
				assertEquals(1+1+1, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				break;
			case 11: //Spiny.state0
				assertEquals(1+1+2, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				break;
			case 12: //TomahawkMan.base
				assertEquals(1+2+6+1, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				break;
			case 13: //TomahawkMan.feather
				assertEquals(1+1+5, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				break;
			case 14: //TomahawkMan.tomahawk
				assertEquals(1+1+3, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				break;
			case 15: //FeatherBullet1.state0
				assertEquals(1+1+1, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				break;
			case 16: //FeatherBullet2.state0
				assertEquals(1+1+1, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				break;
			case 17: //FeatherBullet3.state0
				assertEquals(1+1+1, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				break;
			case 18: //Tomahawk.state0
				assertEquals(1+1+1, graph.getNodeCount());
				assertEquals(graph.getNodeCount()-1, graph.getEdgeCount());
				break;
			}
		}
	}
	
	@Test
	public void testCreateDBForInterEntityParallel(){
		Collection<Graph<String,Integer>> db = dbCreator.createDatabaseForInterEntityParallel();

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
		
		//Agent files are converted to graph in lexicographical order
		Iterator<Graph<String,Integer>> it = db.iterator();
		while(it.hasNext()){
			Graph<String,Integer> graph = it.next();
			switch(graph.getID()){
			case 0: case 1: case 2: case 3: case 4: case 5:
				//FlameMan.flame(1) -> Spawn .FlameShield(1) *6
				assertEquals(2+15+7, graph.getNodeCount());
				break;
			case 6:
				//FlameMan.bullet(1) -> Spawn .FlameShot(1) *1
				assertEquals(2+8+2+2, graph.getNodeCount());
				break;
			case 7:
				//Lakitu.state0(2) -> Spawn .SpinyEgg(1)
				assertEquals(2+3+3+2, graph.getNodeCount());
				break;
			case 8:
				//TomahawkMan.feather -> Spawn .FeatherBullet1 (1)
				assertEquals(2+6+2, graph.getNodeCount());
				break;
			case 9:
				//TomahawkMan.feather -> Spawn .FeatherBullet2 (1)
				assertEquals(2+6+2, graph.getNodeCount());
				break;
			case 10:
				//TomahawkMan.feather -> Spawn .FeatherBullet3 (1)
				assertEquals(2+6+2, graph.getNodeCount());
				break;
			case 11:
				//TomahawkMan.tomahawk -> Spawn .Tomahawk(1) *1
				assertEquals(2+4+2, graph.getNodeCount());
				break;
			}
		}
	}
	
	@Test
	public void testCreateDBforNesting(){
		Collection<Graph<Integer,Integer>> db = dbCreator.createDatabaseForNesting();
		int flameManGraph = (9+7+14+3+1)+(8+6)+(7+1);
		int koopaGraph = (9+3+1)+(7+2+1)+(7+3+5);
		int lakituGraph = (9+5)+(3+2+1)+(8+2);
		int tomahawkManGraph = (9+9+5+3)+(6+1)+(6+1)+(6+1)+(7+1);
		assertEquals(flameManGraph+koopaGraph+lakituGraph+tomahawkManGraph, db.size());
	}
}
