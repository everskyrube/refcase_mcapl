package gwendolen.agilex;

import ail.mas.DefaultEnvironment;
import ail.syntax.*;
import com.fasterxml.jackson.databind.JsonNode;
import ros.Publisher;
import ros.RosBridge;
import ros.RosListenDelegate;
import ros.SubscriptionRequestMsg;
import ros.msgs.std_msgs.PrimitiveMsg;

import java.util.ArrayList;

public class RosEnv extends DefaultEnvironment {

	private static final String ROS_URL = "ws://localhost:9090";
	private static final String SCAN_TOPIC = "/scan";
	private static final String SCAN_TYPE = "sensor_msgs/LaserScan";
	private static final String CONTROL_TOPIC = "/gwendolen_control";
	private static final String CONTROL_TYPE = "std_msgs/Bool";
	private static final double SAFE_DISTANCE_THRESHOLD = 0.45;

	private static final String HALT_TOPIC = "/safehalt_request";
	private static final String HALT_TYPE = "std_msgs/Bool";

	private static final String ODOM_TOPIC = "/odom";
	private static final String ODOM_TYPE ="nav_msgs/Odometry";

	private volatile boolean robotStoppedFlag = false;
	private boolean lastStoppedState = false;

	private final RosBridge bridge;
	//private double lastLaserReading = Double.MAX_VALUE;

	public RosEnv() {
		super();
		bridge = new RosBridge();
		bridge.connect(ROS_URL, true);
		System.out.println("Environment started, connection with ROS established.");

		// Subscribe to laser scan topic
		bridge.subscribe(
				SubscriptionRequestMsg.generate(SCAN_TOPIC)
						.setType(SCAN_TYPE),
				new RosListenDelegate() {
					@Override
					public void receive(JsonNode data, String stringRep) {
						handleLaserScanData(data);
					}
				});

		// Subscribe to safeHaltRequested topic
		bridge.subscribe(
				SubscriptionRequestMsg.generate(HALT_TOPIC).setType(HALT_TYPE),
				new RosListenDelegate() {
					@Override
					public void receive(JsonNode msg, String s) { haldleSafeHaltRequested(msg);}
				}
		);

		// Subcribe to odometry Topic -> To check if the robot is still moving
		bridge.subscribe(
				SubscriptionRequestMsg.generate(ODOM_TOPIC)
						.setType(ODOM_TYPE),
				new RosListenDelegate() {
					@Override
					public void receive(JsonNode msg, String s) { handleOdometryData(msg); }
				}
		);

	}


	private void handleOdometryData(JsonNode msg){

		if (msg == null) return;
		JsonNode twistNode = msg.path("msg").path("twist").path("twist");
		//System.out.println("RAW ODOM" + twistNode.toString());

		if (twistNode.isMissingNode()) {
			return; // Not a valid odometry message
		}

		double linearX = twistNode.path("linear").path("x").asDouble(Double.NaN);
		double linearY = twistNode.path("linear").path("y").asDouble(Double.NaN);
		double angularZ = twistNode.path("angular").path("z").asDouble(Double.NaN);

		// System.out.printf("Odometry -> X: %.3f | Y: %.3f | Z: %.3f%n", linearX, linearY, angularZ);

		if (Double.isNaN(linearX) || Double.isNaN(linearY) || Double.isNaN(angularZ)) {
			return; // Ignore malformed message
		}

		boolean stopped =
			Math.abs(linearX) < 0.01 &&
			Math.abs(linearY) < 0.01 &&
			Math.abs(angularZ) < 0.01;

		if (stopped && !lastStoppedState){
			addPercept(new Literal("halt_observed"));
			//System.out.println("Percept generated: Robot Stopped");
		}

		lastStoppedState = stopped;

	}





	private void haldleSafeHaltRequested(JsonNode msg){

		boolean halt = msg.get("data").asBoolean();

		if (halt){
			addPercept(new Literal ("safe_halt_req"));
		}
	}


	private void handleLaserScanData(JsonNode data) {
		JsonNode ranges= data.get("msg").path("ranges");

		double minRange = extractMinRange(ranges);

		//System.out.printf("LidarValue: %f%n", minRange);

		if (minRange < SAFE_DISTANCE_THRESHOLD) {
			addPercept(new Literal("geofence_violation"));
		}
	}

	private double extractMinRange(JsonNode ranges) {
		double minValue = Double.MAX_VALUE;

		for (JsonNode valueNode : ranges){
			if (valueNode.isNumber()){
				double value = valueNode.asDouble();
				if (value < minValue){
					minValue = value;
				}
			}
		}
		return minValue;
	}



	@Override
	public Unifier executeAction(String agName, Action act) {
		if ("entry_stop".equals(act.getFunctor())) {
			publishStopSignal();
			System.out.println("Sending the message to stop moving");
		}

		//if ("first_tick_action")
		return new Unifier();
	}

	private void publishStopSignal() {
		Publisher control = new Publisher(CONTROL_TOPIC, CONTROL_TYPE, bridge);
		control.publish(new PrimitiveMsg<>(true));
	}

	@Override
	public boolean done() {
		return false;
	}
}

