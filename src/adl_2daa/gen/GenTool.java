package adl_2daa.gen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import adl_2daa.ast.structure.Root;
import adl_2daa.gen.generator.ASTUtility;
import adl_2daa.gen.generator.PostGenProcessor;
import adl_2daa.gen.generator.Skeleton;
import adl_2daa.gen.signature.FileIterator;
import adl_2daa.gen.testtool.TestInitializer;
import adl_2daa.tool.Parser;

public class GenTool {

	public static double orderSup = 0.02;
	public static double interStateSup = 0.02;
	public static double parallelSup = 0.03;
	public static double interEntitySup = 0.21;
	public static double nestingSup = 0.01;
	
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
	 * Move all scripts that are static enemy from root/PreSortSkel to root/StaticSkel. Existing files will be overwritten
	 */
	public static void sortOutStaticSkel(String tier){
		initTool();
		if(tier != null && !tier.isEmpty()){
			tier = "/"+tier;
		}
		FileIterator it = new FileIterator();
		it.trackFiles(new File("PreSortSkel"+tier));
		File staticSkelFolder = new File("StaticSkel"+tier);
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
		String tierSubdir = "";
		if(tier != null && !tier.isEmpty()){
			tierSubdir = "/"+tier;
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
		String tierSubdir = "";
		if(tier != null && !tier.isEmpty()){
			tierSubdir = "/"+tier;
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
	
	private static void saveSingleSkel(Skeleton skel, String dir, String id, String tier) throws IOException{
		String subdirName = Character.toUpperCase(tier.charAt(0)) + tier.substring(1);
		int hp = 2;
		if(tier.equalsIgnoreCase("elite")){
			hp = 8;
		}else if(tier.equalsIgnoreCase("miniboss")){
			hp = 12;
		}else if(tier.equalsIgnoreCase("boss")){
			hp = 24;
		}
		skel.dirtyIdentifierChange("Agent"+id+"_"+tier.toLowerCase());
		skel.dirtyMutateInitialHp(hp);
		skel.saveAsScript(new File(dir+"/"+subdirName));
	}
	
	private static void saveMultipleSkel(Skeleton skel, String dir, String id) throws IOException{
		skel.dirtyIdentifierChange("Agent"+id+"_enemy");
		skel.dirtyMutateInitialHp(2);
		skel.saveAsScript(new File(dir+"/Enemy"));
		skel.dirtyIdentifierChange("Agent"+id+"_elite");
		skel.dirtyMutateInitialHp(8);
		skel.saveAsScript(new File(dir+"/Elite"));
		skel.dirtyIdentifierChange("Agent"+id+"_miniboss");
		skel.dirtyMutateInitialHp(12);
		skel.saveAsScript(new File(dir+"/Miniboss"));
		skel.dirtyIdentifierChange("Agent"+id+"_boss");
		skel.dirtyMutateInitialHp(24);
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
