### Issues:
* The main problem is threads, rosbridge runs seperate threads and with the EASS this causes a **Concurrent Comodification** sometimes. I've no idea how to solve it but I attempt to do it by adding `synchronized (percepts) { ... }` code blocks anywhere percepts are being added (this is probably not wise)

* The EASS code doesnt always work. I'm trying to figure out why but I have not been extremely successful.

### Current State of Things:

All agent programs can be found in: `src/examples/eass/ros`

#### Useful agent code

* The folder `fumic` contains a grid world that attempts to mimic the percepts we get in ROS. It can be used for testing agent code. However, the ROS simulation might have timing issues etc - so its best not to assume things will be perfect!
* The folder `rain` contains ROS code for things!!
    * `RosbridgeEASSEnvironment` is the class that extends `EASSEnvironment` for rosbridge
    * A sample class that extends `RosbridgeEASSEnvironment` is `RemoteInspectionEnv`, another is `AtNearEnv`
    * For a very simple demo, look at `RemoteInspectionEnv` and `remote-inspection.gwen`
    * For a working (but slow and messy) demo of plan weakening look at `pw_attempt_maintain.gwen` and `AtNearEnv`
    * The rest of the files are mostly just attempts at getting things to work
    * The most comprehensive of these is `last_go.gwen` (with `AtNearEnv`) which is my final attempt at all of this.

#### A description of the agent code can be found in the diagram/note labelled agent code description 