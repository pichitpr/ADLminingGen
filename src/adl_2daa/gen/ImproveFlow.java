package adl_2daa.gen;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;


public class ImproveFlow {
	
	public static void start(){
		ImproveFlow flow = new ImproveFlow();
		try {
			flow.step0_firstEvaluation();
			flow.step1_staticSortOut();
			flow.step2_improveStatic();
			flow.step3_improveStaticWait(1,5);
			flow.step4_improveWait(1,5);
			flow.saveCSVReport(new File("improvement_result.csv"));
			flow.cleanup();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private final int roundCount = 5;
	
	private final File baseScript = new File("result_script");
	private final File veilProject = new File("Veil");
	private final File log = new File("eval_log");
	
	private final File presortSkel = new File("PreSortSkel");
	private final File staticSkel = new File("StaticSkel");
	private final File improvedStatic = new File("improve_static_script");
	
	private final File highSpawnSkel = new File("HighSpawnSkel");
	private final File improvedWait = new File("improve_wait_script");
	
	private final File veilAgentDB = new File(veilProject, "Profiling/GenDB/Rush");
	private final File veilProfile = new File(veilProject, "Profiling/Profile");
	private final File veilEvalPass = new File(veilProject, "result_script_pass");
	private final File veilEvalFailLow = new File(veilProject, "result_script_fail_low");
	private final File veilEvalFailHigh = new File(veilProject, "result_script_fail_high");
	
	private final File logBaseScript = new File(log, "0_base");
	private final File logBaseProfile = new File(log, "0_base_profile");
	private final File logBaseScriptPass = new File(log, "0_base_script_pass");
	private final File logBaseScriptFailLow = new File(log, "0_base_script_fail/result_script_fail_low");
	private final File logBaseScriptFailHigh = new File(log, "0_base_script_fail/result_script_fail_high");
	
	private final File logStaticFromLow = new File(log, "1_static_script/result_script_fail_low");
	private final File logStaticFromHigh = new File(log, "1_static_script/result_script_fail_high");
	private final File logNonStaticFromLow = new File(log, "1_nonstatic_script/result_script_fail_low");
	private final File logNonStaticFromHigh = new File(log, "1_nonstatic_script/result_script_fail_high");
	
	private final File logImprovedStatic = new File(log, "2_improved_static");
	private final File logImprovedStaticProfile = new File(log, "2_improved_static_profile");
	private final File logImprovedStaticPass = new File(log, "2_improved_static_pass");
	private final File logImprovedStaticFailLow = new File(log, "2_improved_static_fail/result_script_fail_low");
	private final File logImprovedStaticFailHigh = new File(log, "2_improved_static_fail/result_script_fail_high");
	
	private final File logImprovedStaticWait = new File(log, "3_improved_static_wait");
	
	private final File logImprovedWait = new File(log, "4_improved_wait");
	
	private final String logStrImprovedMoreWaitScript =  "_script/more_wait";
	private final String logStrImprovedLessWaitScript = "_script/less_wait";
	private final String logStrImprovedMoreWaitProfile =  "_profile/more_wait";
	private final String logStrImprovedLessWaitProfile = "_profile/less_wait";
	private final String logStrImprovedWaitPass = "_script_pass";
	private final String logStrImprovedWaitFailLow = "_script_fail/result_script_fail_low";
	private final String logStrImprovedWaitFailHigh = "_script_fail/result_script_fail_high";
	
	private void step0_firstEvaluation() throws Exception{		
		//Log base script in result_script
		deleteIfExist(logBaseScript);
		FileUtils.copyDirectory(baseScript, logBaseScript);
		
		//Generate profile and evaluate. Then, save log
		generateAndEvaluateProfile(logBaseScript, logBaseProfile, logBaseScriptPass, logBaseScriptFailLow, logBaseScriptFailHigh);
	}
	
	private void step1_staticSortOut() throws Exception{
		//Sort out script and log. Separated by fail case
		deleteIfExist(presortSkel);
		FileUtils.copyDirectory(logBaseScriptFailLow, presortSkel);
		deleteIfExist(staticSkel);
		GenTool.sortOutStaticSkel("enemy");
		GenTool.sortOutStaticSkel("elite");
		GenTool.sortOutStaticSkel("miniboss");
		GenTool.sortOutStaticSkel("boss");
		deleteIfExist(logStaticFromLow);
		deleteIfExist(logNonStaticFromLow);
		FileUtils.copyDirectory(staticSkel, logStaticFromLow);
		FileUtils.copyDirectory(presortSkel, logNonStaticFromLow);
		
		deleteIfExist(presortSkel);
		FileUtils.copyDirectory(logBaseScriptFailHigh, presortSkel);
		deleteIfExist(staticSkel);
		GenTool.sortOutStaticSkel("enemy");
		GenTool.sortOutStaticSkel("elite");
		GenTool.sortOutStaticSkel("miniboss");
		GenTool.sortOutStaticSkel("boss");
		deleteIfExist(logStaticFromHigh);
		deleteIfExist(logNonStaticFromHigh);
		FileUtils.copyDirectory(staticSkel, logStaticFromHigh);
		FileUtils.copyDirectory(presortSkel, logNonStaticFromHigh);
	}
	
	private void step2_improveStatic() throws Exception{
		//Improve static script and save log
		deleteIfExist(staticSkel);
		FileUtils.copyDirectory(logStaticFromLow, staticSkel);
		FileUtils.copyDirectory(logStaticFromHigh, staticSkel);
		deleteIfExist(improvedStatic);
		GenTool.improveStaticSkel(false, "enemy");
		GenTool.improveStaticSkel(false, "elite");
		GenTool.improveStaticSkel(false, "miniboss");
		GenTool.improveStaticSkel(false, "boss");
		deleteIfExist(logImprovedStatic);
		FileUtils.copyDirectory(improvedStatic, logImprovedStatic);
		
		//Generate and evaluate profile. Then, save log
		generateAndEvaluateProfile(logImprovedStatic, logImprovedStaticProfile, logImprovedStaticPass, 
				logImprovedStaticFailLow, logImprovedStaticFailHigh);
	}
	
	private void step3_improveStaticWait(int startIteration, int maxIteration) throws Exception{
		improveWaitIterate(logImprovedStaticWait, logImprovedStaticFailLow, logImprovedStaticFailHigh, startIteration, maxIteration);
	}
	
	private void step4_improveWait(int startIteration, int maxIteration) throws Exception{
		improveWaitIterate(logImprovedWait, logNonStaticFromLow, logNonStaticFromHigh, startIteration, maxIteration);
	}
	
	private void improveWaitIterate(File rootLogFolder, File firstFailLowScriptDir, File firstFailHighScriptDir, 
			int startIteration, int maxIteration) throws Exception{
		int currentIteration = startIteration;
		while(currentIteration <= maxIteration){
			//Setup pointer to script that requires Wait() improvement
			File requiredMoreWaitScript = null;
			File requiredLessWaitScript = null;
			if(currentIteration == 1){
				requiredMoreWaitScript = firstFailHighScriptDir;
				requiredLessWaitScript = firstFailLowScriptDir;
			}else{
				requiredMoreWaitScript = new File(rootLogFolder, (currentIteration-1)+logStrImprovedWaitFailHigh);
				requiredLessWaitScript = new File(rootLogFolder, (currentIteration-1)+logStrImprovedWaitFailLow);
			}
			File logImprovedMoreWaitScript = new File(rootLogFolder, currentIteration+logStrImprovedMoreWaitScript);
			File logImprovedLessWaitScript = new File(rootLogFolder, currentIteration+logStrImprovedLessWaitScript);
			File logImprovedMoreWaitProfile = new File(rootLogFolder, currentIteration+logStrImprovedMoreWaitProfile);
			File logImprovedLessWaitProfile = new File(rootLogFolder, currentIteration+logStrImprovedLessWaitProfile);
			File logImprovedWaitPass = new File(rootLogFolder, currentIteration+logStrImprovedWaitPass);
			File logImprovedWaitFailLow = new File(rootLogFolder, currentIteration+logStrImprovedWaitFailLow);
			File logImprovedWaitFailHigh = new File(rootLogFolder, currentIteration+logStrImprovedWaitFailHigh);
			
			//Improve scale up Wait() and save log
			deleteIfExist(highSpawnSkel);
			FileUtils.copyDirectory(requiredMoreWaitScript, highSpawnSkel);
			deleteIfExist(improvedWait);
			GenTool.improveWaitDelay(2, false, "enemy");
			GenTool.improveWaitDelay(2, false, "elite");
			GenTool.improveWaitDelay(2, false, "miniboss");
			GenTool.improveWaitDelay(2, false, "boss");
			deleteIfExist(logImprovedMoreWaitScript);
			FileUtils.copyDirectory(improvedWait, logImprovedMoreWaitScript);
			
			//Improve scale down Wait() and save log
			deleteIfExist(highSpawnSkel);
			FileUtils.copyDirectory(requiredLessWaitScript, highSpawnSkel);
			deleteIfExist(improvedWait);
			GenTool.improveWaitDelay(0.75f, false, "enemy");
			GenTool.improveWaitDelay(0.75f, false, "elite");
			GenTool.improveWaitDelay(0.75f, false, "miniboss");
			GenTool.improveWaitDelay(0.75f, false, "boss");
			deleteIfExist(logImprovedLessWaitScript);
			FileUtils.copyDirectory(improvedWait, logImprovedLessWaitScript);
			
			//Generate and evaluate profile. Then, save log
			generateAndEvaluateProfile(logImprovedMoreWaitScript, logImprovedMoreWaitProfile, logImprovedWaitPass, 
					logImprovedWaitFailLow, logImprovedWaitFailHigh);
			//We need tmp as the method will just delete previous log
			File tmpLogPass = new File("tmp_log/improved_wait_pass");
			File tmpLogFailLow = new File("tmp_log/improved_wait_fail_low");
			File tmpLogFailHigh = new File("tmp_log/improved_wait_fail_high");
			deleteIfExist(tmpLogPass);
			deleteIfExist(tmpLogFailLow);
			deleteIfExist(tmpLogFailHigh);
			generateAndEvaluateProfile(logImprovedLessWaitScript, logImprovedLessWaitProfile, tmpLogPass, 
					tmpLogFailLow, tmpLogFailHigh);
			FileUtils.copyDirectory(tmpLogPass, logImprovedWaitPass);
			FileUtils.copyDirectory(tmpLogFailLow, logImprovedWaitFailLow);
			FileUtils.copyDirectory(tmpLogFailHigh, logImprovedWaitFailHigh);
			
			if(logImprovedWaitFailLow.list().length == 0 && logImprovedWaitFailHigh.list().length == 0){
				break;
			}
			currentIteration++;
		}
	}
	
	private void generateAndEvaluateProfile(File agentDB, File profileLogDir, File passScriptdir, 
			File failLowScriptDir, File failHighScriptDir) throws Exception{
		String[] tiers = new String[]{"Enemy", "Elite", "Miniboss", "Boss"};
		
		//Generate profile and move to temporary directory. Each round will have temp directory named "roundX"
		deleteIfExist(veilAgentDB);
		FileUtils.copyDirectory(agentDB, veilAgentDB);
		for(int i=1; i<=roundCount; i++){
			deleteIfExist(veilProfile);
			execVeil(false);
			if(!veilProfile.exists()){
				veilProfile.mkdirs();
			}
			for(String tier : tiers){
				File f = new File(veilProfile, tier);
				if(!f.exists()) f.mkdirs();
			}
			FileUtils.copyDirectory(veilProfile, new File("round"+i));
		}
		
		//Create actual profile folder where sub-directory are arranged into tier/roundX. Also delete temp directories "roundX" afterward
		deleteIfExist(veilProfile);
		veilProfile.mkdirs();
		for(String tier : tiers){
			File tierSubdir = new File(veilProfile, tier);
			tierSubdir.mkdirs();
			for(int i=1; i<=roundCount; i++){
				FileUtils.copyDirectory(new File("round"+i+"/"+tier), new File(tierSubdir, "round"+i));
			}
		}
		for(int i=1; i<=roundCount; i++){
			deleteIfExist(new File("round"+i));
		}
		
		//Save profile log
		deleteIfExist(profileLogDir);
		FileUtils.copyDirectory(veilProfile, profileLogDir);
		
		//Evaluate profile and save log
		deleteIfExist(veilEvalPass);
		deleteIfExist(veilEvalFailLow);
		deleteIfExist(veilEvalFailHigh);
		execVeil(true);
		deleteIfExist(passScriptdir);
		deleteIfExist(failLowScriptDir);
		deleteIfExist(failHighScriptDir);
		FileUtils.copyDirectory(veilEvalPass, passScriptdir);
		FileUtils.copyDirectory(veilEvalFailLow, failLowScriptDir);
		FileUtils.copyDirectory(veilEvalFailHigh, failHighScriptDir);
	}
	
	private void execVeil(boolean evaluationMode) throws Exception{
		String targetJar = "Veil/"+(evaluationMode ? "veil_evalagent.jar" : "veil_aiV5Rev2Lap.jar");
		ProcessBuilder pb = new ProcessBuilder(new String[]{"java", "-jar", targetJar});
		pb.redirectErrorStream(true);
		Process ps = pb.start();
		//Pitfall: process' stdout can be full and cause the process to hang. We need to empty the stdout
		BufferedInputStream in = new BufferedInputStream(ps.getInputStream());
        byte[] bytes = new byte[4096];
        while (in.read(bytes) != -1) {}
		int exitcode = ps.waitFor();
		//App.exit of Libgdx return -1 as exit code by default
		if(exitcode != -1){
			System.out.println("Jar may not be terminated appropriately " + targetJar+" code:"+exitcode);
		}
	}
	
	private void deleteIfExist(File dir) throws IOException{
		if(dir.exists()){
			if(dir.isDirectory()){
				FileUtils.deleteDirectory(dir);
			}else{
				dir.delete();
			}
		}
	}
	
	private void cleanup() throws Exception{
		deleteIfExist(highSpawnSkel);
		deleteIfExist(improvedStatic);
		deleteIfExist(improvedWait);
		deleteIfExist(presortSkel);
		deleteIfExist(staticSkel);
	}
	
	public void saveCSVReport(File file) throws IOException{
		String csv = ",Enemy,Elite,Miniboss,Boss,Total\n";
		csv += "Pass"+countScriptAsCSVString(logBaseScriptPass,null)+"\n";
		csv += "Not Pass"+countScriptAsCSVString(logBaseScriptFailLow,logBaseScriptFailHigh)+"\n";
		csv += "- Static"+countScriptAsCSVString(logStaticFromLow,logStaticFromHigh)+"\n";
		csv += "- - Improved static Pass"+countScriptAsCSVString(logImprovedStaticPass, null)+"\n";
		csv += "- - Improved static Fail"+countScriptAsCSVString(logImprovedStaticFailLow, logImprovedStaticFailHigh)+"\n";
		int iteration = 1;
		while(true){
			File target = new File(logImprovedStaticWait, iteration+logStrImprovedWaitPass);
			if(!target.exists())
				break;
			csv += "- - - "+iteration+"_improved static wait Pass"+countScriptAsCSVString(target, null)+"\n";
			csv += "- - - "+iteration+"_improved static wait Fail"+countScriptAsCSVString(
					new File(logImprovedStaticWait, iteration+logStrImprovedWaitFailLow),
					new File(logImprovedStaticWait, iteration+logStrImprovedWaitFailHigh)
					)+"\n";
			iteration++;
		}
		csv += "- NonStatic"+countScriptAsCSVString(logNonStaticFromLow,logNonStaticFromHigh)+"\n";
		iteration = 1;
		while(true){
			File target = new File(logImprovedWait, iteration+logStrImprovedWaitPass);
			if(!target.exists())
				break;
			csv += "- - "+iteration+"_improved wait Pass"+countScriptAsCSVString(target, null)+"\n";
			csv += "- - "+iteration+"_improved wait Fail"+countScriptAsCSVString(
					new File(logImprovedWait, iteration+logStrImprovedWaitFailLow),
					new File(logImprovedWait, iteration+logStrImprovedWaitFailHigh)
					)+"\n";
			iteration++;
		}
		FileUtils.writeStringToFile(file, csv, StandardCharsets.US_ASCII);
	}
	
	private String countScriptAsCSVString(File dir, File dir2){
		if(!dir.isDirectory()){
			return "";
		}else{
			int[] result = countScript(dir);
			if(dir2 == null || !dir2.isDirectory()){
				return ","+result[0]+","+result[1]+","+result[2]+","+result[3];
			}else{
				int[] result2 = countScript(dir2);
				String str = "";
				for(int i=0; i<4; i++){
					str += ","+(result[i]+result2[i]);
				}
				return str;
			}
		}
	}
	
	private int[] countScript(File dir){
		if(!dir.isDirectory()){
			return null;
		}else{
			return new int[]{
					(new File(dir, "Enemy")).listFiles().length,
							(new File(dir, "Elite")).listFiles().length,
							(new File(dir, "Miniboss")).listFiles().length,
							(new File(dir, "Boss")).listFiles().length
			};
		}
	}
}
