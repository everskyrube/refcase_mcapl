package eass.ros.mimic.sim;



import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.io.IOException;
import java.net.URI;

public class ROSMimicBridge {
    protected URI defaultUri;

    protected WebSocketClient client;
    private ROSMimicSocket rosmimicsocket;

    public boolean started = false;

    public ROSMimicBridge(){
        try {
        defaultUri = new URI("ws://localhost:8080/gridworld/example");
        client = new WebSocketClient();
        rosmimicsocket = new ROSMimicSocket();}
              catch (Exception e)
            {
                e.printStackTrace();
                System.out.println("Error");
            }
    }
    public void connect()
    {
        setupsocket();
        started = true;
    }
    private void setupsocket()
    {
        try {


            client.start();
            ClientUpgradeRequest request = new ClientUpgradeRequest();

            client.connect(rosmimicsocket,defaultUri,request);

        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("Error");
        }
    }

    public void close()
    {
        closesocket();
    }
    public void send(String message)
    {
        rosmimicsocket.send(message);
    }
    private void closesocket()
    {
        try {
            client.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @WebSocket
    public class ROSMimicSocket{
        Session connectedSession;
        @OnWebSocketConnect
        public void onConnect(Session session) throws IOException
        {
            connectedSession = session;

            session.getRemote().sendString("EASS");


        }

        @OnWebSocketMessage
        public void onMessage(String message){

            System.out.println("Received percept: "+message);
        }

        @OnWebSocketClose
        public void onClose(int statusCode, String reason)
        {
            System.out.println("WebSocket Closed. Code:" + statusCode);
        }

        public void send(String message)
        {
            System.out.println("sending command: "+message);
            try {
                connectedSession.getRemote().sendString(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
        @OnWebSocketError
        public void onError(Session session, Throwable err){
            System.out.println(err.getMessage());

        }
    }
}
