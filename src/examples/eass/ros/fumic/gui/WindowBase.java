package eass.ros.fumic.gui;

import java.awt.Container;

import javax.swing.JFrame;

public abstract class WindowBase {
    JFrame parent;
    void setParent(JFrame parent)
    {
        this.parent = parent;
    }
    public void close()
    {
        parent.dispose();
    }
    abstract Container getBasePanel();
    public void newWindow()
    {
        JFrame frame = new JFrame("Gridworld Robot Simulator");
        this.setParent(frame);
        frame.setContentPane(this.getBasePanel());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

}
