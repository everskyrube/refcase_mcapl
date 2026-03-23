package gwendolen.partialplans.devel.tests;

import ail.mas.DefaultEnvironment;
import ail.syntax.Action;
import ail.syntax.Predicate;
import ail.syntax.Unifier;
import ail.util.AILexception;

public class MoveEnv extends DefaultEnvironment {

	String curloc="l1";
	@Override
	public Unifier executeAction(String agName, Action act) throws AILexception {
		if(act.getFunctor().equals("move_to"))
		{
			String newlocs = act.getTerm(0).toString();
			Predicate newloc = new Predicate("at");
			newloc.addTerm(new Predicate(newlocs));
			
			Predicate oldloc = new Predicate("at"); 
			oldloc.addTerm(new Predicate(curloc));
			
			removePercept(agName,oldloc); 
			addPercept(agName,newloc);
			curloc = newlocs;
			
		}
		Unifier u =super.executeAction(agName, act);
		return u;//super.executeAction(agName, act);
	}
	

}
