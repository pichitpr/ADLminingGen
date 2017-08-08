package adl_2daa.gen.testtool;

import adl_2daa.gen.signature.ActionMainSignature;
import adl_2daa.gen.signature.ActionSignature;
import adl_2daa.gen.signature.Datatype;
import adl_2daa.gen.signature.FunctionMainSignature;
import adl_2daa.gen.signature.GeneratorRegistry;
import adl_2daa.gen.signature.Signature;

public class TestInitializer {

	private static void registerAction(){
		GeneratorRegistry.registerActionSignature("AddExtraVelocityToPlayer",
				new ActionMainSignature(Datatype.DIRECTION,Datatype.DECIMAL,Datatype.BOOL));
		GeneratorRegistry.registerActionSignature("ChangeDirectionToPlayerByStep", 
				new ActionMainSignature(Datatype.DIRECTION_SET,Datatype.INT));
		GeneratorRegistry.registerActionSignature("Despawn",
				new ActionMainSignature());
		GeneratorRegistry.registerActionSignature("FlipDirection",
				new ActionMainSignature(Datatype.DIRECTION));
		/*GeneratorRegistry.registerActionSignature("FloorStun",
				new ActionMainSignature(Datatype.INT));*/
		GeneratorRegistry.registerActionSignature("Goto",
				new ActionMainSignature(Datatype.IDENTIFIER));
		GeneratorRegistry.registerActionSignature("Jump",
				new ActionMainSignature(Datatype.POSITION, Datatype.DECIMAL, Datatype.INT, Datatype.BOOL));
		GeneratorRegistry.registerActionSignature("Notify",
				new ActionMainSignature(Datatype.DYNAMIC, Datatype.INT));
		/*GeneratorRegistry.registerActionSignature("RunCircling", 
				new ActionMainSignature(Datatype.POSITION, Datatype.DECIMAL, Datatype.DECIMAL, Datatype.INT));*/
		GeneratorRegistry.registerActionSignature("RunHarmonic", 
				new ActionMainSignature(Datatype.DIRECTION, Datatype.DECIMAL, Datatype.BOOL));
		GeneratorRegistry.registerActionSignature("RunStraight", 
				new ActionMainSignature(Datatype.DIRECTION, Datatype.DECIMAL, Datatype.BOOL));
		GeneratorRegistry.registerActionSignature("RunTo", 
				new ActionMainSignature(Datatype.POSITION, Datatype.DECIMAL));
		
		ActionMainSignature set = new ActionMainSignature(2, 0, Datatype.CHOICE, Datatype.DYNAMIC, Datatype.INT);
		set.addChoiceSignature("atk", new ActionSignature(Datatype.DYNAMIC, Datatype.INT));
		set.addChoiceSignature("group", new ActionSignature(Datatype.DYNAMIC, Datatype.INT));
		set.addChoiceSignature("direction", new ActionSignature(Datatype.DYNAMIC, Datatype.DIRECTION));
		set.addChoiceSignature("position", new ActionSignature(Datatype.DYNAMIC, Datatype.POSITION));
		set.addChoiceSignature("gravityeff", new ActionSignature(Datatype.DYNAMIC, Datatype.DECIMAL));
		set.addChoiceSignature("collider", new ActionSignature(Datatype.DYNAMIC, Datatype.COLLIDER));
		set.addChoiceSignature("attacker", new ActionSignature(Datatype.DYNAMIC, Datatype.BOOL));
		set.addChoiceSignature("defender", new ActionSignature(Datatype.DYNAMIC, Datatype.BOOL));
		set.addChoiceSignature("invul", new ActionSignature(Datatype.DYNAMIC, Datatype.BOOL));
		set.addChoiceSignature("projectile", new ActionSignature(Datatype.DYNAMIC, Datatype.BOOL));
		set.addChoiceSignature("phasing", new ActionSignature(Datatype.DYNAMIC, Datatype.BOOL));
		set.addChoiceSignature("texture", new ActionSignature(Datatype.DYNAMIC, Datatype.INT));
		set.addChoiceSignature("hp", new ActionSignature(Datatype.DYNAMIC, Datatype.INT));
		GeneratorRegistry.registerActionSignature("Set", set);
		
		GeneratorRegistry.registerActionSignature("Spawn",
				new ActionMainSignature(2, -1, Datatype.IDENTIFIER, Datatype.POSITION, Datatype.DIRECTION));
		//Placeholder for Spawn with specified target
		GeneratorRegistry.registerActionSignature("@Spawn",
				new ActionMainSignature(2, -1, Datatype.IDENTIFIER, Datatype.POSITION, Datatype.DIRECTION));
		
		GeneratorRegistry.registerActionSignature("Var", 
				new ActionMainSignature(Datatype.INT));
		GeneratorRegistry.registerActionSignature("VarDec", 
				new ActionMainSignature(Datatype.INT));
		GeneratorRegistry.registerActionSignature("VarInc", 
				new ActionMainSignature(Datatype.INT));
		GeneratorRegistry.registerActionSignature("VarSet", 
				new ActionMainSignature(Datatype.INT, Datatype.INT));
		GeneratorRegistry.registerActionSignature("Wait", 
				new ActionMainSignature(Datatype.BOOL));
	}
	
	private static void registerFunction(){
		GeneratorRegistry.registerFunctionSignature("Abs",
				new FunctionMainSignature(Datatype.DECIMAL, 
						Datatype.DECIMAL));
		GeneratorRegistry.registerFunctionSignature("Anchor",
				new FunctionMainSignature(Datatype.POSITION, 
						Datatype.POSITION));
		GeneratorRegistry.registerFunctionSignature("Attack",
				new FunctionMainSignature(Datatype.BOOL));
		GeneratorRegistry.registerFunctionSignature("Attacked",
				new FunctionMainSignature(Datatype.BOOL));
		
		FunctionMainSignature btnPress = new FunctionMainSignature(Datatype.BOOL, 1, 0, 
				Datatype.CHOICE);
		btnPress.addChoiceSignature("up", new Signature(Datatype.BOOL));
		btnPress.addChoiceSignature("down", new Signature(Datatype.BOOL));
		btnPress.addChoiceSignature("left", new Signature(Datatype.BOOL));
		btnPress.addChoiceSignature("right", new Signature(Datatype.BOOL));
		btnPress.addChoiceSignature("attack", new Signature(Datatype.BOOL));
		btnPress.addChoiceSignature("jump", new Signature(Datatype.BOOL));
		GeneratorRegistry.registerFunctionSignature("ButtonPress", btnPress);
		
		GeneratorRegistry.registerFunctionSignature("CollideWithDynamic", 
				new FunctionMainSignature(Datatype.BOOL));
		GeneratorRegistry.registerFunctionSignature("Damage",
				new FunctionMainSignature(Datatype.BOOL));
		GeneratorRegistry.registerFunctionSignature("Damaged",
				new FunctionMainSignature(Datatype.BOOL));
		GeneratorRegistry.registerFunctionSignature("DecimalSet", 
				new FunctionMainSignature(Datatype.DECIMAL_SET, 
						Datatype.DECIMAL, Datatype.DECIMAL, Datatype.DECIMAL));
		GeneratorRegistry.registerFunctionSignature("DecimalSetSymmetry",
				new FunctionMainSignature(Datatype.DECIMAL_SET, 
						Datatype.DECIMAL, Datatype.DECIMAL, Datatype.DECIMAL));
		
		FunctionMainSignature dirComp = new FunctionMainSignature(Datatype.DIRECTION, 2, 0,
				Datatype.CHOICE, Datatype.DIRECTION);
		dirComp.addChoiceSignature("X", new Signature(Datatype.DIRECTION, 
				Datatype.DIRECTION));
		dirComp.addChoiceSignature("Y", new Signature(Datatype.DIRECTION, 
				Datatype.DIRECTION));
		GeneratorRegistry.registerFunctionSignature("DirectionComponent", dirComp);
		
		GeneratorRegistry.registerFunctionSignature("DirectionSet", 
				new FunctionMainSignature(Datatype.DIRECTION_SET, 
						Datatype.DIRECTION));
		GeneratorRegistry.registerFunctionSignature("DirectionSetDivide", 
				new FunctionMainSignature(Datatype.DIRECTION_SET, 
						Datatype.INT));
		GeneratorRegistry.registerFunctionSignature("DirectionSetRange", 
				new FunctionMainSignature(Datatype.DIRECTION_SET, 
						Datatype.DIRECTION, Datatype.DIRECTION, Datatype.DECIMAL));
		
		FunctionMainSignature dist = new FunctionMainSignature(Datatype.DECIMAL, 2, 0,
				Datatype.CHOICE, Datatype.POSITION);
		dist.addChoiceSignature("X", new Signature(Datatype.DECIMAL, 
				Datatype.POSITION));
		dist.addChoiceSignature("Y", new Signature(Datatype.DECIMAL, 
				Datatype.POSITION));
		GeneratorRegistry.registerFunctionSignature("DistanceTo", dist);
		
		FunctionMainSignature distPlayer = new FunctionMainSignature(Datatype.DECIMAL, 1, 0,
				Datatype.CHOICE);
		distPlayer.addChoiceSignature("X", new Signature(Datatype.DECIMAL));
		distPlayer.addChoiceSignature("Y", new Signature(Datatype.DECIMAL));
		GeneratorRegistry.registerFunctionSignature("DistanceToPlayer", distPlayer);
		
		GeneratorRegistry.registerFunctionSignature("DynamicCount", 
				new FunctionMainSignature(Datatype.INT, 
						Datatype.IDENTIFIER));
		
		FunctionMainSignature dynFilter = new FunctionMainSignature(Datatype.DYNAMIC, 1, 0,
				Datatype.CHOICE);
		dynFilter.addChoiceSignature("this", new Signature(Datatype.DYNAMIC));
		dynFilter.addChoiceSignature("player", new Signature(Datatype.DYNAMIC));
		GeneratorRegistry.registerFunctionSignature("DynamicFilter", dynFilter);
		
		FunctionMainSignature get = new FunctionMainSignature(Datatype.INT, 2, 0,
				Datatype.CHOICE, Datatype.DYNAMIC);
		get.addChoiceSignature("atk", new Signature(Datatype.INT, Datatype.DYNAMIC));
		get.addChoiceSignature("group", new Signature(Datatype.INT, Datatype.DYNAMIC));
		get.addChoiceSignature("direction", new Signature(Datatype.DIRECTION, Datatype.DYNAMIC));
		get.addChoiceSignature("position", new Signature(Datatype.POSITION, Datatype.DYNAMIC));
		get.addChoiceSignature("gravityeff", new Signature(Datatype.DECIMAL, Datatype.DYNAMIC));
		get.addChoiceSignature("collider", new Signature(Datatype.COLLIDER, Datatype.DYNAMIC));
		get.addChoiceSignature("attacker", new Signature(Datatype.BOOL, Datatype.DYNAMIC));
		get.addChoiceSignature("defender", new Signature(Datatype.BOOL, Datatype.DYNAMIC));
		get.addChoiceSignature("invul", new Signature(Datatype.BOOL, Datatype.DYNAMIC));
		get.addChoiceSignature("projectile", new Signature(Datatype.BOOL, Datatype.DYNAMIC));
		get.addChoiceSignature("phasing", new Signature(Datatype.BOOL, Datatype.DYNAMIC));
		//get.addChoiceSignature("parent", new Signature(Datatype.DYNAMIC, Datatype.DYNAMIC));
		//get.addChoiceSignature("children", new Signature(Datatype.DYNAMIC_SET, Datatype.DYNAMIC));
		get.addChoiceSignature("hp", new Signature(Datatype.INT, Datatype.DYNAMIC));
		GeneratorRegistry.registerFunctionSignature("Get", get);
		
		GeneratorRegistry.registerFunctionSignature("InTheAir", 
				new FunctionMainSignature(Datatype.BOOL));
		GeneratorRegistry.registerFunctionSignature("Notified", 
				new FunctionMainSignature(Datatype.BOOL, 
						Datatype.INT));
		GeneratorRegistry.registerFunctionSignature("Peak", 
				new FunctionMainSignature(Datatype.BOOL));
		GeneratorRegistry.registerFunctionSignature("Perpendicular", 
				new FunctionMainSignature(Datatype.DIRECTION,
						Datatype.DIRECTION));
		
		//Use this for normal generation (comment out below section)
		/*GeneratorRegistry.registerFunctionSignature("Random", 
				new FunctionMainSignature(Datatype.ABSTRACT, Datatype.ABSTRACT_SET));*/
		//Use this for random generation (comment out above section)
		GeneratorRegistry.registerFunctionSignature("_Random_decimal", 
				new FunctionMainSignature(Datatype.DECIMAL, Datatype.DECIMAL_SET));
		GeneratorRegistry.registerFunctionSignature("_Random_direction", 
				new FunctionMainSignature(Datatype.DIRECTION, Datatype.DIRECTION_SET));
		
		GeneratorRegistry.registerFunctionSignature("RandomPositionInRadius", 
				new FunctionMainSignature(Datatype.POSITION, 
						Datatype.POSITION, Datatype.DECIMAL));
		GeneratorRegistry.registerFunctionSignature("RangeCap", 
				new FunctionMainSignature(Datatype.DECIMAL, 
						Datatype.DECIMAL, Datatype.DECIMAL, Datatype.DECIMAL));
		GeneratorRegistry.registerFunctionSignature("RangeCapCircular", 
				new FunctionMainSignature(Datatype.DECIMAL, 
						Datatype.DECIMAL, Datatype.DECIMAL, Datatype.DECIMAL));
		GeneratorRegistry.registerFunctionSignature("Rel", 
				new FunctionMainSignature(Datatype.POSITION, 
						Datatype.POSITION));
		GeneratorRegistry.registerFunctionSignature("RelPlayer", 
				new FunctionMainSignature(Datatype.POSITION, 
						Datatype.POSITION));
		GeneratorRegistry.registerFunctionSignature("SurfaceInDir", 
				new FunctionMainSignature(Datatype.BOOL, 
						Datatype.DIRECTION));
		GeneratorRegistry.registerFunctionSignature("TimePass", 
				new FunctionMainSignature(Datatype.INT));
		GeneratorRegistry.registerFunctionSignature("TravelDistance", 
				new FunctionMainSignature(Datatype.INT));
		GeneratorRegistry.registerFunctionSignature("TurnTo", 
				new FunctionMainSignature(Datatype.DIRECTION,
						Datatype.DIRECTION_SET, Datatype.POSITION));
		GeneratorRegistry.registerFunctionSignature("TurnToPlayer", 
				new FunctionMainSignature(Datatype.DIRECTION,
						Datatype.DIRECTION_SET));
		GeneratorRegistry.registerFunctionSignature("VarGet", 
				new FunctionMainSignature(Datatype.INT, 
						Datatype.INT));
		
		//==============================
		//v4
		
		GeneratorRegistry.registerFunctionSignature("AnchorPlayer",
				new FunctionMainSignature(Datatype.POSITION, 
						Datatype.POSITION));
		GeneratorRegistry.registerFunctionSignature("RelDirection", 
				new FunctionMainSignature(Datatype.DIRECTION, 
						Datatype.DIRECTION));
		GeneratorRegistry.registerFunctionSignature("Rel2Direction", 
				new FunctionMainSignature(Datatype.DIRECTION, 
						Datatype.DIRECTION));
		GeneratorRegistry.registerFunctionSignature("RandomPositionInRange", 
				new FunctionMainSignature(Datatype.POSITION, 
						Datatype.POSITION,Datatype.POSITION));
		
		//==============================
		//v5 (23/2/59) -- Special function converted from expression
		
		GeneratorRegistry.registerFunctionSignature("@U_neg", 
				new FunctionMainSignature(Datatype.DECIMAL, Datatype.DECIMAL));
		GeneratorRegistry.registerFunctionSignature("@U_not", 
				new FunctionMainSignature(Datatype.BOOL, Datatype.BOOL));
		GeneratorRegistry.registerFunctionSignature("@L_and", 
				new FunctionMainSignature(Datatype.BOOL, Datatype.BOOL, Datatype.BOOL));
		GeneratorRegistry.registerFunctionSignature("@L_or", 
				new FunctionMainSignature(Datatype.BOOL, Datatype.BOOL, Datatype.BOOL));
		GeneratorRegistry.registerFunctionSignature("@C_eq", 
				new FunctionMainSignature(Datatype.BOOL, Datatype.ABSTRACT, Datatype.ABSTRACT));
		GeneratorRegistry.registerFunctionSignature("@C_neq", 
				new FunctionMainSignature(Datatype.BOOL, Datatype.ABSTRACT, Datatype.ABSTRACT));
		GeneratorRegistry.registerFunctionSignature("@C_gt", 
				new FunctionMainSignature(Datatype.BOOL, Datatype.DECIMAL, Datatype.DECIMAL));
		GeneratorRegistry.registerFunctionSignature("@C_lt", 
				new FunctionMainSignature(Datatype.BOOL, Datatype.DECIMAL, Datatype.DECIMAL));
		GeneratorRegistry.registerFunctionSignature("@C_ge", 
				new FunctionMainSignature(Datatype.BOOL, Datatype.DECIMAL, Datatype.DECIMAL));
		GeneratorRegistry.registerFunctionSignature("@C_le", 
				new FunctionMainSignature(Datatype.BOOL, Datatype.DECIMAL, Datatype.DECIMAL));
		GeneratorRegistry.registerFunctionSignature("@A_add", 
				new FunctionMainSignature(Datatype.DECIMAL, Datatype.DECIMAL, Datatype.DECIMAL));
		GeneratorRegistry.registerFunctionSignature("@A_sub", 
				new FunctionMainSignature(Datatype.DECIMAL, Datatype.DECIMAL, Datatype.DECIMAL));
		GeneratorRegistry.registerFunctionSignature("@A_mul", 
				new FunctionMainSignature(Datatype.DECIMAL, Datatype.DECIMAL, Datatype.DECIMAL));
		GeneratorRegistry.registerFunctionSignature("@A_div", 
				new FunctionMainSignature(Datatype.DECIMAL, Datatype.DECIMAL, Datatype.DECIMAL));
		GeneratorRegistry.registerFunctionSignature("@A_mod", 
				new FunctionMainSignature(Datatype.DECIMAL, Datatype.DECIMAL, Datatype.DECIMAL));
	}
	
	public static void init(){
		GeneratorRegistry.cleanup();
		registerAction();
		registerFunction();
	}
}
