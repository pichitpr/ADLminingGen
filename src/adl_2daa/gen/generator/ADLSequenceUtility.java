package adl_2daa.gen.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import lcs.LCSSequence;
import lcs.LCSSequenceEmbedding;
import lcs.SimpleLCSEmbedding;

import org.jacop.constraints.XlteqY;
import org.jacop.core.IntVar;
import org.jacop.core.Store;

import adl_2daa.ast.structure.Sequence;
import adl_2daa.gen.encoder.ADLSequence;
import adl_2daa.gen.encoder.ADLSequenceDecoder;
import adl_2daa.gen.encoder.ADLSequenceEncoder;

public class ADLSequenceUtility {

	public static boolean isEOBTransition(String eAct){
		/*
		List<String> eActList = new LinkedList<String>();
		eActList.add(eAct);
		ASTStatement st = ADLSequenceDecoder.instance.decode(eActList).get(0);
		if(!(st instanceof Action)) return false;
		Action action = (Action)st;
		return (action.getName().equals("Goto") || action.getName().equals("Despawn"));
		*/
		String actionName = ADLSequenceDecoder.decodeActionOnly(eAct).getName();
		return eAct.length() == 1 && (actionName.equals("Goto") || actionName.equals("Despawn"));
	}
	
	public static void merge(ADLSequence sequence, List<String> encodedSequence,
			String eobTransition){
		/*
		String eobTransition = null;
		for(String eAct : encodedSequence){
			if(isEOBTransition(eAct)){
				eobTransition = eAct;
				break;
			}
		}
		if(eobTransition != null) encodedSequence.remove(eobTransition);
		*/
		
		/*
		//A map eAct -> [slotIndex, slotIndex, slotIndex,...]
		HashMap<String, List<Integer>> existingActionSlot = new HashMap<String, List<Integer>>();
		//A map eAct -> [encodedSequenceIndex,...]
		HashMap<String, List<Integer>> repeatedActionIndex = new HashMap<String, List<Integer>>();
		
		//Checking for appearance of each Goto/Despawn/Spawn in given sequence
		int i=0, slot;
		for(String eAct : encodedSequence){
			String actionName = ADLSequenceDecoder.decodeActionOnly(eAct).getName();
			if(actionName.equals("Goto") || actionName.equals("Despawn") || 
					actionName.equals("Spawn")){
				List<Integer> slotList, actionIndexList;
				if(existingActionSlot.containsKey(eAct)){
					slotList = existingActionSlot.get(eAct);
					actionIndexList = repeatedActionIndex.get(eAct);
				}else{
					slotList = new ArrayList<Integer>();
					existingActionSlot.put(eAct, slotList);
					actionIndexList = new ArrayList<Integer>();
					repeatedActionIndex.put(eAct, actionIndexList);
				}
				actionIndexList.add(i);
				slot = 0;
				for(String skelEAct : sequence.getEncodedSequence()){
					if(eAct.equals(skelEAct)){
						//Same action under the same condition
						slotList.add(slot*2+1);
					}
					slot++;
				}
			}
			i++;
		}
		
		Store store = new Store();
		*/
		
		//Randomly allocate slot for repeating key action (Goto/Despawn/Spawn)
		//under the same nesting condition with highest coverage as possible
		KeyActionSequence wrappedRelation = new KeyActionSequence(encodedSequence);
		KeyActionSequence wrappedSkel = new KeyActionSequence(sequence.getEncodedSequence());
		Set<LCSSequenceEmbedding<String>> lcsResult = 
				SimpleLCSEmbedding.allLCSEmbeddings(wrappedRelation, wrappedSkel, null);
		LCSSequenceEmbedding<String> relAllocation = null;
		int i = ASTUtility.randomRange(0, lcsResult.size()-1);
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
