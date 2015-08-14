package adl_2daa.gen.miner;

import adl_2daa.ast.structure.Root;
import adl_2daa.ast.structure.Sequence;
import adl_2daa.gen.testtool.TestInitializer;
import adl_2daa.gen.testtool.TestUtility;
import adl_2daa.tool.Parser;

public class Sandbox {

	public static void main(String[] args) throws Exception{
		TestInitializer.init();
		
		String script = TestUtility.readFileAsString(
				"G:\\libgdx\\Script\\Megaman 6\\TomahawkMan.txt"
				);
		Parser parser = new Parser();
		Root root = parser.parse(script);
		Sequence seq = root.getRelatedAgents().get(0).getStates().get(0).getSequences().get(0);
		EncodedSequence eSeq = ADLSequenceEncoder.instance.parseAsEncodedSequence(seq, false);
		for(EncodedAction eAct : eSeq.eActList){
			System.out.println(TestUtility.actionToString(eAct.actionID, 
					eAct.nestingConditions, false));
			System.out.println(TestUtility.actionToString(eAct.actionID, 
					eAct.nestingConditions, true));
		}
	}
}
