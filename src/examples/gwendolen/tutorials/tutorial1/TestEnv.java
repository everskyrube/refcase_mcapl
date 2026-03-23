package gwendolen.tutorials.tutorial1;

import ail.mas.DefaultEnvironment;
import ail.syntax.Action;
import ail.syntax.Term;
import ail.syntax.Literal;
import ail.syntax.NumberTermImpl;
import ail.syntax.Unifier;
import ail.util.AILexception;

public class TestEnv extends DefaultEnvironment {

	@Override
	public Unifier executeAction(String agName, Action act) throws AILexception {
		if(act.getFunctor().equals("move"))
		{
			Term loc = act.getTerm(1);
			Literal thing = new Literal("thing"); 
			thing.addTerm(new NumberTermImpl("123"));
			thing.addTerm(new NumberTermImpl("1"));
			addPercept(thing);
		}
		return super.executeAction(agName, act);
	}

}
