package eass.ros.fumic.gui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;

public class GridCellLabel extends JLabel {
    int size = 10;
    public GridCellLabel(String val)
    {
        super();
        Border border = new LineBorder(Color.BLACK, 1);
        Dimension labelSize = new Dimension(this.size, this.size);
        setBorder(border);
//        setBackground();
        setVisible(true);
        setOpaque(true);
        setText(val);
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);

    }
}
