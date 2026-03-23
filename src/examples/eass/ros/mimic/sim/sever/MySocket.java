package eass.ros.mimic.sim.sever;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

@WebSocket
public class MySocket {
    Session simSession;
    Session eassSession;

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        System.out.println("Close: " + reason);
    }

    @OnWebSocketError
    public void onError(Throwable t) {
        System.out.println("Error: " + t.getMessage());
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.println("Connect: " + session.getRemoteAddress().getAddress());

        if(session.getRemoteAddress().getAddress().toString().equals("/0:0:0:0:0:0:0:1")){
            simSession = session;
            simConnect();
        }
        else if(session.getRemoteAddress().getAddress().toString().equals("/127.0.0.1"))
        {
            eassSession = session;
            eassConnect();
        }
        else {

            try {
                session.getRemote().sendString("Hello Webbrowser");
            } catch (IOException e) {
                System.out.println("IO Exception");
            }
        }
    }

    private void eassConnect()
    {
        try {
            eassSession.getRemote().sendString("Hello EASS");
        }
        catch (IOException e)
        {
            System.out.println("EASS session IO Exception");
            e.printStackTrace();
        }
    }
    private void simConnect()
    {
        try {
            simSession.getRemote().sendString("Hello gridworld");
        }
        catch (IOException e)
        {
            System.out.println("Sim session IO Exception");
            e.printStackTrace();
        }
    }
    @OnWebSocketMessage
    public void onMessage(Session session,String message) {
        if(session.equals(simSession)) {
            System.out.println("Sim message: " + message);
        }
        else if (session.equals(eassSession))
        {
            System.out.println("EASS message: " + message);
        }
            else{
            System.out.println("Message: " + message);
        }
    }
}