package adl_2daa.gen.mining0;

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
import adl_2daa.tool.Parser;
import de.parsemis.graph.Graph;

public class DatabaseCreator {

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
	
	private void loadScriptAsAST(File file){
		try{
			BufferedReader buf = new BufferedReader(new FileReader(file));
			String script="",line;
			while((line=buf.readLine()) != null){
				script += line+"\n";
			}
			buf.close();
			script = script.trim();
			Parser parser = new Parser();
			agentFile.add(parser.parse(script));
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	/**
	 * Create database for order mining from loaded agent file. There are 3 types of DB
	 * depends on sequences used in creation.
	 * @param stateType 0:.init 1:.des 2:states
	 */
	public List<List<String>>[] createDatabaseForOrder(int stateType){
		List<List<List<String>>> db = new ArrayList<List<List<String>>>();
		EncodedSequence eSeq;
		for(Root root : agentFile){
			for(Agent agent : root.getRelatedAgents()){
				switch(stateType){
				case 0: //Init
					if(agent.getInit() != null){
						eSeq = ADLSequenceEncoder.instance.parseAsEncodedSequence(agent.getInit(), false);
						db.add(eSeq.toSequence());
					}
					break;
				case 1: //Des
					if(agent.getDes() != null){
						eSeq = ADLSequenceEncoder.instance.parseAsEncodedSequence(agent.getDes(), false);
						db.add(eSeq.toSequence());
					}
					break;
				default: //Update
					for(State state : agent.getStates()){
						for(Sequence astSeq : state.getSequences()){
							eSeq = ADLSequenceEncoder.instance.parseAsEncodedSequence(astSeq, false);
							db.add(eSeq.toSequence());
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
		
		for(Root root : agentFile){
			for(Agent agent : root.getRelatedAgents()){
				int stateCount = agent.getStates().size();
				EncodedAgent eAgent = new EncodedAgent(agent, true);
				for(EncodedState eState : eAgent.states){
					for(EncodedSequence eSeq : eState.sequences){
						for(EncodedSequence eFlow : eSeq.allFlowToTransition){
							String targetState = eFlow.identifier;
							if(targetState.equals("des")){
								//System.out.println(eAgent.identifier+"."+eState.identifier+"."+eSeq.identifier+" -> .des");
								leftdbDes.add(eFlow.toSequence());
								rightdbDes.add(eAgent.des.toSequence());
								tagDes.add(stateCount);
							}else{
								EncodedState targetEstate = eAgent.getStateByIdentifier(targetState);
								//System.out.println(eAgent.identifier+"."+eState.identifier+"."+eSeq.identifier+" -> ."+targetState+":"+targetEstate.sequences.size());
								for(EncodedSequence targetEseq : targetEstate.sequences){
									leftdb.add(eFlow.toSequence());
									rightdb.add(targetEseq.toSequence());
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
		
		for(Root root : agentFile){
			EncodedRoot eRoot = new EncodedRoot(root);
			for(EncodedAgent eAgent : eRoot.relatedAgents){
				for(EncodedState eState : eAgent.states){
					graph.createNewGraph(eState.identifier);
					int rootNode = graph.addNode(ADLSequenceEncoder.impossibleAction);
					for(EncodedSequence eSeq : eState.sequences){
						int seqRootNode = graph.addNode(ADLSequenceEncoder.impossibleAction);
						graph.addEdge(rootNode, seqRootNode, 0, true);
						for(EncodedAction eAct : eSeq.eActList){
							graph.addEdge(seqRootNode, graph.addNode(eAct.toItem()), 0, true);
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
		
		//TODO: A state that spawn the same agent multiple times will cause
		//duplicate graph, is this acceptable???
		//also, we do not consider Spawn in .des, is this Ok? -- now ok
		for(Root root : agentFile){
			EncodedRoot eRoot = new EncodedRoot(root);
			for(EncodedAgent eAgent : eRoot.relatedAgents){
				for(EncodedState eState : eAgent.states){
					for(String spawnableAgentName : eState.getAllSpawnableEntity()){
						EncodedAgent spawnableAgent = eRoot.getAgentByIdentifier(spawnableAgentName);
						EncodedState childInitialState = spawnableAgent.states.get(0);
						if(childInitialState.sequences.size() == 0)
							break;
						
						graph.createNewGraph(eState.identifier);
						int rootNode = graph.addNode(ADLSequenceEncoder.impossibleAction);
						
						//Spawner parallel sequence
						for(EncodedSequence eSeq : eState.sequences){
							int seqRootNode = graph.addNode(ADLSequenceEncoder.impossibleAction);
							graph.addEdge(rootNode, seqRootNode, 0, true);
							for(EncodedAction eAct : eSeq.eActList){
								graph.addEdge(seqRootNode, graph.addNode(eAct.toItem()), 0, true);
							}
						}
						
						//Child parallel sequence
						for(EncodedSequence eSeq : childInitialState.sequences){
							int seqRootNode = graph.addNode(ADLSequenceEncoder.impossibleAction);
							graph.addEdge(rootNode, seqRootNode, 1, true);
							for(EncodedAction eAct : eSeq.eActList){
								graph.addEdge(seqRootNode, graph.addNode(eAct.toItem()), 1, true);
							}
						}
						
						//Additional info
						graph.addEdge(rootNode, 
								graph.addNode(new String(new byte[]{(byte)childInitialState.sequences.size()}, StandardCharsets.US_ASCII)), 
								2, true);
						
						db.add(graph.finishGraph());
					}
				}
			}
		}
		
		return db;
	}
	
	public Collection<Graph<Integer,Integer>> createDatabaseForNesting(){
		Collection<Graph<Integer,Integer>> db = new LinkedList<Graph<Integer,Integer>>();
		
		for(Root root : agentFile){
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
