package adl_2daa.gen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import adl_2daa.ast.structure.Root;
import adl_2daa.gen.encoder.DatabaseCreator;
import adl_2daa.gen.generator.ASTUtility;
import adl_2daa.gen.generator.PostGenProcessor;
import adl_2daa.gen.generator.Skeleton;
import adl_2daa.gen.profile.AgentProfile;
import adl_2daa.gen.signature.FileIterator;
import adl_2daa.gen.testtool.TestInitializer;
import adl_2daa.tool.Parser;

public class GenTool {

	public static double orderSup = 0.02;
	public static double interStateSup = 0.02;
	public static double parallelSup = 0.03;
	public static double interEntitySup = 0.21;
	public static double nestingSup = 0.01;
	
	public static int enemyHP = 2; //Prev=2
	public static int eliteHP = 12; //Prev=8
	public static int minibossHP = 18; //Prev=12
	public static int bossHP = 26; //Prev=24
	
	public static void generateRandom(int startID, int agentCount){
		initTool();
		
		//Checking dataset and generate range data for each ADL component
		DatabaseCreator dbCreator = new DatabaseCreator();
		List<AgentProfile[]> profiles = dbCreator.getAllAgentProfile("data");
		int[][] usageRange = new int[6][2]; //0:Agent, 1:State, 2:Sequence, 3:Action, 4:Condition, 5:Loop | 0:Min, 1:Max
		for(int i=0; i<6; i++){
			usageRange[i][0] = Integer.MAX_VALUE;
			usageRange[i][1] = Integer.MIN_VALUE;
		}
		for(AgentProfile[] skelProfile : profiles){
			//Agent
			setMinMax(usageRange[0], skelProfile.length);
			for(AgentProfile profile : skelProfile){
				//State
				setMinMax(usageRange[1], profile.getStructureInfo().length);
				
				for(int stateIdx=0; stateIdx<profile.getStructureInfo().length; stateIdx++){
					int seqCount = profile.getStructureInfo()[stateIdx];
					//Sequence
					setMinMax(usageRange[2], seqCount);
					
					for(int seqIdx=0; seqIdx<seqCount; seqIdx++){
						//Action
						setMinMax(usageRange[3], profile.getActionUsageInfo()[stateIdx][seqIdx][0]);
						//Condition
						setMinMax(usageRange[4], profile.getActionUsageInfo()[stateIdx][seqIdx][1]);
						//Loop
						setMinMax(usageRange[5], profile.getActionUsageInfo()[stateIdx][seqIdx][2]);
					}
				}
				
				//.des block
				setMinMax(usageRange[3], profile.getDesActionUsage()[0]);
				setMinMax(usageRange[4], profile.getDesActionUsage()[1]);
				setMinMax(usageRange[5], profile.getDesActionUsage()[2]);
			}
		}
		
		assert(usageRange[0][0] > 0);
		assert(usageRange[1][0] > 0);
		assert(usageRange[2][0] > 0);
		
		int agentCounter = 0;
		while(agentCounter < startID+agentCount){
			try{
				//Generate random skeleton profile
				AgentProfile[] genSkelProfile = new AgentProfile[ASTUtility.randomRange(usageRange[0][0], usageRange[0][1])];
				for(int i=0; i<genSkelProfile.length; i++){
					genSkelProfile[i] = new AgentProfile();
					genSkelProfile[i].setMainAgent(i == 0);
					if(i == 0){
						genSkelProfile[i].setRootName("Agent"+agentCounter);
					}
					
					int[] desInfo = new int[3];
					for(int idx=0; idx<3; idx++){
						desInfo[idx] = ASTUtility.randomRange(usageRange[idx+3][0], usageRange[idx+3][1]);
					}
					genSkelProfile[i].setDesActionUsage(desInfo);
					
					int[] stateInfo = new int[ASTUtility.randomRange(usageRange[1][0], usageRange[1][1])];
					int[][][] actionInfo = new int[stateInfo.length][][];
					for(int stateIdx=0; stateIdx<stateInfo.length; stateIdx++){
						stateInfo[stateIdx] = ASTUtility.randomRange(usageRange[2][0], usageRange[2][1]);
						actionInfo[stateIdx] = new int[stateInfo[stateIdx]][];
						for(int seqIdx=0; seqIdx<actionInfo[stateIdx].length; seqIdx++){
							actionInfo[stateIdx][seqIdx] = new int[3];
							for(int idx=0; idx<3; idx++){
								actionInfo[stateIdx][seqIdx][idx] = ASTUtility.randomRange(usageRange[idx+3][0], usageRange[idx+3][1]);
							}
						}
					}
					genSkelProfile[i].setStructureInfo(true, stateInfo);
					genSkelProfile[i].setActionUsageInfo(actionInfo);
				}
				
				//generate random skel from profile
				Skeleton skel = new Skeleton();
				skel.randomlyGenerate(genSkelProfile);
				
				for(int i=0; i<4; i++){
					int hp = 1;
					String subdirName = "";
					switch(i){
					case 0: hp = enemyHP; subdirName = "RandomEnemy"; break;
					case 1: hp = eliteHP; subdirName = "RandomElite"; break;
					case 2: hp = minibossHP; subdirName = "RandomMiniboss"; break;
					case 3: hp = bossHP; subdirName = "RandomBoss"; break;
					}
					skel.dirtyMutateInitialHp(hp);
					skel.saveAsScript(new File("result_script_random/"+subdirName));
				}
				
				agentCounter++;
			}catch(AssertionError err){
				err.printStackTrace();
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
	}
	
	private static void setMinMax(int[] rangeData, int value){
		if(value < rangeData[0])
			rangeData[0] = value;
		if(value > rangeData[1])
			rangeData[1] = value;
	}
	
	public static void initTool(){
		ASTUtility.resetRandomizer();
		TestInitializer.init();
	}
	
	public static Miner setupMinerAndResult(){
		Miner gen = new Miner();
		gen.initialize("data");
		gen.clearResult();
		
		System.out.println("Mine inter entity parallel");
		gen.mineInterEntityParallelSequence(interEntitySup, false);
		
		System.out.println("Mine parallel");
		gen.mineParallelSequence(parallelSup, false);
		
		System.out.println("Mine inter state order");
		gen.mineInterStateOrder(interStateSup, false);
		
		System.out.println("Mine order");
		gen.mineSequenceOrdering(orderSup, false, true);
		
		System.out.println("Mine nesting");
		gen.mineNesting(nestingSup, false);
		
		return gen;
	}
	
	/**
	 * Generate agents based on mined data from root/data and save in root/result_script or root/result_script_empty
	 * if it is an empty agent. Existing files will be overwritten
	 */
	public static void generate(int startID, int agentCount){
		initTool();
		Miner miner = setupMinerAndResult();
		List<Integer> emptyAgentList = new LinkedList<Integer>();
		int agentCounter = startID;
		while(agentCounter < startID+agentCount){
			try{
				Skeleton skel = new Skeleton();
				skel.fullyGenerate(miner,"Agent"+agentCounter);
				boolean emptySkel = false;
				if(skel.isEmptySkeleton()){
					emptyAgentList.add(agentCounter);
					emptySkel = true;
				}else{
					skel.reduceWait();
					skel.removePhasing();
				}
				String filename = ""+agentCounter;
				if(emptySkel){
					filename = "_e" + filename;
				}
				saveMultipleSkel(skel, emptySkel ? "result_script_empty" : "result_script",  filename);
				agentCounter++;
			}catch(IOException ioex){
				ioex.printStackTrace();
				break;
			}catch(AssertionError err){
				err.printStackTrace();
			}catch(OutOfMemoryError noMem){
				noMem.printStackTrace();
				System.gc();
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
	}
	
	/**
	 * Move all scripts that are static enemy from root/PreSortSkel to root/StaticSkel. Existing files will be overwritten.
	 */
	public static void sortOutStaticSkel(String tier){
		initTool();
		if(tier != null && !tier.isEmpty()){
			tier = "/"+capitalize(tier);
		}
		FileIterator it = new FileIterator();
		it.trackFiles(new File("PreSortSkel"+tier));
		File staticSkelFolder = new File("StaticSkel"+tier);
		staticSkelFolder.mkdirs();
		while(it.hasNext()){
			File rootFile = it.next();
			Root root = loadScriptAsAST(rootFile);
			String id = rootFile.getName().replaceAll("\\D+","");
			Skeleton skel = new Skeleton("Agent"+id, root);
			if(PostGenProcessor.isStaticEnemy(skel)){
				try {
					FileUtils.moveFile(rootFile, new File(staticSkelFolder, rootFile.getName()));
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("Failed to sort out "+rootFile.getName());
				}
			}
		}
	}
	
	/**
	 * Load all scripts in root/StaticSkel and save the improved version in root/improve_static_script. Existing files will be overwritten
	 */
	public static void improveStaticSkel(boolean saveMultipleSkel, String tier){
		initTool();
		if(!(new File("improve_static_script")).exists()){
			(new File("improve_static_script/Enemy")).mkdirs();
			(new File("improve_static_script/Elite")).mkdirs();
			(new File("improve_static_script/Miniboss")).mkdirs();
			(new File("improve_static_script/Boss")).mkdirs();
		}
		String tierSubdir = "";
		if(tier != null && !tier.isEmpty()){
			tierSubdir = "/"+capitalize(tier);
		}
		Miner miner = setupMinerAndResult();
		PostGenProcessor.filterOutStaticRelation(miner);
		FileIterator it = new FileIterator();
		it.trackFiles(new File("StaticSkel"+tierSubdir));
		while(it.hasNext()){
			File rootFile = it.next();
			Root root = loadScriptAsAST(rootFile);
			String id = rootFile.getName().replaceAll("\\D+","");
			Skeleton skel = new Skeleton("Agent"+id, root);
			while(true){
				try{
					PostGenProcessor.improveSkeleton(skel, miner);
					if(saveMultipleSkel){
						saveMultipleSkel(skel, "improve_static_script", ""+id);
					}else{
						saveSingleSkel(skel, "improve_static_script", ""+id, tier);
					}
					break;
				}catch(AssertionError aex){
					aex.printStackTrace();
				}catch(OutOfMemoryError oex){
					oex.printStackTrace();
					System.gc();
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Load all scripts in root/HighSpawnSkel and save the improved version to root/improve_wait_script. Existing files are overwritten
	 */
	public static void improveWaitDelay(float multiplier, boolean saveMultipleSkel, String tier){
		initTool();
		if(!(new File("improve_wait_script")).exists()){
			(new File("improve_wait_script/Enemy")).mkdirs();
			(new File("improve_wait_script/Elite")).mkdirs();
			(new File("improve_wait_script/Miniboss")).mkdirs();
			(new File("improve_wait_script/Boss")).mkdirs();
		}
		String tierSubdir = "";
		if(tier != null && !tier.isEmpty()){
			tierSubdir = "/"+capitalize(tier);
		}
		FileIterator it = new FileIterator();
		it.trackFiles(new File("HighSpawnSkel"+tierSubdir));
		while(it.hasNext()){
			File rootFile = it.next();
			Root root = loadScriptAsAST(rootFile);
			String id = rootFile.getName().replaceAll("\\D+","");
			Skeleton skel = new Skeleton("Agent"+id, root);
			while(true){
				try{
					PostGenProcessor.improveWaitDelay(skel.getRoot(), multiplier);
					if(saveMultipleSkel){
						saveMultipleSkel(skel, "improve_wait_script", ""+id);
					}else{
						saveSingleSkel(skel, "improve_wait_script", ""+id, tier);
					}
					break;
				}catch(AssertionError aex){
					aex.printStackTrace();
				}catch(OutOfMemoryError oex){
					oex.printStackTrace();
					System.gc();
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}
		}
	}
	
	public static void mutateHP(String dir, String tier, String targetDir){
		if(!(new File(targetDir)).exists()){
			(new File(targetDir+"/Enemy")).mkdirs();
			(new File(targetDir+"/Elite")).mkdirs();
			(new File(targetDir+"/Miniboss")).mkdirs();
			(new File(targetDir+"/Boss")).mkdirs();
		}
		String tierSubdir = "";
		if(tier != null && !tier.isEmpty()){
			tierSubdir = "/"+capitalize(tier);
		}
		FileIterator it = new FileIterator();
		it.trackFiles(new File(dir+tierSubdir));
		while(it.hasNext()){
			File rootFile = it.next();
			Root root = loadScriptAsAST(rootFile);
			String id = rootFile.getName().replaceAll("\\D+","");
			Skeleton skel = new Skeleton("Agent"+id, root);
			while(true){
				try{
					saveSingleSkel(skel, targetDir, ""+id, tier);
					break;
				}catch(AssertionError aex){
					aex.printStackTrace();
				}catch(OutOfMemoryError oex){
					oex.printStackTrace();
					System.gc();
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}
		}
	}
	
	private static String capitalize(String str){
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}
	
	private static void saveSingleSkel(Skeleton skel, String dir, String id, String tier) throws IOException{
		String subdirName = capitalize(tier);
		int hp = enemyHP;
		if(tier.equalsIgnoreCase("elite")){
			hp = eliteHP;
		}else if(tier.equalsIgnoreCase("miniboss")){
			hp = minibossHP;
		}else if(tier.equalsIgnoreCase("boss")){
			hp = bossHP;
		}
		skel.dirtyIdentifierChange("Agent"+id+"_"+tier.toLowerCase());
		skel.dirtyMutateInitialHp(hp);
		skel.saveAsScript(new File(dir+"/"+subdirName));
	}
	
	private static void saveMultipleSkel(Skeleton skel, String dir, String id) throws IOException{
		skel.dirtyIdentifierChange("Agent"+id+"_enemy");
		skel.dirtyMutateInitialHp(enemyHP);
		skel.saveAsScript(new File(dir+"/Enemy"));
		skel.dirtyIdentifierChange("Agent"+id+"_elite");
		skel.dirtyMutateInitialHp(eliteHP);
		skel.saveAsScript(new File(dir+"/Elite"));
		skel.dirtyIdentifierChange("Agent"+id+"_miniboss");
		skel.dirtyMutateInitialHp(minibossHP);
		skel.saveAsScript(new File(dir+"/Miniboss"));
		skel.dirtyIdentifierChange("Agent"+id+"_boss");
		skel.dirtyMutateInitialHp(bossHP);
		skel.saveAsScript(new File(dir+"/Boss"));
	}
	
	private static Root loadScriptAsAST(File file){
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
}
