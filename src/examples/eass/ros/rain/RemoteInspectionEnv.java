package eass.ros.rain;

import ail.mas.scheduling.NActionScheduler;
import ail.syntax.*;
import ail.util.AILexception;
import ajpf.util.AJPFLogger;
import com.fasterxml.jackson.databind.JsonNode;
import ros.RosBridge;
import ros.SubscriptionRequestMsg;
import ros.msgs.geometry_msgs.Twist;
import ros.msgs.geometry_msgs.Vector3;
import ros.msgs.move_base_msgs.MoveBaseActionResult;
import ros.msgs.remote_inspection_msgs.Radiation;
import ros.msgs.std_msgs.PrimitiveMsg;
import ros.tools.MessageUnpacker;
import ros.tools.PeriodicPublisher;

import java.util.Random;
import java.util.Scanner;

public class RemoteInspectionEnv extends RosbridgeEASSEnvironment{


    int previousLogLevel = 0;
    String logname = "eass.ros.rain.RemoteInspectionEnvironment";

    float radiation = 0;

    public RemoteInspectionEnv(){
        super();
        super.scheduler_setup(this,new NActionScheduler(100));

    }

    public void set_rosbridge_publishers(){
        add_publisher("cmdvel","/cmd_vel", "geometry_msgs/Twist");
        add_publisher("gwentomovebase","/gwendolen_to_move_base", "geometry_msgs/Vector3");
        add_periodic_publisher("cmdvel","/cmd_vel", "geometry_msgs/Twist");
        add_publisher("radiationStatus","radiationStatus", "std_msgs/String");
    }
    public void set_rosbridge_subscribers(){
        if (AJPFLogger.ltFine(logname)){
            AJPFLogger.fine(logname,"Subscribing and setting up publishers");
        }
        try{
            RosBridge rosbridge = getRosbridge();
            rosbridge.subscribe(SubscriptionRequestMsg.generate("/move_base/result").setType("move_base_msgs/MoveBaseActionResult"), this::receive_movebase_result);

            rosbridge.subscribe(SubscriptionRequestMsg.generate("/radiation_sensor_plugin/sensor_0").setType("gazebo_radiation_plugins/Simulated_Radiation_Msg"),this::recieve_radiation_result);

        } catch (Exception e) {
            AJPFLogger.warning(logname,e.getMessage());

        }
    }

    public void recieve_radiation_result(JsonNode data, String stringRep)
    {
        MessageUnpacker<Radiation> unpacker = new MessageUnpacker<Radiation>(Radiation.class);
        Radiation msg = unpacker.unpackRosMessage(data);
        radiation = msg.value;
    }
    public void receive_movebase_result(JsonNode data, String stringRep)
    {
        MessageUnpacker<MoveBaseActionResult> unpacker = new MessageUnpacker<MoveBaseActionResult>(MoveBaseActionResult.class);
        MoveBaseActionResult msg = unpacker.unpackRosMessage(data);

        Literal movebase_result = new Literal("movebase_result");
        movebase_result.addTerm(new NumberTermImpl(msg.header.seq));
        movebase_result.addTerm(new NumberTermImpl(msg.status.status));
        addUniquePercept("movebase_result",movebase_result);
    }

    public Unifier executeAction(String agName, Action act) throws AILexception
    {
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
        } else if (actionname.equals("keep_moving")) {
            NumberTerm period = (NumberTerm) act.getTerm(0);
            NumberTerm lx = (NumberTerm) act.getTerm(1);
            NumberTerm ly = (NumberTerm) act.getTerm(2);
            NumberTerm lz = (NumberTerm) act.getTerm(3);
            NumberTerm ax = (NumberTerm) act.getTerm(4);
            NumberTerm ay = (NumberTerm) act.getTerm(5);
            NumberTerm az = (NumberTerm) act.getTerm(6);
            keep_moving((int) period.solve(),lx.solve(),ly.solve(),lz.solve(),ax.solve(),ay.solve(),az.solve());
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
            double x = random.nextDouble()*20 - 10;
            double y = random.nextDouble()*20 - 10;
            while (inaccessible(x, y)) {
                x = random.nextDouble()*20 - 10;
                y = random.nextDouble()*20 - 10;
            }
            NumberTerm lx = new NumberTermImpl(x);
            NumberTerm ly = new NumberTermImpl(y);
            act.getTerm(0).unifies(lx, u);
            act.getTerm(1).unifies(ly, u);
        }
        else if(actionname.equals("debuglogfine"))
        {
            previousLogLevel = AJPFLogger.getLevel("ail.syntax.EvaluationAndRuleBaseIterator").intValue();
            AJPFLogger.setIntLevel("ail.syntax.EvaluationAndRuleBaseIterator",AJPFLogger.FINE);
        }
        else if(actionname.equals("logoff"))
        {
            AJPFLogger.setIntLevel("ail.syntax.EvaluationAndRuleBaseIterator",previousLogLevel);
        }

        Unifier theta = super.executeAction(agName, act);
        theta.compose(u);
        return theta;

    }

    private boolean inaccessible(double x, double y) {
        if (x > 0.6 && x < 4.2 && y > -7.3 && y < -3) {
            return true;
        } else if (x > -8 && x < -5 && y > -9 && y < -6.5 ) {
            return true;
        } else if (x > 7.5 && x < 8.6 && y < -4 && y > -7.2) {
            return true;
        } else if (x > -1.1 && x < 2.1 && y > 3 && y < 4.2 ) {
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


        Vector3 linear = new Vector3(lx,ly,lz);
        Vector3 angular = new Vector3(ax,ay,az);
        publish("cmdvel",new Twist(linear, angular));
    }

    public void move(double lx, double ly, double lz) {

        publish("gwentomovebase",new Vector3(lx,ly,lz));
    }

    public void keep_moving(int period, double lx, double ly, double lz, double ax, double ay, double az) {

        Vector3 linear = new Vector3(lx,ly,lz);
        Vector3 angular = new Vector3(ax,ay,az);
        peridioc_publish("cmdvel",new Twist(linear, angular), 2000);
    }

    public void stop_moving() {


        Vector3 linear = new Vector3(0.0,0.0,0.0);
        Vector3 angular = new Vector3(0.0,0.0,0.0);
        publish("cmdvel",new Twist(linear, angular));
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
        publish("radiationStatus",new PrimitiveMsg<String>(status));
    }
    @Override
    public boolean done() {
        return false;
    }
}
