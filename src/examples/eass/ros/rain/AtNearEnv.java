package eass.ros.rain;

import ail.mas.scheduling.NActionScheduler;
import ail.mas.scheduling.RoundRobinScheduler;
import ail.semantics.AILAgent;
import ail.syntax.*;
import ail.util.AILexception;
import ajpf.util.AJPFLogger;
import com.fasterxml.jackson.databind.JsonNode;
import ros.RosBridge;
import ros.SubscriptionRequestMsg;
import ros.msgs.geometry_msgs.Vector3;
import ros.msgs.move_base_msgs.MoveBaseActionResult;
import ros.msgs.remote_inspection_msgs.Radiation;
import ros.msgs.std_msgs.PrimitiveMsg;
import ros.tools.MessageUnpacker;

import java.util.*;

public class AtNearEnv extends RosbridgeEASSEnvironment {

    String logname = "eass.ros.rain.ROSAtNearEnvironment";
    String atperceptname = "on";
    String nearperceptname = "near";
    String radiationperceptname = "radiation";

    boolean started_moving = false;
    float radiation = 0;
    double at_epsilon_error = 0.5;
    double near_error = 3;
    Predicate current_radiation_level = null;

    HashMap<String, AbstractMap.SimpleEntry<Double, Double>> location_coordinates;
    HashMap<String, Predicate> at_location_predicates;
    HashMap<String, Predicate> near_location_predicates;
    ArrayList<Predicate> currently_near;
    Predicate currently_at;

    HashMap<String, Predicate> radiation_percepts = new HashMap<>();
    String[] radlevels = {"low", "almosthigh", "high"};
    int previousLogLevel = 0;

    void initialise_radiation_percepts() {
        String radiation_pred_name = "radiation";
        for (String radlevel : radlevels) {
            Predicate rad_predicate = new Predicate(radiation_pred_name);
            rad_predicate.addTerm(new Predicate(radlevel));
            radiation_percepts.put(radlevel, rad_predicate);
        }
    }

    public AtNearEnv() {
        super();
        initialise_radiation_percepts();
        super.scheduler_setup(this, new NActionScheduler(100));
//        super.scheduler_setup(this,new RoundRobinScheduler());
        // because we want to know where we are
        addUniquePercept("started", new Literal("started"));
        AJPFLogger.info(logname, "Added started percept");


    }

    public void set_rosbridge_publishers() {
        AJPFLogger.info(logname, "setting up ROSBridge publishers");
        add_publisher("cmdvel", "/cmd_vel", "geometry_msgs/Twist");
        add_publisher("gwentomovebase", "/gwendolen_to_move_base", "geometry_msgs/Vector3");
        add_periodic_publisher("cmdvel", "/cmd_vel", "geometry_msgs/Twist");
        add_publisher("radiationStatus", "radiationStatus", "std_msgs/String");
        AJPFLogger.info(logname, "done setting up ROSBridge publishers");
    }

    public void set_rosbridge_subscribers() {

        AJPFLogger.info(logname, "setting up ROSBridge subscribers");
        try {
            RosBridge rosbridge = getRosbridge();
            rosbridge.subscribe(SubscriptionRequestMsg.generate("/move_base/result").setType("move_base_msgs/MoveBaseActionResult"), this::receive_movebase_result);

            rosbridge.subscribe(SubscriptionRequestMsg.generate("/radiation_sensor_plugin/sensor_0").setType("gazebo_radiation_plugins/Simulated_Radiation_Msg"), this::recieve_radiation_result);

            rosbridge.subscribe(SubscriptionRequestMsg.generate("/current_pose").setType("geometry_msgs/Vector3"), this::receive_current_pose);

        } catch (Exception e) {
            AJPFLogger.warning(logname, e.getMessage());

        }
        AJPFLogger.info(logname, "done setting up ROSBridge subscribers");
    }

    public void receive_current_pose(JsonNode data, String stringRep) {
        synchronized (percepts) {
            MessageUnpacker<Vector3> unpacker = new MessageUnpacker<Vector3>(Vector3.class);
            Vector3 msg = unpacker.unpackRosMessage(data);
            AJPFLogger.fine(logname, "Received pose info");
            doOn(msg);
            doNear(msg);
        }
    }

    boolean epsilonFromLoc(double cx, double cy, double lx, double ly, double epsilon) {
        double dist = getDistance(cx, cy, lx, ly);
        return (dist < epsilon);


    }

    double getDistance(double cx, double cy, double lx, double ly) {
        double dist = (cx - lx) * (cx - lx) + (cy - ly) * (cy - ly);
        dist = Math.sqrt(dist);
        dist = Math.abs(dist);
        return dist;
    }

    boolean atLoc(double cx, double cy, String loc) {
        return atLoc(cx, cy, location_coordinates.get(loc));
    }

    boolean atLoc(double cx, double cy, AbstractMap.SimpleEntry<Double, Double> loc) {
        return epsilonFromLoc(cx, cy, loc.getKey(), loc.getValue(), at_epsilon_error);
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

    void doOn(Vector3 msg) {
        synchronized (percepts) {
            // do nothing if gwendolen hasn't started
            // needs to change maybe


            Predicate current_loc = getLoc(msg.x, msg.y);

            if (current_loc != null) {
                if (current_loc != currently_at) {
                    AJPFLogger.fine(logname, "Adding percept " + current_loc.toString());
                    addUniquePercept(atperceptname, current_loc);
                    currently_at = current_loc;
                }

            } else {
                // we are currently somewhere
                // and we are actually no where
                if (currently_at != null) {
                    AJPFLogger.fine(logname, "Removing percept " + currently_at.toString());
                    removePercept(currently_at);
                    currently_at = current_loc;
                }

            }

        }
    }

    boolean nearLoc(double cx, double cy, String loc) {
        return nearLoc(cx, cy, location_coordinates.get(loc));
    }

    boolean nearLoc(double cx, double cy, AbstractMap.SimpleEntry<Double, Double> loc) {
        return distanceFromLocBetween(cx, cy, loc.getKey(), loc.getValue(), this.near_error, this.at_epsilon_error);
    }

    boolean distanceFromLocBetween(double cx, double cy, double lx, double ly, double upperBound, double lowerBound) {
        double dist = getDistance(cx, cy, lx, ly);
        if (dist < upperBound) {
            if (dist >= lowerBound) {
                return true;
            }

        }
        return false;

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

    void doNear(Vector3 msg) {
        synchronized (percepts) {

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
                    removePercept(p);
                    AJPFLogger.warning(logname, "Removing percept " + p.toString());
                }
            }
            // we add all the percepts that are in our new near locs
            // not adding the ones that are already there (as in currently near)
            for (Predicate p : nearlocs) {
                if (!currently_near.contains(p)) {
                    addPercept(p);
                    AJPFLogger.warning(logname, "Adding percept " + p.toString());
                }
            }
            // then we can set our currently near to this new thing
            currently_near.clear();
            currently_near.addAll(nearlocs);
        }
    }


    public void recieve_radiation_result(JsonNode data, String stringRep) {
        synchronized (percepts) {
            MessageUnpacker<Radiation> unpacker = new MessageUnpacker<Radiation>(Radiation.class);
            Radiation msg = unpacker.unpackRosMessage(data);
            radiation = (radiation + msg.value) / 2;
            if (started_moving) {
                receive_inspect();

            }
        }
    }

    String near_predlist_toString() {
        String toret = "";
        if (currently_near != null) {
            for (Predicate p : currently_near) {
                toret += p.toString() + ",";
            }
        }
        return toret;
    }

    public void receive_movebase_result(JsonNode data, String stringRep) {
        synchronized (percepts) {
            MessageUnpacker<MoveBaseActionResult> unpacker = new MessageUnpacker<MoveBaseActionResult>(MoveBaseActionResult.class);
            MoveBaseActionResult msg = unpacker.unpackRosMessage(data);
            AJPFLogger.info(logname, "received movebase result");
            Literal movebase_result = new Literal("movebase_result");
            movebase_result.addTerm(new NumberTermImpl(msg.header.seq));
            movebase_result.addTerm(new NumberTermImpl(msg.status.status));
            addUniquePercept("movebase_result", movebase_result);
        }
    }

    public Unifier executeAction(String agName, Action act) throws AILexception {
        String actionname = act.getFunctor();
        int nterms = act.getTermsSize();
        Unifier u = new Unifier();
        if ((actionname.equals("move")) && nterms == 3) {
            if (!started_moving)
                started_moving = true;
            NumberTerm lx = (NumberTerm) act.getTerm(0);
            NumberTerm ly = (NumberTerm) act.getTerm(1);
            NumberTerm lz = (NumberTerm) act.getTerm(2);
            move(lx.solve(), ly.solve(), lz.solve());
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
        } else if (actionname.equals("cancel_goal")) {
            cancel_goal();
        } else if (actionname.equals("debug")) {
            System.out.println("Debugging stuff");
        } else if (actionname.equals("debuglogfine")) {
            previousLogLevel = AJPFLogger.getLevel("ail.syntax.EvaluationAndRuleBaseIterator").intValue();
            AJPFLogger.setIntLevel("ail.syntax.EvaluationAndRuleBaseIterator", AJPFLogger.FINE);
        } else if (actionname.equals("logoff")) {
            AJPFLogger.setIntLevel("ail.syntax.EvaluationAndRuleBaseIterator", previousLogLevel);
        }

        Unifier theta = super.executeAction(agName, act);
        theta.compose(u);
        return theta;

    }

    public void cancel_goal() {
        publish("gwentomovebase", new Vector3(0, 0, 0));
    }


    public void move(double lx, double ly, double lz) {

        publish("gwentomovebase", new Vector3(lx, ly, lz));
    }


    public void receive_inspect() {

//        System.out.println("Radiation: " + radiation);
        String radlevel = "low";
        if (radiation >= 200) { //250
            radlevel = "high";
        } else if (radiation >= 120) { //120
            radlevel = "almosthigh";
        }
        Predicate updated_rad_level = radiation_percepts.get(radlevel);
        if (current_radiation_level == null || (current_radiation_level != updated_rad_level)) {
            addUniquePercept(radiationperceptname, updated_rad_level);
            publish("radiationStatus", new PrimitiveMsg<String>(radlevel));
        }
        current_radiation_level = radiation_percepts.get(radlevel);

    }

    public void init_after_adding_agents() {

        // add in all the location coordinates for the at thing
        for (int i = 0; i < this.getAgents().size(); i++) {
            AILAgent agent = this.getAgents().get(i);
            // cycle through to get all the location_coordinates
//            dup_agentName = agent.getAgName();
            BeliefBase bb = agent.getBB();
            ArrayList<Literal> allLiterals = bb.getAll();
            for (int j = 0; j < allLiterals.size(); j++) {
                Literal aliteral = allLiterals.get(j);
                if (aliteral.getFunctor().equals("location_coordinate")) {
                    if (this.location_coordinates == null) {
                        this.location_coordinates = new HashMap<String, AbstractMap.SimpleEntry<Double, Double>>();
                        this.at_location_predicates = new HashMap<String, Predicate>();
                        this.near_location_predicates = new HashMap<String, Predicate>();
                    }
                    add_coordinates_and_predicate(aliteral);
                }
            }
            // maybe add a predicate for unknown
            // but leave that for later
        }
        // hmmm add the the start percept?

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
        Predicate at_pred = new Predicate(atperceptname);
        at_pred.addTerm(loc_pred);
        this.at_location_predicates.put(locname, at_pred);
        Predicate near_pred = new Predicate(nearperceptname);
        near_pred.addTerm(loc_pred);
        this.near_location_predicates.put(locname, near_pred);
    }

    AbstractMap.SimpleEntry<Double, Double> create_pair(double x, double y) {
        return new AbstractMap.SimpleEntry<Double, Double>(x, y);
    }

    @Override
    public boolean done() {
        return false;
    }
}
