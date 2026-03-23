package gwendolen.partialplans.checkalg;

import ail.mas.DefaultEnvironment;
import ail.syntax.Action;
import ail.syntax.NumberTermImpl;
import ail.syntax.Predicate;
import ail.syntax.Unifier;
import ail.util.AILexception;

public class MoveEnvROSMimic_radiation extends DefaultEnvironment {

	String prevloc = "l0";
	boolean fm = true;
	String default_rad_status = "low";
	Predicate default_radpercept;
	@Override
	public void init_after_adding_agents()
	{
		if (fm) {
			Predicate oldloc = new Predicate("at");
			oldloc.addTerm(new Predicate(prevloc));
			//addPercept("agent", oldloc);
			Predicate rad_percept = get_radiation_pred(default_rad_status);
			addPercept(rad_percept);
			addPercept(oldloc);
			default_radpercept =rad_percept;
			fm = false;
		}
	}
	Predicate get_radiation_pred(String status)
	{
		Predicate rad_percept = new Predicate("radiation");
		rad_percept.addTerm(new Predicate(status));

		return rad_percept;
	}
	@Override
	public Unifier executeAction(String agName, Action act) throws AILexception {


		if (act.getFunctor().equals("move_to")) {
			clearPercepts();
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
			nearloc.addTerm(new Predicate("l10"));
			// adding the near percept
			// when we get to l4 we realise that we are "near l6"
			if (moveloc.equalsIgnoreCase("l8")) {
				Predicate newradpred= get_radiation_pred("medium");
				addPercept(newradpred);
				addPercept(nearloc);
				System.out.println("Added "+newradpred.toString());

			} else {
				// it might be there already and we might have to remove it
				// putting this in move because that is what decides it
				addPercept(default_radpercept);
				System.out.println("Added "+default_radpercept.toString());
			}
			Predicate mbres = new Predicate("movebase_result");
			mbres.addTerm(new Predicate(moveloc));
			mbres.addTerm(new NumberTermImpl(3));
			addPercept(mbres);


		}
		else if (act.getFunctor().equals("cancel_goal")) {
			clearPercepts();
			Predicate mbres = new Predicate("movebase_result");
			mbres.addTerm(new Predicate(prevloc));
			mbres.addTerm(new NumberTermImpl(2));
			addPercept(mbres);
			addPercept(default_radpercept);
		}
		Unifier u = super.executeAction(agName, act);
		return u;// super.executeAction(agName, act);
	}

}
