package gwendolen.partialplans.checkalg;

import ail.mas.DefaultEnvironment;
import ail.syntax.Action;
import ail.syntax.Predicate;
import ail.syntax.Unifier;
import ail.util.AILexception;

public class MoveEnvWithNear_l6 extends DefaultEnvironment {

	String prevloc = "l0";
	boolean fm = true;

	@Override
	public void init_after_adding_agents()
	{
		if (fm) {
			Predicate oldloc = new Predicate("at");
			oldloc.addTerm(new Predicate(prevloc));
			//addPercept("agent", oldloc);
			addPercept(oldloc);
			fm = false;
		}
	}
	@Override
	public Unifier executeAction(String agName, Action act) throws AILexception {
	
		if (act.getFunctor().equals("move_to")) {
			String moveloc = act.getTerm(0).toString();
			Predicate newloc = new Predicate("at");
			newloc.addTerm(new Predicate(moveloc));

			Predicate oldloc = new Predicate("at");
			oldloc.addTerm(new Predicate(prevloc));

			removePercept(oldloc);

			addPercept( newloc);
			System.out.println("Removed at " + oldloc.toString());
			System.out.println("Added at " + newloc.toString());
			prevloc = moveloc;

			Predicate nearloc = new Predicate("near");
			nearloc.addTerm(new Predicate("l6"));
			// adding the near percept
			// when we get to l4 we realise that we are "near l6"
			if (moveloc.equalsIgnoreCase("l4")) {
				addPercept( nearloc);

			} else {
				// it might be there already and we might have to remove it
				// putting this in move because that is what decides it
				removePercept( nearloc);
			}

		}
		Unifier u = super.executeAction(agName, act);
		return u;// super.executeAction(agName, act);
	}

}
