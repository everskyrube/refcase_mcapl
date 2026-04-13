package gwendolen.refcase;
import ail.mas.DefaultEnvironment;
import ail.syntax.*;
import com.fasterxml.jackson.databind.JsonNode;
import ros.Publisher;
import ros.RosBridge;
import ros.RosListenDelegate;
import ros.SubscriptionRequestMsg;
import ros.msgs.std_msgs.PrimitiveMsg;

public class RosEnv extends DefaultEnvironment {

	private static final String ROS_URL = "ws://localhost:9090";

	private static final String CONTROL_TOPIC = "/gwendolen_control";
	private static final String CONTROL_TYPE = "std_msgs/Bool";
	
	private static final String STATE_TOPIC = "/gwendolen_state";
	private static final String STATE_TYPE = "std_msgs/String";

	private static final String MOVE_TOPIC = "/gwendolen_move_percept";
	private static final String MOVE_TYPE = "std_msgs/Bool";

	private static final String TICK_TOPIC = "/gwendolen_tick";
	private static final String TICK_TYPE = "std_msgs/Bool";

	private static final String GEOFENCEVIOLATION_TOPIC = "/gwendolen_geofence_violation";
	private static final String GEOFENCEVIOLATION_TYPE = "std_msgs/Bool";
	
	private static final String SCAN_TOPIC = "/scan";
	private static final String SCAN_TYPE = "sensor_msgs/LaserScan";
	private static final double SAFE_DISTANCE_THRESHOLD = 0.45;

	private static final String HALT_TOPIC = "/safehalt_request";
	private static final String HALT_TYPE = "std_msgs/Bool";

	private static final String ODOM_TOPIC = "/odom";
	private static final String ODOM_TYPE ="nav_msgs/Odometry";
	private boolean lastStoppedState = false;

	private final RosBridge bridge;



	public RosEnv() {
		super();
		bridge = new RosBridge();
		bridge.connect(ROS_URL, true);
		System.out.println("Environment started, connection with ROS established.");
		
		publishState("Monitoring");
		System.out.println("Publishing state Monitoring");

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
					public void receive(JsonNode msg, String s) { handleSafeHaltRequested(msg);}
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
			removePercept(new Literal("move"));
			addPercept(new Literal("halt_observed"));
			// System.out.println("Percept generated: Robot Stopped");
		}

		if (!stopped && lastStoppedState){
			removePercept(new Literal("halt_observed"));
			addPercept(new Literal("move"));
			// System.out.println("Percept generated: move");
		}

		lastStoppedState = stopped;

	}





	private void handleSafeHaltRequested(JsonNode msg){

		if (msg == null) return;

		JsonNode dataNode = msg.path("msg").path("data");

		if (dataNode.isMissingNode()){
			return;
		}

		boolean haltRequested = dataNode.asBoolean(false);

		if (haltRequested){
			addPercept(new Literal ("safe_halt_req"));
			System.out.println("Percept generated: safe_halt_req");
		}
	}


	private void handleLaserScanData(JsonNode data) {
		JsonNode ranges= data.get("msg").path("ranges");

		double minRange = extractMinRange(ranges);

		//System.out.printf("LidarValue: %f%n", minRange);

		if (minRange < SAFE_DISTANCE_THRESHOLD) {
			addPercept(new Literal("geofence_violation"));
			publishGeofenceViolation();
			System.out.println("Percept generated and published: geofence_violation");
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
			publishState("EnforcedStop");
			System.out.println("Publishing state EnforcedStop");
		}

		if ("waiting".equals(act.getFunctor())) {
			new Thread(() -> {
				try {
					Thread.sleep(2000);
					addPercept(new Literal("tick"));
					publishTick();
					System.out.println("Tick percept added and published");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}).start();
		}

		if ("publish_wait_for_halt".equals(act.getFunctor())) {
			publishState("WaitingForHalt");
			System.out.println("Publishing state WaitingForHalt");
		}

		if ("publish_safe_halt_active".equals(act.getFunctor())) {
			publishState("SafeHaltActive");
			System.out.println("Publishing state SafeHaltActive");
		}

		if ("publish_move_percept".equals(act.getFunctor())) {
			publishMovePercept();
			System.out.println("Publishing move percept");
		}

		return new Unifier();
	}

	private void publishStopSignal() {
		Publisher control = new Publisher(CONTROL_TOPIC, CONTROL_TYPE, bridge);
		control.publish(new PrimitiveMsg<>(true));
	}

	private void publishState(String state) {
		Publisher statePub = new Publisher(STATE_TOPIC, STATE_TYPE, bridge);
		statePub.publish(new PrimitiveMsg<>(state));
	}

	private void publishMovePercept() {
		Publisher movePub = new Publisher(MOVE_TOPIC, MOVE_TYPE, bridge);
		movePub.publish(new PrimitiveMsg<>(true));
	}

	private void publishTick() {
		Publisher tickPub = new Publisher(TICK_TOPIC, TICK_TYPE, bridge);
		tickPub.publish(new PrimitiveMsg<>(true));
	}

	private void publishGeofenceViolation() {
		Publisher geofencePub = new Publisher(GEOFENCEVIOLATION_TOPIC, GEOFENCEVIOLATION_TYPE, bridge);
		geofencePub.publish(new PrimitiveMsg<>(true));
	}

	@Override
	public boolean done() {
		return false;
	}
}

