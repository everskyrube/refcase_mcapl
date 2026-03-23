package eass.ros.mimic;

import ail.mas.scheduling.NActionScheduler;
import ail.syntax.*;
import ail.util.AILexception;
import ajpf.util.AJPFLogger;
import eass.mas.DefaultEASSEnvironment;
import eass.ros.mimic.sim.ROSMimicBridge;


public class ROSMimicEASS extends DefaultEASSEnvironment {

    private String name= "ROS Mimic EASS";
    private String logname = "eass.ros.mimic.mas";

    private boolean isStarted = false;
    ROSMimicBridge rosMimicBridge;
    float radiation = 0;

    boolean started = false;
    public ROSMimicEASS() {
        super();
        super.scheduler_setup(this,new NActionScheduler(100));
        rosMimicBridge = new ROSMimicBridge();
    }

    @Override
    public void init_after_adding_agents(){
        AJPFLogger.info(logname,"Waiting to connect to server");
       rosMimicBridge.connect();

    }

    public Unifier exectueAction(String agName, Action act) throws AILexception
    {
        if(!isStarted) {
            if (rosMimicBridge.started) {
                addPercept(new Literal("started"));
            }
        }
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
            move(lx.solve(),ly.solve(),lz.solve(),ax.solve(),ay.solve(),az.solve());
        } else if ((actionname.equals("move")) && nterms == 3) {
            NumberTerm lx = (NumberTerm) act.getTerm(0);
            NumberTerm ly = (NumberTerm) act.getTerm(1);
            NumberTerm lz = (NumberTerm) act.getTerm(2);
            move(lx.solve(),ly.solve(),lz.solve());
        }
//        else if (actionname.equals("keep_moving")) {
//            NumberTerm period = (NumberTerm) act.getTerm(0);
//            NumberTerm lx = (NumberTerm) act.getTerm(1);
//            NumberTerm ly = (NumberTerm) act.getTerm(2);
//            NumberTerm lz = (NumberTerm) act.getTerm(3);
//            NumberTerm ax = (NumberTerm) act.getTerm(4);
//            NumberTerm ay = (NumberTerm) act.getTerm(5);
//            NumberTerm az = (NumberTerm) act.getTerm(6);
//            keep_moving((int) period.solve(),lx.solve(),ly.solve(),lz.solve(),ax.solve(),ay.solve(),az.solve());
//        } else if (actionname.equals("stop_moving")) {
//            stop_moving();
//        }
        else if (actionname.equals("inspect")) {
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
        }
        Unifier theta = super.executeAction(agName, act);
        theta.compose(u);
        return theta;
    }

    public void move(double lx, double ly, double lz, double ax, double ay, double az) {
       String tosend= String.format("move(%d,%d,%d,%d,%d,%d)",lx,ly,lz,ax,ay,az);
       rosMimicBridge.send(tosend);
    }

    public void move(double lx, double ly, double lz) {

        String tosend= String.format("move(%d,%d,%d)",lx,ly,lz);
        rosMimicBridge.send(tosend);
    }



    public void receive_inspect() {
        String status = null;
        System.out.println("Radiation: " + radiation);
        if (radiation >= 250 ) {
            status = "red";
            Literal rad = new Literal("danger_red");
            addUniquePercept("danger_red",rad);
        } else if (radiation >= 120) {
            status = "orange";
            Literal rad = new Literal("danger_orange");
            addUniquePercept("danger_orange",rad);
        } else {
            status = "green";
        }
        rosMimicBridge.send(status);
    }


    @Override
    public void cleanup(){
        done = true;
    }



}
