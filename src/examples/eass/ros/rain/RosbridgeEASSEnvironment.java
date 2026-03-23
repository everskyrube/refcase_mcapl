package eass.ros.rain;

import ail.util.AILConfig;
import ajpf.util.AJPFLogger;
import eass.mas.DefaultEASSEnvironment;
import ros.Publisher;
import ros.RosBridge;
import ros.tools.PeriodicPublisher;

import java.util.HashMap;

public abstract class RosbridgeEASSEnvironment extends DefaultEASSEnvironment {

    /**
     * The rosbridge socket
     */

    protected RosBridge rosbridge;

    // checking if we are connected
    protected boolean connectedtorosbridge= true;

    // name of the environment
    private String name= "Default EASS Rosbridge Environment";

    private String logname ="rosbridge.eass.mas";
    // default port
    protected int portnumber = 9090;

    // default host
    protected String defaulthost = "localhost";
    private HashMap<String, Publisher> publishers;
    private HashMap<String, PeriodicPublisher> periodicPublishers;
    /**
     * Constructor - creates rosbridge connection
     */
    public  RosbridgeEASSEnvironment(){
        super();
        rosbridge = new RosBridge();
        publishers = new HashMap<String, Publisher>();
        periodicPublishers = new HashMap<>();
    }

    public RosbridgeEASSEnvironment(int portnumber){
        super();
        rosbridge = new RosBridge();
        this.portnumber  = portnumber;
    }

    public RosbridgeEASSEnvironment(int portnumber,String hostaddress){
        super();
        rosbridge = new RosBridge();
        this.portnumber  = portnumber;
        this.defaulthost = hostaddress;
    }

    private String create_uri()
    {
        return "ws://"+this.defaulthost+":"+this.portnumber;
    }
    @Override
    public void init_after_adding_agents(){
        if(connectedtorosbridge){
            AJPFLogger.info(logname,"Waiting for connection");
            try {
                rosbridge.connect(create_uri(), true);
                AJPFLogger.info(logname,"ROSBridge setting up publishers and subscribers");
                set_rosbridge_subscribers();
                set_rosbridge_publishers();
            } catch ( Exception e)
            {
                AJPFLogger.severe(logname,e.getMessage());
                System.exit(0);
            }
            AJPFLogger.info(logname,"Connected to ROSBridge");
        }
    }

    public abstract void set_rosbridge_subscribers();

    public abstract void set_rosbridge_publishers();
    public void add_publisher(String name, String topic, String msgType)
    {
        publishers.put(name,new Publisher(topic,msgType,rosbridge));
    }
    public void add_periodic_publisher(String name, String topic, String msgType)
    {
        periodicPublishers.put(name,new PeriodicPublisher(topic,msgType,rosbridge));
    }

    public void publish(String name,Object msg)
    {
        publishers.get(name).publish(msg);
    }
    public void peridioc_publish(String name, Object msg, int period)
    {
        periodicPublishers.get(name).beginPublishing(msg,period);

    }

//    @Override
//    public void do_job() {
//        if(connectedtorosbridge)
//        {
//            if(rosbridge.hasConnected()) {
//                readPredicatesfromRosbridge();
//            }
//            else
//            {
//                System.err.println("something wrong with rosbridge");
//            }
//        }
//        else {
//            debuggingPredicates();
//        }
//    }
//
//    public abstract void readPredicatesfromRosbridge();
//
//    public void debuggingPredicates() {};

    @Override
    public void cleanup(){
        done = true;
        if(connectedtorosbridge)
        {
            rosbridge.closeConnection();
        }
    }

    @Override
    public boolean done(){
        if (done)
        {
            return true;
        }
        return false;
    }

    @Override
    public void configure(AILConfig config){
        if(config.containsKey("connectedtosocket"))
        {
            if(config.getProperty("connectedtosocket").equals("false"))
            {
                connectedtorosbridge = false;
            }
            else
            {
                connectedtorosbridge = true;
            }
        }
    }
    public void notConnectedToSocket() {connectedtorosbridge = false;}

    public boolean connectedToSocket() {return connectedtorosbridge;}

    public RosBridge getRosbridge() {return rosbridge;}
}

