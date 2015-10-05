package adl_2daa.gen.encoder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import parsemis.extension.GraphCreationHelper;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;
import adl_2daa.gen.signature.FileIterator;
import adl_2daa.tool.Parser;
import de.parsemis.graph.Graph;

public class DatabaseCreator {

	private FileIterator it = new FileIterator();
	
	/**
	 * This method must be called before performing mining process
	 * to store all references to dataset
	 */
	public void load(String directory){
		File dir = new File(directory);
		if(!dir.isDirectory()){
			System.out.println("Specified directory is not directory");
			return;
		}
		it.trackFiles(dir);
	}
	
	/*
	private List<Root> agentFile;
	
	public DatabaseCreator(){
		agentFile = new ArrayList<Root>();
	}
	
	public void load(String directory, boolean searchSubdir){
		File dir = new File(directory);
		if(!dir.isDirectory()){
			System.out.println("Specified directory is not directory");
			return;
		}
		loadInDir(dir, searchSubdir);
	}

	private void loadInDir(File dir, boolean searchSubdir){
		for(File f : dir.listFiles()){
			if(f.isDirectory()){
				if(searchSubdir) loadInDir(f, searchSubdir);
			}else{
				loadScriptAsAST(f);
			}
		}
	}
	*/
	
	private Root loadScriptAsAST(File file){
		try{
			BufferedReader buf = new BufferedReader(new FileReader(file));
			String script="",line;
			while((line=buf.readLine()) != null){
				script += line+"\n";
			}
			buf.close();
			script = script.trim();
			Parser parser = new Parser();
			return parser.parse(script);
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Create database for order mining from loaded agent file. There are 3 types of DB
	 * depends on sequences used in creation.
	 * @param stateType 0:.init 1:.des 2:states
	 */
	public List<List<String>>[] createDatabaseForOrder(int stateType){
		List<List<List<String>>> db = new ArrayList<List<List<String>>>();
		ADLSequence eSeq;
		ADLSequenceEncoder.instance.setAnalyzeFlow(false);
		it.reset();
		
		while(it.hasNext()){
			Root root = loadScriptAsAST(it.next());
			for(Agent agent : root.getRelatedAgents()){
				switch(stateType){
				case 0: //Init
					if(agent.getInit() != null){
						eSeq = ADLSequenceEncoder.instance.encode(agent.getInit());
						db.add(eSeq.toMinerSequence());
					}
					break;
				case 1: //Des
					if(agent.getDes() != null){
						eSeq = ADLSequenceEncoder.instance.encode(agent.getDes());
						db.add(eSeq.toMinerSequence());
					}
					break;
				default: //Update
					for(State state : agent.getStates()){
						for(Sequence astSeq : state.getSequences()){
							eSeq = ADLSequenceEncoder.instance.encode(astSeq);
							db.add(eSeq.toMinerSequence());
						}
					}
				}
			}
		}
		
		@SuppressWarnings("unchecked")
		List<List<String>>[] dbAry = db.toArray(new List[db.size()]);
		return dbAry;
	}
	
	/**
	 * Create database for inter-state order mining. Return Object[6] where <br/>
	 * 0,1,2: left,right,tag db for flows that end with "goto"
	 * 3,4,5: flows end with "despawn"
	 */
	public Object[] createDatabaseForInterStateOrder(){
		List<List<List<String>>> leftdb = new ArrayList<List<List<String>>>();
		List<List<List<String>>> leftdbDes = new ArrayList<List<List<String>>>();
		List<List<List<String>>> rightdb = new ArrayList<List<List<String>>>();
		List<List<List<String>>> rightdbDes = new ArrayList<List<List<String>>>();
		List<Integer> tag = new ArrayList<Integer>();
		List<Integer> tagDes = new ArrayList<Integer>();
		
		ADLSequenceEncoder.instance.setAnalyzeFlow(true);
		it.reset();
		
		while(it.hasNext()){
			Root root = loadScriptAsAST(it.next());
			for(Agent agent : root.getRelatedAgents()){
				int stateCount = agent.getStates().size();
				ADLAgent eAgent = new ADLAgent(agent);
				for(ADLState eState : eAgent.states){
					for(ADLSequence eSeq : eState.sequences){
						for(ADLSequence eFlow : eSeq.allFlowToTerminal){
							String targetState = eFlow.identifier;
							if(targetState.equals("des")){
								//System.out.println(eAgent.identifier+"."+eState.identifier+"."+eSeq.identifier+" -> .des");
								leftdbDes.add(eFlow.toMinerSequence());
								rightdbDes.add(eAgent.des.toMinerSequence());
								tagDes.add(stateCount);
							}else{
								ADLState targetEstate = eAgent.getStateByIdentifier(targetState);
								//System.out.println(eAgent.identifier+"."+eState.identifier+"."+eSeq.identifier+" -> ."+targetState+":"+targetEstate.sequences.size());
								for(ADLSequence targetEseq : targetEstate.sequences){
									leftdb.add(eFlow.toMinerSequence());
									rightdb.add(targetEseq.toMinerSequence());
									tag.add(stateCount);
								}
							}
						}
					}
				}
			}
		}
		
		Object[] dbAry = new Object[6];
		
		dbAry[0] = leftdb.toArray(new List[leftdb.size()]);
		dbAry[1] = rightdb.toArray(new List[rightdb.size()]);
		dbAry[2] = tag.toArray(new Integer[tag.size()]);
		
		dbAry[3] = leftdbDes.toArray(new List[leftdbDes.size()]);
		dbAry[4] = rightdbDes.toArray(new List[rightdbDes.size()]);
		dbAry[5] = tagDes.toArray(new Integer[tagDes.size()]);
		
		return dbAry;
	}
	
	public Collection<Graph<String,Integer>> createDatabaseForParallel(){
		Collection<Graph<String,Integer>> db = new LinkedList<Graph<String,Integer>>();
		GraphCreationHelper<String, Integer> graph = 
				new GraphCreationHelper<String, Integer>();
		
		ADLSequenceEncoder.instance.setAnalyzeFlow(false);
		it.reset();
		
		while(it.hasNext()){
			Root root = loadScriptAsAST(it.next());
			ADLRoot eRoot = new ADLRoot(root);
			for(ADLAgent eAgent : eRoot.agents){
				for(ADLState eState : eAgent.states){
					graph.createNewGraph(eAgent.identifier+"."+eState.identifier);
					int rootNode = graph.addNode(ADLSequenceEncoder.dummyEncodedAction);
					for(ADLSequence eSeq : eState.sequences){
						int seqRootNode = graph.addNode(ADLSequenceEncoder.dummyEncodedAction);
						graph.addEdge(rootNode, seqRootNode, 
								EncodeTable.STATE_SEQUENCE_EDGE, true);
						for(String eAct : eSeq.encodedSequence){
							graph.addEdge(seqRootNode, graph.addNode(eAct), 
									EncodeTable.SEQUENCE_ACTION_EDGE, true);
						}
					}
					db.add(graph.finishGraph());
				}
			}
		}
		
		return db;
	}
	
	public Collection<Graph<String,Integer>> createDatabaseForInterEntityParallel(){
		Collection<Graph<String,Integer>> db = new LinkedList<Graph<String,Integer>>();
		GraphCreationHelper<String, Integer> graph = 
				new GraphCreationHelper<String, Integer>();
		
		it.reset();
		//TODO: A state that spawn the same agent multiple times will cause
		//duplicate graph, is this acceptable???
		//also, we do not consider Spawn in .des, is this Ok? -- now ok
		while(it.hasNext()){
			Root root = loadScriptAsAST(it.next());
			ADLRoot eRoot = new ADLRoot(root);
			for(ADLAgent eAgent : eRoot.agents){
				for(ADLState eState : eAgent.states){
					for(String spawnableAgentName : eState.getAllSpawnableEntity()){
						ADLAgent spawnableAgent = eRoot.getAgentByIdentifier(spawnableAgentName);
						ADLState childInitialState = spawnableAgent.states.get(0);
						if(childInitialState.sequences.size() == 0)
							break;
						
						graph.createNewGraph(eState.identifier);
						int rootNode = graph.addNode(ADLSequenceEncoder.dummyEncodedAction);
						
						//Spawner parallel sequence
						for(ADLSequence eSeq : eState.sequences){
							int seqRootNode = graph.addNode(ADLSequenceEncoder.dummyEncodedAction);
							graph.addEdge(rootNode, seqRootNode, EncodeTable.STATE_SEQUENCE_EDGE, true);
							for(String eAct : eSeq.encodedSequence){
								graph.addEdge(seqRootNode, graph.addNode(eAct), EncodeTable.SEQUENCE_ACTION_EDGE, true);
							}
						}
						
						//Child parallel sequence
						for(ADLSequence eSeq : childInitialState.sequences){
							int seqRootNode = graph.addNode(ADLSequenceEncoder.dummyEncodedAction);
							graph.addEdge(rootNode, seqRootNode, EncodeTable.STATE_SEQUENCE_EDGE, true);
							for(String eAct : eSeq.encodedSequence){
								graph.addEdge(seqRootNode, graph.addNode(eAct), EncodeTable.SEQUENCE_ACTION_OTHER_ENTITY_EDGE, true);
							}
						}
						
						//Additional info
						graph.addEdge(rootNode, 
								graph.addNode(new String(new byte[]{(byte)childInitialState.sequences.size()}, StandardCharsets.US_ASCII)), 
								EncodeTable.TAG_EDGE, true);
						
						db.add(graph.finishGraph());
					}
				}
			}
		}
		
		return db;
	}
	
	public Collection<Graph<Integer,Integer>> createDatabaseForNesting(){
		Collection<Graph<Integer,Integer>> db = new LinkedList<Graph<Integer,Integer>>();
		
		it.reset();
		while(it.hasNext()){
			Root root = loadScriptAsAST(it.next());
			for(Agent agent : root.getRelatedAgents()){
				if(agent.getInit() != null){
					db.addAll(
							ADLNestingEncoder.instance.parseAsGraphCollection(agent.getInit())
							);
				}
				if(agent.getDes() != null){
					db.addAll(
							ADLNestingEncoder.instance.parseAsGraphCollection(agent.getDes())
							);
				}
				for(State state : agent.getStates()){
					for(Sequence seq : state.getSequences()){
						db.addAll(
								ADLNestingEncoder.instance.parseAsGraphCollection(seq)
								);
					}
				}
			}
		}
		
		return db;
	}
}

