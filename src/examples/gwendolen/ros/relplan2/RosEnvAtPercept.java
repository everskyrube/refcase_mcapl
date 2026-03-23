package gwendolen.ros.relplan2;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import java.lang.Math;

import com.fasterxml.jackson.databind.JsonNode;

import ail.mas.DefaultEnvironment;
import ail.semantics.AILAgent;
import ail.syntax.Action;
import ail.syntax.BeliefBase;
import ail.syntax.Literal;
import ail.syntax.NumberTerm;
import ail.syntax.NumberTermImpl;
import ail.syntax.Predicate;
import ail.syntax.Term;
import ail.syntax.Unifier;
import ail.util.AILexception;
import ajpf.util.AJPFLogger;
import ros.Publisher;
import ros.RosBridge;
import ros.RosListenDelegate;
import ros.SubscriptionRequestMsg;
import ros.msgs.geometry_msgs.Twist;
import ros.msgs.geometry_msgs.Vector3;
import ros.msgs.move_base_msgs.MoveBaseActionResult;
import ros.msgs.remote_inspection_msgs.Radiation;
import ros.msgs.std_msgs.PrimitiveMsg;

import ros.tools.MessageUnpacker;
import ros.tools.PeriodicPublisher;

public class RosEnvAtPercept extends DefaultEnvironment {
	static final String logname = "gwendolen.ros.rv_test.RosEnvAtPercept";

	RosBridge bridge = new RosBridge();

	float radiation = 0;
	double at_epsilon_error = 0.5;
	double near_error = 3;


	boolean started_moving = false; 
	HashMap<String, AbstractMap.SimpleEntry<Double, Double>> location_coordinates;
	HashMap<String, Predicate> at_location_predicates;
	HashMap<String, Predicate> near_location_predicates;
	ArrayList<Predicate> currently_near;
	Predicate currently_at;
	String currently_at_string;
	String dup_agentName;

	public RosEnvAtPercept() {
		super();

		bridge.connect("ws://localhost:9090", true);
		System.out.println("Environment started, connection with ROS established.");

		bridge.subscribe(
				SubscriptionRequestMsg.generate("/move_base/result").setType("move_base_msgs/MoveBaseActionResult"),
//				.setThrottleRate(1)
//				.setQueueLength(1),
				new RosListenDelegate() {
					public void receive(JsonNode data, String stringRep) {
						MessageUnpacker<MoveBaseActionResult> unpacker = new MessageUnpacker<MoveBaseActionResult>(
								MoveBaseActionResult.class);
						MoveBaseActionResult msg = unpacker.unpackRosMessage(data);
						clearPercepts();
//					System.out.println("Frame id: "+msg.header.frame_id);
//					System.out.println("Stamp sec: "+msg.header.stamp.secs);
//					System.out.println("Seq: "+msg.header.seq);
//					System.out.println("Goal: "+msg.status.goal_id.id);
//					System.out.println("Stamp sec: "+msg.status.goal_id.stamp.secs);
//					System.out.println("Status: "+msg.status.status);
//					System.out.println("Text: "+msg.status.text);
//					
//					System.out.println();
						Literal movebase_result = new Literal("movebase_result");
						movebase_result.addTerm(new NumberTermImpl(msg.header.seq));
						movebase_result.addTerm(new NumberTermImpl(msg.status.status));
						addPercept(movebase_result);
					}
				});

		bridge.subscribe(SubscriptionRequestMsg.generate("/radiation_sensor_plugin/sensor_0")
				.setType("gazebo_radiation_plugins/Simulated_Radiation_Msg"),
//				.setThrottleRate(1)
//				.setQueueLength(1),
				new RosListenDelegate() {
					public void receive(JsonNode data, String stringRep) {
						MessageUnpacker<Radiation> unpacker = new MessageUnpacker<Radiation>(Radiation.class);
						Radiation msg = unpacker.unpackRosMessage(data);
						 float old_rad = radiation;
						radiation = (radiation+msg.value)/2;
						if (started_moving)
						{
							receive_inspect();
							String toprint = "";
							if(currently_at!=null)
								toprint+=currently_at.toString(); 
							toprint+="/"+near_predlist_toString()+"\n"+old_rad+","+msg.value+" -> "+radiation;
						//	System.out.println(toprint);
						}
					}
				});

		// so we can get the current pose
		bridge.subscribe(SubscriptionRequestMsg.generate("/current_pose").setType("geometry_msgs/Vector3"),
				new RosListenDelegate() {
					public void receive(JsonNode data, String stringRep) {
						MessageUnpacker<Vector3> unpacker = new MessageUnpacker<Vector3>(Vector3.class);
						Vector3 msg = unpacker.unpackRosMessage(data);
						// AJPFLogger.warning(logname, "Current pose: " + msg.x + " " + msg.y + " " +
						// msg.z);

						doAt(msg);
						doNear(msg);

					}
				});

	}

	String near_predlist_toString()
	{
		String toret = "";
		if(currently_near!=null) {
		for(Predicate p : currently_near)
		{
			toret +=p.toString()+",";
		}
		}
		return toret; 
	}
	void doNear(Vector3 msg) {

		if (dup_agentName != null) {
			ArrayList<Predicate> nearlocs = this.getNearLocs(msg.x, msg.y);
			if (this.currently_near == null) {
				// just has all the currently near percept s
				currently_near = new ArrayList<>();

			}

			// this is so we dont get multiple "near" percepts
			// which may be bad actually
			// if the percept is not in our new near locs
			// we can remove it
			for (Predicate p : currently_near) {
				if (!nearlocs.contains(p)) {
					removePercept(dup_agentName, p);
					AJPFLogger.warning(logname, "Removing percept " + p.toString());
				}
			}
			// we add all the percepts that are in our new near locs
			// not adding the ones that are already there (as in currently near)
			for (Predicate p : nearlocs) {
				if (!currently_near.contains(p)) {
					addPercept(dup_agentName, p);
					AJPFLogger.warning(logname, "Adding percept " + p.toString());
				}
			}
			// then we can set our currently near to this new thing
			currently_near.clear();
			currently_near.addAll(nearlocs);
		}
	}


	boolean doAt(Vector3 msg) {
		// do nothing if gwendolen hasn't started
		// needs to change maybe

		if (dup_agentName != null) {
			Predicate current_loc = getLoc(msg.x, msg.y);

			// we are currently no where
			// and we are actually no where
			// do nothing
			if (currently_at == null) {
				// we are currently no where
				// and we are actually some where
				// tell us where we are
				if (current_loc != null) {
					AJPFLogger.warning(logname, "Adding percept " + current_loc.toString());
					addPercept(dup_agentName, current_loc);
					currently_at = current_loc;
				}
			}

			else {
				if (current_loc != null) {
					// we are currently somewhere
					// and we are actually there

					// we are currently somewhere
					// and we are actually somewhere else

					if (!currently_at.getTerm(0).equals(current_loc.getTerm(0))) {
						AJPFLogger.warning(logname, "Removing percept " + currently_at.toString());
						removePercept(dup_agentName, currently_at);

						AJPFLogger.warning(logname, "Adding percept " + current_loc.toString());
						addPercept(dup_agentName, current_loc);
						currently_at = current_loc;

					}
				} else {
					// we are currently somewhere
					// and we are actually no where
					AJPFLogger.warning(logname, "Removing percept " + currently_at.toString());
					removePercept(dup_agentName, currently_at);
					currently_at = current_loc;
					return false;
				}
			}

		}
		return true;
	}

	Predicate getLoc(double cx, double cy) {
		// go over all the location coordinates
		for (String loc : this.location_coordinates.keySet()) {
			if (atLoc(cx, cy, loc)) {
				return this.at_location_predicates.get(loc);
			}
		}
		return null;
	}

	boolean atLoc(double cx, double cy, String loc) {
		return atLoc(cx, cy, location_coordinates.get(loc));
	}

	boolean atLoc(double cx, double cy, AbstractMap.SimpleEntry<Double, Double> loc) {
		return epsilonFromLoc(cx, cy, loc.getKey(), loc.getValue(), at_epsilon_error);
	}

	ArrayList<Predicate> getNearLocs(double cx, double cy) {
		ArrayList<Predicate> nearlocs = new ArrayList<>();
		// not excluding at loc, check for this in the beliefs in gwendolen
		for (String loc : this.location_coordinates.keySet()) {
			if (nearLoc(cx, cy, loc)) {
				nearlocs.add(this.near_location_predicates.get(loc));
			}
		}
		return nearlocs;
	}

	boolean nearLoc(double cx, double cy, String loc) {
		return nearLoc(cx, cy, location_coordinates.get(loc));
	}

	boolean nearLoc(double cx, double cy, AbstractMap.SimpleEntry<Double, Double> loc) {
		return distanceFromLocBetween(cx, cy, loc.getKey(), loc.getValue(), this.near_error, this.at_epsilon_error);
	}

	boolean distanceFromLocBetween(double cx, double cy, double lx, double ly, double upperBound, double lowerBound) {
		double dist = getDistance(cx,cy,lx,ly);
		if (dist < upperBound) {
			if (dist >= lowerBound) {
				return true;
			}

		}
		return false;

	}


	boolean epsilonFromLoc(double cx, double cy, double lx, double ly, double epsilon) {
		double dist = getDistance(cx,cy,lx,ly);
		if (dist < epsilon)
			return true;

		return false;

	}
	double getDistance(double cx, double cy, double lx, double ly)
	{
		double dist = (cx - lx) * (cx - lx) + (cy - ly) * (cy - ly);
		dist = Math.sqrt(dist);
		dist = Math.abs(dist); 
		return dist; 
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see ail.mas.DefaultEnvironment#executeAction(java.lang.String,
	 * ail.syntax.Action)
	 */
	public Unifier executeAction(String agName, Action act) throws AILexception {
		String actionname = act.getFunctor();
		int nterms = act.getTermsSize();
		Unifier u = new Unifier();
		if ((actionname.equals("move")) && nterms == 6) {
			NumberTerm lx = (NumberTerm) act.getTerm(0);
			NumberTerm ly = (NumberTerm) act.getTerm(1);
			NumberTerm lz = (NumberTerm) act.getTerm(2);
			NumberTerm ax = (NumberTerm) act.getTerm(3);
			NumberTerm ay = (NumberTerm) act.getTerm(4);
			NumberTerm az = (NumberTerm) act.getTerm(5);
			move(lx.solve(), ly.solve(), lz.solve(), ax.solve(), ay.solve(), az.solve());
		} else if ((actionname.equals("move")) && nterms == 3) {
			if (!started_moving)
				started_moving = true; 
			NumberTerm lx = (NumberTerm) act.getTerm(0);
			NumberTerm ly = (NumberTerm) act.getTerm(1);
			NumberTerm lz = (NumberTerm) act.getTerm(2);
			move(lx.solve(), ly.solve(), lz.solve());
		} else if (actionname.equals("keep_moving")) {
			NumberTerm period = (NumberTerm) act.getTerm(0);
			NumberTerm lx = (NumberTerm) act.getTerm(1);
			NumberTerm ly = (NumberTerm) act.getTerm(2);
			NumberTerm lz = (NumberTerm) act.getTerm(3);
			NumberTerm ax = (NumberTerm) act.getTerm(4);
			NumberTerm ay = (NumberTerm) act.getTerm(5);
			NumberTerm az = (NumberTerm) act.getTerm(6);
			keep_moving((int) period.solve(), lx.solve(), ly.solve(), lz.solve(), ax.solve(), ay.solve(), az.solve());
		} else if (actionname.equals("stop_moving")) {
			stop_moving();
		} else if (actionname.equals("inspect")) {
			try {
				Thread.sleep(3000);
				receive_inspect();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else if (actionname.equals("wait")) {
			NumberTerm period = (NumberTerm) act.getTerm(0);
			try {
				Thread.sleep((int) period.solve());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else if (actionname.equals("key_press")) {
			Scanner myObj = new Scanner(System.in);
			System.out.println(":::");
			myObj.nextLine(); // Read user input
		} else if (actionname.equals("get_random_coordinates")) {
			Random random = new Random();
			double x = random.nextDouble() * 20 - 10;
			double y = random.nextDouble() * 20 - 10;
			while (inaccessible(x, y)) {
				x = random.nextDouble() * 20 - 10;
				y = random.nextDouble() * 20 - 10;
			}
			NumberTerm lx = new NumberTermImpl(x);
			NumberTerm ly = new NumberTermImpl(y);
			act.getTerm(0).unifies(lx, u);
			act.getTerm(1).unifies(ly, u);
		} else if (actionname.equals("cancel_goal")) {
			cancel_goal();
		} else if (actionname.equals("debug")) {
			System.out.println("Debugging stuff");
		}

		Unifier theta = super.executeAction(agName, act);
		theta.compose(u);
		return theta;
	}

	private boolean inaccessible(double x, double y) {
		if (x > 0.6 && x < 4.2 && y > -7.3 && y < -3) {
			return true;
		} else if (x > -8 && x < -5 && y > -9 && y < -6.5) {
			return true;
		} else if (x > 7.5 && x < 8.6 && y < -4 && y > -7.2) {
			return true;
		} else if (x > -1.1 && x < 2.1 && y > 3 && y < 4.2) {
			return true;
		} else if (x > 8.7) {
			return true;
		} else if (y > 9) {
			return true;
		} else if (x < -9) {
			return true;
		} else if (y < -9) {
			return true;
		} else if (x > 2.9 && x < 3.4 && y > 2.8 && y < 6.4) {
			return true;
		} else if (x > -5.8 && x < -4.9 && y < -6.9 && y > -8.15) {
			return true;
		} else if (x > 4.6 && x < 5.1 && y > -3.8 && y < -3.4) {
			return true;
		} else if (x > -6.9 && x < -6.15 && y > -4.8 && y < -1.5) {
			return true;
		} else if (x > -2.6 && x < -1.9 && y < 0.8 && y < 6.6) {
			return true;
		}

		return false;
	}

	public void move(double lx, double ly, double lz, double ax, double ay, double az) {
		Publisher cmd_vel = new Publisher("/cmd_vel", "geometry_msgs/Twist", bridge);

		Vector3 linear = new Vector3(lx, ly, lz);
		Vector3 angular = new Vector3(ax, ay, az);
		cmd_vel.publish(new Twist(linear, angular));
	}

	public void move(double lx, double ly, double lz) {
		Publisher move_base = new Publisher("/gwendolen_to_move_base", "geometry_msgs/Vector3", bridge);
		move_base.publish(new Vector3(lx, ly, lz));
	}

	public void cancel_goal() {
		Publisher move_base = new Publisher("/gwendolen_to_move_base", "geometry_msgs/Vector3", bridge);
		move_base.publish(new Vector3(0, 0, 0));
	}

	public void keep_moving(int period, double lx, double ly, double lz, double ax, double ay, double az) {
		PeriodicPublisher cmd_vel = new PeriodicPublisher("/cmd_vel", "geometry_msgs/Twist", bridge);

		Vector3 linear = new Vector3(lx, ly, lz);
		Vector3 angular = new Vector3(ax, ay, az);
		cmd_vel.beginPublishing(new Twist(linear, angular), 2000);
	}

	public void stop_moving() {
		Publisher cmd_vel = new Publisher("/cmd_vel", "geometry_msgs/Twist", bridge);

		Vector3 linear = new Vector3(0.0, 0.0, 0.0);
		Vector3 angular = new Vector3(0.0, 0.0, 0.0);
		cmd_vel.publish(new Twist(linear, angular));
	}


	public void receive_inspect() {
		String status = null;
	//System.out.println("Radiation: " + radiation);
		if (radiation >= 120) {
			status = "red";
			Literal rad = new Literal("danger_red");
			addPercept(rad);
		} else if (radiation >= 90) {
			status = "orange";
			Literal rad = new Literal("danger_orange");
			addPercept(rad);
		} else {
			status = "green";
			Literal rad = new Literal("danger_green"); 
			addPercept(rad);
		}
		Publisher radstatus = new Publisher("radiationStatus", "std_msgs/String", bridge);
		radstatus.publish(new PrimitiveMsg<String>(status));
	}
	@Override
	public void init_after_adding_agents() {

		// add in all the location coordinates for the at thing
		for (int i = 0; i < this.getAgents().size(); i++) {
			AILAgent agent = this.getAgents().get(i);
			// cycle through to get all the location_coordinates
			dup_agentName = agent.getAgName();
			BeliefBase bb = agent.getBB();
			ArrayList<Literal> allLiterals = bb.getAll();
			for (int j = 0; j < allLiterals.size(); j++) {
				Literal aliteral = allLiterals.get(j);
				if (aliteral.getFunctor().equals("location_coordinate")) {
					if (this.location_coordinates == null) {
						this.location_coordinates = new HashMap<String, SimpleEntry<Double, Double>>();
						this.at_location_predicates = new HashMap<String, Predicate>();
						this.near_location_predicates = new HashMap<String, Predicate>();
					}
					add_coordinates_and_predicate(aliteral);
				}
			}
			// maybe add a predicate for unknown
			// but leave that for later
		}
		super.init_after_adding_agents();
	}

	void add_coordinates_and_predicate(Literal loc_coordinate) {
		AbstractMap.SimpleEntry<Double, Double> pair = null;
		// so now we get the name, and x, y
		Term loc_pred = loc_coordinate.getTerm(0);
		String locname = loc_pred.toString();
		double x = ((NumberTerm) loc_coordinate.getTerm(1)).solve();
		double y = ((NumberTerm) loc_coordinate.getTerm(2)).solve();
		pair = create_pair(x, y);
		this.location_coordinates.put(locname, pair);
		Predicate at_pred = new Predicate("at");
		at_pred.addTerm(loc_pred);
		this.at_location_predicates.put(locname, at_pred);
		Predicate near_pred = new Predicate("near");
		near_pred.addTerm(loc_pred);
		this.near_location_predicates.put(locname, near_pred);
	}

	AbstractMap.SimpleEntry<Double, Double> create_pair(double x, double y) {
		return new AbstractMap.SimpleEntry<Double, Double>(x, y);
	}

	@Override
	public boolean done() {
		return true;
	}

}
