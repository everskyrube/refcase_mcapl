package eass.ros.fumic.agent;

import ail.syntax.*;
import eass.mas.socket.EASSSocketClientEnvironment;
import ail.mas.scheduling.NActionScheduler;
import ail.util.AILSocketClient;
import ajpf.util.AJPFLogger;
//import ros.msgs.geometry_msgs.Vector3;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class GridworldEnvironment extends EASSSocketClientEnvironment {

    String logname = "eass.ros.fumic.agent.GridworldEnvironment";

    double radiation = 0;
    boolean received_started = false;
    String current_at_loc = null;
    ArrayList<Predicate> currently_near = null;
    int nearLb = 0;
    int nearUb = 1;
    boolean considerBehindLocsForNear = false;

    public GridworldEnvironment() {
        super();
        super.scheduler_setup(this, new NActionScheduler(200));
    }

    @Override
    public void readPredicatesfromSocket() {

        if (AJPFLogger.ltFine(logname)) {
            AJPFLogger.fine(logname, "Reading Values from Socket");
        }

        try {
//            System.out.println("Reading stuff");
            AILSocketClient socket = getSocket();
            int mb_id = -1;
            int mb_res = -1;
            double rad_val = -1;
            int started = -1;
            int robot_x = -1;
            int robot_y = -1;
            boolean readvalues = false;
            if (socket.pendingInput()) {
                robot_x = socket.readInt();
                robot_y = socket.readInt();
                mb_id = socket.readInt();
                mb_res = socket.readInt();
                rad_val = socket.readDouble();
                started = socket.readInt();
                readvalues = true;
            }

            while (socket.pendingInput()) {
                robot_x = socket.readInt();
                robot_y = socket.readInt();
                mb_id = socket.readInt();
                mb_res = socket.readInt();
                rad_val = socket.readDouble();
                started = socket.readInt();
                readvalues = true;
            }
            if (readvalues) {
                String toprint = String.format("read: movebase_result(%d,%d), radiation (but set to 0): %f, started: %d, xy:%d,%d", mb_id, mb_res, rad_val, started, robot_x, robot_y);
                radiation = 0;//rad_val;
                System.out.println(toprint);
                if (started > 0) {
                    if (!received_started) {
                        addPercept(new Literal("started"));
                        received_started = true;
                    }
                }
                Literal mb = new Literal("movebase_result");
                mb.addTerm(new NumberTermImpl(mb_id));
                mb.addTerm(new NumberTermImpl(mb_res));
                if (mb_res != -1) {
                    addUniquePercept("movebase_result", mb);
                }
                String at_loc_string = getLocName(robot_x, robot_y);
                Literal at_loc = new Literal("on");
                at_loc.addTerm(new Literal(at_loc_string));
                // only add this new percept if it did not exist before
                if (current_at_loc == null || !current_at_loc.equals(at_loc_string)) {
                    current_at_loc = at_loc_string;
                    addUniquePercept("on", at_loc);
                    doNear(robot_x,robot_y);
                    receive_inspect();
                }

            }

        } catch (Exception e) {
            AJPFLogger.warning(logname, e.getMessage());
        }


    }

    //calculate all near within bounds distance
    private ArrayList<Predicate> getNearLocs(int x, int y, int lb, int ub, boolean considerBehindLocs) {
        ArrayList<String> nearLocs = new ArrayList<>();
        ArrayList<Predicate> nearLocsPreds = new ArrayList<>();
        // based on lb being 0 meaning we want distance 1
        String locnameholder;
        ArrayList<int[]> nearcoords = new ArrayList<>();
        for (int i = ub; i > lb; i--) {
            // so the locations are
            // x+i, y / x, y+i / x, y-i
            // we dont consider places behind us unless true
            // if considerBehindLocs x-i / y
            nearcoords.add(new int[]{x + i, y});
            nearcoords.add(new int[]{x, y + i});
            nearcoords.add(new int[]{x, y - i});
            if (considerBehindLocs) {
                nearcoords.add(new int[]{x - i, y});
            }
        }
        for(int i = 0; i<nearcoords.size(); i++)
        {
            int xx = nearcoords.get(i)[0];
            int yy = nearcoords.get(i)[1];
            if(xx >= 0 && yy >=0 && xx <10 && yy <10)
            {
                locnameholder = getLocName(xx , yy);
                nearLocs.add(locnameholder);
                nearLocsPreds.add(getNearLocPred(locnameholder));

            }
        }
        AJPFLogger.fine(logname,"Near " + x + " " + y);
        //printing all the near locs
        for (int i = 0; i < nearLocs.size(); i++) {
            AJPFLogger.fine(logname,nearLocs.get(i));
        }
        return nearLocsPreds;
    }

    Predicate getNearLocPred(String locname) {
        Predicate np = new Predicate("near");
        np.addTerm(new Literal(locname));
        return np;
    }

    void doNear(int x, int y) {

        ArrayList<Predicate> nearlocs = getNearLocs(x, y, nearLb, nearUb, considerBehindLocsForNear);
        if (currently_near == null) {
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

    private String getLocName(int x, int y) {
        return String.format("l%d_%d", x, y);
    }

    public Unifier executeAction(String agName, ail.syntax.Action act) throws ail.util.AILexception {
        Unifier u = new ail.syntax.Unifier();
        String actionname = act.getFunctor();
        int nterms = act.getTermsSize();
        if ((actionname.equals("move")) && nterms == 6) {
            NumberTerm lx = (NumberTerm) act.getTerm(0);
            NumberTerm ly = (NumberTerm) act.getTerm(1);
            NumberTerm lz = (NumberTerm) act.getTerm(2);
            NumberTerm ax = (NumberTerm) act.getTerm(3);
            NumberTerm ay = (NumberTerm) act.getTerm(4);
            NumberTerm az = (NumberTerm) act.getTerm(5);
            move(lx.solve(), ly.solve(), lz.solve(), ax.solve(), ay.solve(), az.solve());
        } else if ((actionname.equals("move")) && nterms == 3) {
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
            myObj.nextLine();  // Read user input
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
        }


        u.compose(super.executeAction(agName, act));
        return u;

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


        AILSocketClient socket = getSocket();
        socket.writeInt((int) lx);
        socket.writeInt((int) ly);
    }

    public void move(double lx, double ly, double lz) {


        AILSocketClient socket = getSocket();
        socket.writeInt((int) lx);
        socket.writeInt((int) ly);
    }

    public void keep_moving(int period, double lx, double ly, double lz, double ax, double ay, double az) {


        AILSocketClient socket = getSocket();
        socket.writeInt((int) lx);
        socket.writeInt((int) ly);
    }

    public void stop_moving() {

//
//        AILSocketClient socket = getSocket();
//        socket.writeInt((int)lx);
//        socket.writeInt((int)ly);
    }

    public void receive_inspect() {
        String status = null;
        Literal rad = new Literal("radiation");
        System.out.println("Radiation: " + radiation);
        if (radiation >= 250) {
//            status = "red";
//            Literal rad = new Literal("danger_red");
//            addUniquePercept("danger_red", rad);

            rad.addTerm(new Literal("high"));

        } else if (radiation >= 120) {
            rad.addTerm(new Literal("almosthigh"));
//            status = "orange";
//            Literal rad = new Literal("danger_orange");
//            addUniquePercept("danger_orange", rad);
        } else {
//            status = "green";
            rad.addTerm(new Literal("low"));
        }
        addUniquePercept("radiation",rad);
//        publish("radiationStatus",new PrimitiveMsg<String>(status));
    }

    @Override
    public boolean done() {
        return false;
    }


}
