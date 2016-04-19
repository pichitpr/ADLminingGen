package adl_2daa.gen.profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import adl_2daa.ast.ASTExpression;
import adl_2daa.ast.ASTStatement;
import adl_2daa.ast.expression.BooleanConstant;
import adl_2daa.ast.expression.FloatConstant;
import adl_2daa.ast.expression.Function;
import adl_2daa.ast.expression.IntConstant;
import adl_2daa.ast.expression.StringConstant;
import adl_2daa.ast.statement.Action;
import adl_2daa.ast.structure.Sequence;

public class AgentProperties {
	public Boolean attacker=null,defender=null,invul=null,projectile=null,phasing=null;
	public Integer texture=null,atk=null,group=null,hp=null;
	public Float gravityeff=null;
	public String direction=null,position=null,collider=null;
	public HashMap<Integer,Integer> varset = new HashMap<Integer, Integer>();
	
	public AgentProperties(){}
	
	public AgentProperties(Sequence initBlock){
		parse(initBlock);
	}
	
	public void parse(Sequence seq){
		for(ASTStatement st : seq.getStatements()){
			if(st instanceof Action){
				parse((Action)st);
			}
		}
	}
	
	public Sequence toInit(){
		return new Sequence("init", toInitBlock());
	}
	
	private void parse(Action action){
		//TODO: Must be changed to support function
		/*
		ASTExpression[] p = action.getParams();
		if(action.getName().equals("Set")){
			String choice = ((StringConstant)p[0]).getValue();
			switch(choice){
			case "attacker":
				attacker = ((BooleanConstant)p[2]).isValue();
				break;
			case "defender":
				defender = ((BooleanConstant)p[2]).isValue();
				break;
			case "invul":
				invul = ((BooleanConstant)p[2]).isValue();
				break;
			case "projectile":
				projectile = ((BooleanConstant)p[2]).isValue();
				break;
			case "phasing":
				phasing = ((BooleanConstant)p[2]).isValue();
				break;
			case "texture":
				texture = ((IntConstant)p[2]).getValue();
				break;
			case "atk":
				atk = ((IntConstant)p[2]).getValue();
				break;
			case "group":
				group = ((IntConstant)p[2]).getValue();
				break;
			case "hp":
				hp = ((IntConstant)p[2]).getValue();
				break;
			case "gravityeff":
				gravityeff = ((FloatConstant)p[2]).getValue();
				break;
			case "direction":
				direction = ((StringConstant)p[2]).getValue();
				break;
			case "position":
				position = ((StringConstant)p[2]).getValue();
				break;
			case "collider":
				collider = ((StringConstant)p[2]).getValue();
				break;
			}
		}else if(action.getName().equals("VarSet")){
			varset.put(((IntConstant)p[0]).getValue(), ((IntConstant)p[1]).getValue());
		}
		*/
	}
	
	private List<ASTStatement> toInitBlock(){
		List<ASTStatement> st = new ArrayList<ASTStatement>();
		if(attacker != null){
			st.add(createSet("attacker", false, new BooleanConstant(""+attacker.booleanValue())));
		}
		if(defender != null){
			st.add(createSet("defender", false, new BooleanConstant(""+defender.booleanValue())));
		}
		if(invul != null){
			st.add(createSet("invul", false, new BooleanConstant(""+invul.booleanValue())));
		}
		if(projectile != null){
			st.add(createSet("projectile", false, new BooleanConstant(""+projectile.booleanValue())));
		}
		if(phasing != null){
			st.add(createSet("phasing", false, new BooleanConstant(""+phasing.booleanValue())));
		}
		if(texture != null){
			st.add(createSet("texture", false, new IntConstant(""+texture.intValue())));
		}
		if(group != null){
			st.add(createSet("group", false, new IntConstant(""+group.intValue())));
		}
		if(atk != null){
			st.add(createSet("atk", false, new IntConstant(""+atk.intValue())));
		}
		if(hp != null){
			st.add(createSet("hp", false, new IntConstant(""+hp.intValue())));
		}
		if(gravityeff != null){
			st.add(createSet("gravityeff", false, new FloatConstant(""+gravityeff.floatValue())));
		}
		if(direction != null){
			st.add(createSet("direction", false, new StringConstant(direction)));
		}
		if(position != null){
			st.add(createSet("position", false, new StringConstant(position)));
		}
		if(collider != null){
			st.add(createSet("collider", false, new StringConstant(collider)));
		}
		for(Entry<Integer,Integer> entry : varset.entrySet()){
			st.add(createVarSet(entry.getKey(), entry.getValue()));
		}
		return st;
	}
	
	private ASTStatement createSet(String choice, boolean forPlayer, ASTExpression value){
		List<ASTExpression> params = new ArrayList<ASTExpression>();
		params.add(new StringConstant(choice));
		params.add(createDynamicFilter(forPlayer));
		params.add(value);
		return new Action("Set", params);
	}
	
	private ASTExpression createDynamicFilter(boolean forPlayer){
		List<ASTExpression> param = new ArrayList<ASTExpression>();
		param.add(new StringConstant(forPlayer ? "player" : "this"));
		return new Function("DynamicFilter", param, false);
	}
	
	private ASTStatement createVarSet(int i,int value){
		List<ASTExpression> params = new ArrayList<ASTExpression>();
		params.add(new IntConstant(""+i));
		params.add(new IntConstant(""+value));
		return new Action("VarSet", params);
	}
}
