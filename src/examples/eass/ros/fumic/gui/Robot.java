package eass.ros.fumic.gui;

import ail.util.AILSocketServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.HashMap;

public class Robot extends JPanel {

    public static final String ANSI_RESET = "\u001B[0m";

    // Declaring the color
    // Custom declaration
    public static final String ANSI_YELLOW = "\u001B[33m";


    Ellipse2D circle;
    private Color col;

    int x;
    int y;

    double radiation_value = 0.0;
    int movebase_result = -1;
    int movebase_id=0;
    boolean controlled;

    private int started = 0;

    private boolean read_values = false;

    private double parentSizeFraction = 0.25;
    protected ail.util.AILSocketServer socketServer;

    @Override
    protected void paintComponent(Graphics gh) {
        super.paintComponent(gh);
        doDrawing(gh);
    }

    public void redo_circle() {
        int size = get_size();
//        System.out.println("Robot size "+size);

        circle = new Ellipse2D.Float((int)(getWidth() *parentSizeFraction), (int)(getHeight() *parentSizeFraction), size, size);


    }

    private void colorPrint(String s)
    {
        System.out.println(ANSI_YELLOW
                + s
                + ANSI_RESET);
    }
    public Robot(Color col) {
        initialiseRobot(col, true);
    }

    public Robot(Color col, boolean controlled) {
        initialiseRobot(col, controlled);
    }

    public void initialiseRobot(Color col, boolean controlled) {

        this.controlled = controlled;
        this.col = col;
        x = 0;
        y = 0;
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
//                System.out.println("Resized");
                super.componentResized(e);

            }
        });
        setBackground(Color.WHITE);
        init_radiation();
        start_server();
    }

    private void start_server() {
        if (socketServer == null) {
            if (controlled) {
                colorPrint("Robot waiting for socket connection");
                socketServer = new AILSocketServer();
                colorPrint("Robot got socket connection");
            }

        }
    }

    public void set_controlled() {

        controlled = true;
        start_server();
        updateParameters();
    }

    public void move_left(int xbound, int ybound) {

        x++;

        if (x >= xbound) {
            x = 0;
            y++;
        }
        if (y >= ybound) {
            y = 0;
        }


    }

    private HashMap<Point,Double> radvals=null;

    private ArrayList<Point> getNeighbours(Point p)
    {
        Point[] genPoints = {new Point(p.x,p.y-1),
                new Point(p.x,p.y+1),
                new Point(p.x-1,p.y),
                new Point(p.x+1,p.y)
        };
        ArrayList<Point> points = new ArrayList<>();
        for(Point pp: genPoints)
        {
            if(pp.x >= 0 && pp.y>= 0)
            {
                points.add(pp);
            }
        }
        return points;
    }
    private void init_radiation()
    {
        if(radvals == null)
        {
            Point[] radPoints={new Point(2,2), new Point(2,0)};
            radvals = new HashMap<>();
            double almost_high = 125.34;
            double high = 261.12;

            // rad points
            for (Point p: radPoints)
            {
                // for each point
                // get the neighbours
                ArrayList<Point> ns = getNeighbours(p);
                for (Point np: ns)
                {
                    radvals.put(np,almost_high);
                }
                // and set it almost high
                radvals.put(p,high);
            }
        }
    }

    private boolean set_radiation()
    {
        Point p = new Point(x,y);
        if(radvals.containsKey(p))
        {
            setRadiation_value(radvals.get(p));
            return true;
        }
        else {
            setRadiation_value(0.0);
        }
        return false;
    }
    public void setRadiation_value(double radiation_value)
    {
        this.radiation_value = radiation_value;
    }
    private void readValues() {
        if (controlled) {
            try {
                colorPrint("reading stuff");
                x = socketServer.readInt();
                y = socketServer.readInt();
                String toprint = String.format("read x: %d, y: %d", x, y);
                colorPrint(toprint);
                read_values = true;
            } catch (Exception e) {
                System.err.println("READ ERROR: Closing socket");
                close();
            }
        } else {
            colorPrint("Robot not controlled");
        }
    }

    public void close() {
        if (controlled) {
            socketServer.close();
            colorPrint("Robot socket connection closed");
            controlled = false;
        }
    }

    public void updateParameters() {
        if (controlled) {
            if (socketServer.allok()) {
                try {
                    if (socketServer.pendingInput()) {
                        readValues();
                    }
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
                writeValues();
            } else {
                System.err.println("something wrong with socket server");
            }
        }
    }

    //write values
    public void writeValues() {
        set_radiation();
        if (controlled) {
            try {
                if(read_values)
                {
                    movebase_result = 3;
                    movebase_id = this.x*10+this.y;
                    read_values = false;
                } else
                {
                    movebase_result = -1;

                }
//                colorPrint("Robot Writing stuff ");
                String toprint = String.format("Robot writing: movebase_result(%d,%d), radiation:%f, started:%d, robotxy:%d,%d",movebase_id,movebase_result,radiation_value,started,this.x,this.y);
                colorPrint(toprint);
                //writing movebase result
                socketServer.writeInt(this.x);
                socketServer.writeInt(this.y);


                socketServer.writeInt(movebase_id);
                socketServer.writeInt(movebase_result);
//                socketServer.write("movebase_result(0,3)"); //TODO change this?
                // writing radiation
//                socketServer.write("radiation=0");
                socketServer.writeDouble(radiation_value);
                socketServer.writeInt(started);

                colorPrint("Robot done writing stuff");
            } catch (Exception e) {
                System.err.println("WRITE ERROR: Closing socket");
                close();
            }
        }
    }

    public void start() {
        started = 1;
    }

    private int get_size() {
//        System.out.println("Robot w "+getWidth()+" h "+getHeight());
        int minval = Math.min(getWidth(), getHeight());
        minval = (int)(minval *parentSizeFraction);
        return minval;

    }

    private void doDrawing(Graphics g) {
        redo_circle();
        Graphics2D g2d = (Graphics2D) g;
        Paint current_color = g2d.getPaint();
        g2d.setPaint(col);
        g2d.fill(circle);
        g2d.setPaint(current_color);
        g2d.dispose();
    }
}

