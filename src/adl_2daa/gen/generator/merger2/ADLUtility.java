package adl_2daa.gen.generator.merger2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import lcs.LCSSequence;
import lcs.LCSSequenceEmbedding;
import lcs.SimpleLCSEmbedding;

import org.jacop.constraints.XlteqY;
import org.jacop.core.IntVar;
import org.jacop.core.Store;

import adl_2daa.gen.encoder.ADLAgent;
import adl_2daa.gen.encoder.ADLRoot;
import adl_2daa.gen.encoder.ADLSequence;
import adl_2daa.gen.encoder.ADLSequenceDecoder;
import adl_2daa.gen.encoder.ADLState;
import adl_2daa.gen.generator.JaCopUtility;

public class ADLUtility {

	private static Random random = new Random(1000);
	
	/**
	 * Return [start, end]
	 */
	public static int randomRange(int start, int end){
		return start+random.nextInt(end-start+1);
	}
	
	/**
	 * Precondition: root with at least 1 agent
	 */
	public static ADLAgent randomUniformAgent(ADLRoot root){
		return randomUniform(root.getAgents());
	}
	
	/**
	 * Precondition: agent with at least 1 state
	 */
	public static ADLState randomUniformState(ADLAgent agent){
		return randomUniform(agent.getStates());
	}
	
	/**
	 * Precondition: state with at least 1 sequence
	 */
	public static ADLSequence randomUniformSequence(ADLState state){
		return randomUniform(state.getSequences());
	}
	
	/**
	 * Precondition: non-empty list
	 */
	public static <T> T randomUniform(List<T> list){
		return list.get(randomRange(0, list.size()-1));
	}
	
	public static boolean isEOBTransition(String eAct){
		String actionName = ADLSequenceDecoder.decodeActionOnly(eAct).getName();
		return eAct.length() == 1 && (actionName.equals("Goto") || actionName.equals("Despawn"));
	}
	
	/**
	 * Merge the encodedSequence into the sequence determined by selection.
	 * The encodedSequence MUST contain no more than 1 EOB transition
	 * If there is EOB transition: the selected sequence MUST contain NO EOB transition.
	 */
	public static void merge(ADLSequence sequence, List<String> encodedSequence,
			String eobTransition){
		//Randomly allocate slot for repeating key action (Goto/Despawn/Spawn)
		//under the same nesting condition with highest coverage
		KeyActionSequence wrappedRelation = new KeyActionSequence(encodedSequence);
		KeyActionSequence wrappedSkel = new KeyActionSequence(sequence.getEncodedSequence());
		Set<LCSSequenceEmbedding<String>> lcsResult = 
				SimpleLCSEmbedding.allLCSEmbeddings(wrappedRelation, wrappedSkel, null);
		LCSSequenceEmbedding<String> relAllocation = null;
		int i = randomRange(0, lcsResult.size()-1);
		for(LCSSequenceEmbedding<String> emb : lcsResult){
			if(i == 0){
				relAllocation = emb;
				break;
			}
			i--;
		}
		
		Store store = new Store();
		IntVar[] vars = new IntVar[encodedSequence.size()];
		
		//Setup domain for allocated actions first
		for(i=0; i<relAllocation.size(); i++){
			LCSSequenceEmbedding<String>.EmbeddingItem allocation = relAllocation.itemAt(i);
			int relIndex = wrappedRelation.embToActualIdx(allocation.getI());
			int skelIndex = wrappedSkel.embToActualIdx(allocation.getJ());
			skelIndex = skelIndex*2+1;
			vars[relIndex] = new IntVar(store, skelIndex, skelIndex);
		}
		
		//Domain for the remaining actions
		for(i=0; i<encodedSequence.size(); i++){
			if(vars[i] != null) continue; //Skip allocated action
			vars[i] = new IntVar(store);
			for(int dom=0; dom<=sequence.getEncodedSequence().size(); dom++){
				vars[i].addDom(dom*2, dom*2);
			}
		}
		
		//Ordering constraint
		for(i=1; i<encodedSequence.size(); i++){
			store.impose(new XlteqY(vars[i-1], vars[i]));
		}
		
		//Solve and assign
		int[] result = JaCopUtility.randomUniformAssignment(store, vars);
		TreeMap<Integer, List<Integer>> insertionMap = new TreeMap<Integer, List<Integer>>();
		for(i=0; i<encodedSequence.size(); i++){
			if(result[i] % 2 == 1) continue; //Odd index indicating part of skel
			List<Integer> actionIndexList = insertionMap.get(result[i]);
			if(actionIndexList == null){
				actionIndexList = new ArrayList<Integer>();
				insertionMap.put(result[i]/2, actionIndexList);
			}
			actionIndexList.add(i);
		}
		List<Integer> reversedInsertionIndexList = new LinkedList<Integer>(insertionMap.keySet());
		Collections.reverse(reversedInsertionIndexList);
		//Adding in reverse order to prevent local index shifting
		for(int insertionIndex : reversedInsertionIndexList){
			List<Integer> actionIndexList = insertionMap.get(insertionIndex);
			Collections.reverse(actionIndexList);
			for(int actionIndex : actionIndexList){
				sequence.getEncodedSequence().add(insertionIndex, 
						encodedSequence.get(actionIndex));
			}
		}
		
		//Add EOB transition
		if(eobTransition != null){
			sequence.getEncodedSequence().add(eobTransition);
		}
	}
	
	private static class KeyActionSequence implements LCSSequence<String> {

		private List<String> seq;
		private List<Integer> keyActionIndices;
		
		private KeyActionSequence(List<String> seq){
			this.seq = seq;
			this.keyActionIndices = new ArrayList<Integer>();
			int index = 0;
			for(String eAct : seq){
				String actionName = ADLSequenceDecoder.decodeActionOnly(eAct).getName();
				if(actionName.equals("Goto") || actionName.equals("Despawn") || 
						actionName.equals("Spawn")){
					keyActionIndices.add(index);
				}
				index++;
			}
		}
		
		@Override
		public String itemAt(int embIndex) {
			return seq.get(keyActionIndices.get(embIndex));
		}

		@Override
		public int size() {
			return keyActionIndices.size();
		}
		
		public int embToActualIdx(int embIndex){
			return keyActionIndices.get(embIndex);
		}
	}
}
