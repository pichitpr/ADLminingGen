package adl_2daa.gen.generator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import parsemis.extension.GraphPattern;
import spmf.extension.algorithm.seqgen.SequentialPatternGen;
import spmf.extension.prefixspan.JSPatternGen;
import adl_2daa.ast.ASTExpression;
import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.expression.ASTBinary;
import adl_2daa.ast.expression.ASTUnary;
import adl_2daa.ast.expression.Function;
import adl_2daa.ast.expression.IntConstant;
import adl_2daa.ast.expression.StringConstant;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.statement.Condition;
import adl_2daa.ast.statement.Loop;
import adl_2daa.ast.structure.Agent;
import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.ast.structure.State;
import adl_2daa.gen.Miner;
import adl_2daa.gen.profile.AgentProfile;
import adl_2daa.gen.profile.AgentProperties;
import adl_2daa.gen.signature.GeneratorRegistry;

public class Skeleton {

	private String identifier;
	private Root skel;
	
	public Skeleton(){}
	public Skeleton(String identifier, Root skel){
		this.identifier = identifier;
		this.skel = skel;
	}
	
	private Sequence dirtyInitForMain(){
		AgentProperties prop = new AgentProperties();
		prop.texture = 4;
		prop.position = "c(320,400)";
		prop.direction = "west";
		prop.collider = "32,48";
		prop.gravityeff = 1f;
		prop.hp  = 100;
		prop.attacker = true;
		prop.atk = 10;
		prop.defender = true;
		return prop.toInit();
	}
	
	private Sequence dirtyInitForChild(){
		AgentProperties prop = new AgentProperties();
		prop.texture = 6;
		prop.collider = "16,16";
		prop.projectile = true;
		prop.attacker = true;
		prop.atk = 1;
		return prop.toInit();
	}
	
	public Root getRoot(){
		return skel;
	}
	
	/**
	 * Change main agent (Skeleton) identifier. Only call this method after the generation finish. This method assumes that
	 * no agent can spawn the main agent and the main agent must be the first agent in the script. Note that this method does
	 * not change identifier used as a part of child agent prefix.
	 */
	public void dirtyIdentifierChange(String newIdentifier){
		identifier = newIdentifier;
		Agent mainAgent = skel.getRelatedAgents().get(0);
		Agent newMainAgent = new Agent(newIdentifier, mainAgent.getInit(), mainAgent.getDes(), mainAgent.getStates());
		skel.getRelatedAgents().set(0, newMainAgent);
	}
	
	/**
	 * Change main agent hp in .init. Only call this method after the generation finish. This method assumes that agent hp
	 * is set only once in .init using Set()
	 */
	public void dirtyMutateInitialHp(int newHp){
		if(skel.getRelatedAgents().isEmpty()) return;
		Agent mainAgent = skel.getRelatedAgents().get(0);
		if(mainAgent.getInit() == null || mainAgent.getInit().getStatements().size() == 0) return;
		List<ASTStatement> initBlock = mainAgent.getInit().getStatements();
		for(ASTStatement st : initBlock){
			if(st instanceof Action){
				Action action = (Action)st;
				if(action.getName().equals("Set") && ((StringConstant)action.getParams()[0]).getValue().equals("hp")){
					action.getParams()[2] = new IntConstant(""+newHp);
					break;
				}
			}
		}
	}
	
	/**
	 * Generate a full root from provided miner that passed all mining phase. The result is kept in memory.
	 */
	public GenerationProfile fullyGenerate(Miner miner, String identifier){
		GenerationProfile genProfile = new GenerationProfile();
		AgentProfile[] profiles = miner.generateAgentProfile();
		profiles[0].setRootName(identifier);
		genProfile.profile = profiles[0];
		generateInitialSkeleton(profiles);
		List<GraphPattern<String,Integer>> interEntityList = miner.getFrequentInterEntityParallel();
		List<GraphPattern<String,Integer>> parallelList = miner.getFrequentParallel();
		List<JSPatternGen<String>> gotoList = miner.getFrequentInterStateOrder_Goto();
		List<JSPatternGen<String>> desList = miner.getFrequentInterStateOrder_Despawn();
		List<SequentialPatternGen<String>> orderList = miner.getFrequentOrder();
		
		for(int i=1; i<=profiles[0].getParallelInterEntityRelationUsage(); i++){
			GraphPattern<String,Integer> relation = ASTUtility.randomUniform(interEntityList);
			mergeInterEntity(relation, true);
			genProfile.interEntityList.add(relation);
		}
		for(int i=1; i<=profiles[0].getParallelRelationUsage(); i++){
			GraphPattern<String,Integer> relation = ASTUtility.randomUniform(parallelList);
			mergeParallel(relation);
			genProfile.parallelList.add(relation);
		}
		for(int i=1; i<=profiles[0].getInterStateGotoRelationUsage(); i++){
			JSPatternGen<String> relation = ASTUtility.randomUniform(gotoList);
			mergeInterState(false, relation, true);
			genProfile.gotoList.add(relation);
		}
		for(int i=1; i<=profiles[0].getInterStateDespawnRelationUsage(); i++){
			JSPatternGen<String> relation = ASTUtility.randomUniform(desList);
			mergeInterState(true, relation, true);
			genProfile.desList.add(relation);
		}
		for(int i=1; i<=profiles[0].getOrderRelationUsage(); i++){
			SequentialPatternGen<String> relation =ASTUtility.randomUniform(orderList); 
			mergeOrder(relation);
			genProfile.orderList.add(relation);
		}
		mergeNesting(miner.getFrequentNesting());
		
		finalizeSkeleton();
		
		return genProfile;
	}
	
	/**
	 * Generate initial skeleton from profiles, there must be at least 1 profile.
	 * All blocks in the profile (except sequence block) must be non-empty.
	 * The first agent in generated skeleton is considered "Main agent".
	 */
	public void generateInitialSkeleton(AgentProfile[] profiles){
		//TODO: Check main agent flag before generating
		List<Agent> agents = new LinkedList<Agent>();
		identifier = profiles[0].getRootName();
		skel = new Root(agents);
		
		for(int i=0; i<profiles.length; i++){
			//List<ASTStatement> init = new LinkedList<ASTStatement>();
			Sequence init = profiles[i].isMainAgent() ? dirtyInitForMain() : dirtyInitForChild();
			List<ASTStatement> des = new LinkedList<ASTStatement>();
			List<State> states = new LinkedList<State>();
			Agent agent = new Agent(profiles[i].isMainAgent() ? identifier : identifier+"_sub"+i, 
					init, new Sequence("des", des), states);
			
			assert(profiles[i].getStructureInfo().length > 0);
			for(int j=0; j<profiles[i].getStructureInfo().length; j++){
				List<Sequence> sequences = new LinkedList<Sequence>();
				State state = new State("state"+j, sequences);
				int seqCount = profiles[i].getStructureInfo()[j];
			
				assert(seqCount > 0);
				for(int k=0; k<seqCount; k++){
					sequences.add(new Sequence("seq"+k, new LinkedList<ASTStatement>()));
				}
				
				states.add(state);
			}
			
			agents.add(agent);
		}
	}
	
	public void mergeOrder(SequentialPatternGen<String> relation){
		OrderMerger.instance.merge(skel, relation);
	}
	
	public void mergeInterState(boolean des, JSPatternGen<String> relation, boolean useTag){
		InterStateOrderMerger.instance.merge(skel, des, relation, useTag);
	}
	
	public void mergeParallel(GraphPattern<String,Integer> relation){
		ParallelMerger.instance.merge(skel, relation);
	}
	
	public void mergeInterEntity(GraphPattern<String,Integer> relation, boolean useTag){
		InterEntityParallelMerger.instance.merge(skel, relation);
	}
	
	public void mergeNesting(List<GraphPattern<Integer,Integer>> relation){
		NestingMerger.instance.merge(skel, relation);
	}
	
	public void finalizeSkeleton(){
		//Trim block
		for(int i=skel.getRelatedAgents().size()-1; i>=0; i--){
			Agent agent = skel.getRelatedAgents().get(i);
			List<ASTStatement> seq;
			if(agent.getDes() != null){
				String dummyActionName = GeneratorRegistry.getActionName(
						GeneratorRegistry.getDummyActionSignature().getMainSignature().getId()
						);
				seq = agent.getDes().getStatements();
				for(int j=seq.size()-1; j>=0; j--){
					if(seq.get(j) instanceof Action){
						String actionName = ((Action)seq.get(j)).getName();
						//NOTE:: temporary solution to issue #1 written on Github
						if(actionName.equals("Goto") || actionName.equals("Despawn") || actionName.equals(dummyActionName))
							seq.remove(j);
					}
				}
			}
			for(int j=agent.getStates().size()-1; j>=0; j--){
				State state = agent.getStates().get(j);
				for(int k=state.getSequences().size()-1; k>=0; k--){
					if(state.getSequences().get(k).getStatements().size() == 0){
						state.getSequences().remove(k);
					}
				}
				if(state.getSequences().size() == 0){
					agent.getStates().remove(j);
				}
				
			}
			//Fill empty state back if there is no state left
			if(agent.getStates().size() == 0){
				agent.getStates().add(ASTUtility.createEmptyState("seq0"));
			}
		}
		
		//Analyze reach and modify key action target.
		IdentifierFiller.instance.fillMissingIdentifier(skel);
		
		//Fill init for remaining agents
		for(Agent agent : skel.getRelatedAgents()){
			if(agent.getInit() != null && agent.getInit().getStatements().size() == 0){
				List<ASTStatement> initBlock = dirtyInitForChild().getStatements();
				for(ASTStatement st : initBlock){
					agent.getInit().getStatements().add(st);
				}
			}
		}
		
		//System.out.println( (new ReachProfile(skel)).profileToString(skel) );
	}
	
	public void saveAsScript(File dir) throws IOException{
		StringBuilder strb = new StringBuilder();
		skel.toScript(strb, 0);
		FileUtils.writeStringToFile(new File(dir, identifier+".txt"), strb.toString(), StandardCharsets.US_ASCII);
	}
	
	public boolean isEmptySkeleton(){
		if(skel.getRelatedAgents().size() == 0){
			return true;
		}
		Agent mainAgent = skel.getRelatedAgents().get(0);
		if(mainAgent.getStates().size() == 0){
			return true;
		}
		//Non empty .des exists, do not care about state
		if(mainAgent.getDes() != null && mainAgent.getDes().getStatements().size() > 0){
			return false;
		}
		//Otherwise, check for non-empty sequence in initial state
		for(State st : mainAgent.getStates()){
			for(Sequence seq : st.getSequences()){
				if(seq.getStatements().size() > 0){
					return false;
				}
			}
		}
		return true;
	}
	
	public class GenerationProfile{
		public AgentProfile profile;
		public List<GraphPattern<String,Integer>> interEntityList = new LinkedList<GraphPattern<String,Integer>>();
		public List<GraphPattern<String,Integer>> parallelList = new LinkedList<GraphPattern<String,Integer>>();
		public List<JSPatternGen<String>> gotoList = new LinkedList<JSPatternGen<String>>();
		public List<JSPatternGen<String>> desList = new LinkedList<JSPatternGen<String>>();
		public List<SequentialPatternGen<String>> orderList = new LinkedList<SequentialPatternGen<String>>();
		
		public void print(){
			StringBuilder strb = new StringBuilder(profile.toString());
			strb.append("\n\n");
			for(GraphPattern<String,Integer> relation : interEntityList){
				InterEntityParallelMerger.instance.decodeAndDumpRelation(relation, strb);
			}
			strb.append("\n\n");
			for(GraphPattern<String,Integer> relation : parallelList){
				ParallelMerger.instance.decodeAndDumpRelation(relation, strb);
			}
			strb.append("\n\n");
			for(JSPatternGen<String> relation : gotoList){
				InterStateOrderMerger.instance.decodeAndDumpRelation(false, relation, strb);
			}
			strb.append("\n\n");
			for(JSPatternGen<String> relation : desList){
				InterStateOrderMerger.instance.decodeAndDumpRelation(true, relation, strb);
			}
			strb.append("\n\n");
			for(SequentialPatternGen<String> relation : orderList){
				OrderMerger.instance.decodeAndDumpRelation(relation, strb);
			}
			strb.append("\n");
			System.out.println(strb.toString());
		}
	}
	
	//===================================
	// Non-general operation
	//===================================
	
	public void reduceWait(){
		for(Agent agent : skel.getRelatedAgents()){
			if(agent.getInit() != null){
				reduceWait(agent.getInit().getStatements());
			}
			if(agent.getDes() != null){
				reduceWait(agent.getDes().getStatements());
			}
			for(State st : agent.getStates()){
				for(Sequence seq : st.getSequences()){
					reduceWait(seq.getStatements());
				}
			}
		}
	}
	
	public void removePhasing(){
		Agent mainAgent = skel.getRelatedAgents().get(0);
		if(mainAgent.getInit() != null){
			removePhasing(mainAgent.getInit().getStatements());
		}
		if(mainAgent.getDes() != null){
			removePhasing(mainAgent.getDes().getStatements());
		}
		for(State st : mainAgent.getStates()){
			for(Sequence seq : st.getSequences()){
				removePhasing(seq.getStatements());
			}
		}
	}
	
	private class SimilarStatementChunk{
		private Action baseStatement;
		private int startIndex, endIndex;
		
		public SimilarStatementChunk(Action baseStatement, int start){
			this.baseStatement = baseStatement;
			startIndex = endIndex = start;
		}
		
		public boolean tryAppend(Action st){
			if(isSimilarWait(st, baseStatement)){
				endIndex++;
				return true;
			}else{
				return false;
			}
		}
		
		public void reduce(List<ASTStatement> targetList){
			ASTStatement pickedStatement = targetList.get(ASTUtility.randomRange(startIndex, endIndex));
			for(int i=1; i<=endIndex-startIndex+1; i++){
				targetList.remove(startIndex);
			}
			targetList.add(startIndex, pickedStatement);
		}
		
		private boolean isSimilarWait(Action st1, Action st2){
			if(!st1.getName().equals(st2.getName()) || !st1.getName().equalsIgnoreCase("Wait")){
				return false;
			}
			String firstFunc1 = getFirstFunctionName(st1.getParams()[0]);
			String firstFunc2 = getFirstFunctionName(st2.getParams()[0]);
			if(firstFunc1 == null && firstFunc2 == null)
				return true;
			return firstFunc1 != null && firstFunc2 != null && firstFunc1.equals(firstFunc2);
		}
		
		private String getFirstFunctionName(ASTExpression astExp){
			if(astExp instanceof ASTUnary){
				return getFirstFunctionName(((ASTUnary)astExp).node);
			}else if(astExp instanceof ASTBinary){
				ASTBinary bin = (ASTBinary)astExp;
				String funcName = getFirstFunctionName(bin.left);
				return funcName != null ? funcName : getFirstFunctionName(bin.right);
			}else if(astExp instanceof Function){
				return ((Function)astExp).getName();
			}else{
				return null;
			}
		}
	}
	
	private void reduceWait(List<ASTStatement> seqList){
		List<SimilarStatementChunk> reduceableStatement = new ArrayList<SimilarStatementChunk>();
		int index = 0;
		for(ASTStatement st : seqList){
			if(st instanceof Action){
				Action action = (Action)st;
				if(reduceableStatement.size() == 0 || !reduceableStatement.get(reduceableStatement.size()-1).tryAppend(action)){
					reduceableStatement.add(new SimilarStatementChunk(action, index));
				}
			}else if(st instanceof Condition){
				Condition cond = (Condition)st;
				reduceWait(((Condition) st).getIfblock());
				if(cond.getElseblock() != null){
					reduceWait(cond.getElseblock());
				}
			}else if(st instanceof Loop){
				reduceWait(((Loop)st).getContent());
			}
			index++;
		}
		Collections.reverse(reduceableStatement);
		for(SimilarStatementChunk chunk : reduceableStatement){
			chunk.reduce(seqList);
		}
	}
	
	private void removePhasing(List<ASTStatement> seqList){
		List<Integer> flaggedForRemoval = new ArrayList<Integer>();
		int index = 0;
		for(ASTStatement st : seqList){
			if(st instanceof Action){
				Action action = (Action)st;
				if(isSetPhasing(action)){
					flaggedForRemoval.add(index);
				}
			}else if(st instanceof Condition){
				Condition cond = (Condition)st;
				removePhasing(((Condition) st).getIfblock());
				if(cond.getElseblock() != null){
					removePhasing(cond.getElseblock());
				}
			}else if(st instanceof Loop){
				removePhasing(((Loop)st).getContent());
			}
			index++;
		}
		Collections.reverse(flaggedForRemoval);
		for(int i : flaggedForRemoval){
			seqList.remove(i);
		}
	}
	
	private boolean isSetPhasing(Action action){
		if(action.getName().equals("Set")){
			return ((StringConstant)action.getParams()[0]).getValue().equals("phasing");
		}
		return false;
	}
}
