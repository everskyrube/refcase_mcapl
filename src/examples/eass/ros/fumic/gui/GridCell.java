package eass.ros.fumic.gui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.abs;

// https://stackoverflow.com/questions/8023468/java-grid-of-clickable-elements
public class GridCell extends JLabel {

    public enum GridCellElementType {
        EMPTY, OBSTACLE, FS, DOOR
    }

    public enum GridCellStateType {
        NONE, GOAL, INITIAL, AVOID
    }


    public boolean isAdjacent(GridCell c) {
        if ((abs(c.xpos - xpos) == 1 && abs(c.ypos - ypos) == 0) || (abs(c.ypos - ypos) == 1 && abs(c.xpos - xpos) == 0)) {
            return true;
        }
        return false;
    }

    public String toString() {
        String toret = "[" + this.xpos + "," + this.ypos + "] " + this.stateType + " ";
        if (this.elementTypes != null) {
            for (GridCellElementType elem : this.elementTypes.keySet()) {
                if (elementTypes.get(elem)) {
                    toret += elem + " ";
                }

            }
        }
        return toret;
    }

    private static final EnumMap<GridCellElementType, Color> elemFillColors = new EnumMap<GridCellElementType, Color>(GridCellElementType.class);

    static {
        elemFillColors.put(GridCellElementType.EMPTY, Color.WHITE);
        elemFillColors.put(GridCellElementType.OBSTACLE, Color.BLACK);
        elemFillColors.put(GridCellElementType.FS, Color.GRAY);
        elemFillColors.put(GridCellElementType.DOOR, Color.PINK);
    }


    private static final EnumMap<GridCellStateType, Color> stateFillColors
            = new EnumMap<GridCellStateType, Color>(GridCellStateType.class);

    static {
        stateFillColors.put(GridCellStateType.NONE, Color.WHITE);
        stateFillColors.put(GridCellStateType.INITIAL, Color.BLUE);
        stateFillColors.put(GridCellStateType.GOAL, Color.GREEN);
        stateFillColors.put(GridCellStateType.AVOID, Color.RED
        );
    }


    HashMap<GridCellElementType, Boolean> elementTypes = new HashMap<>();
    GridCellStateType stateType;
    // each grid cell has a label
    // and a position
    // it also has a default color
    int xpos;
    int ypos;
    int size = 10;

    Color fillColor;

    public void updateType(GridCellStateType type) {
        if (this.stateType != type) {
            stateType = type;
        } else {
            this.stateType = GridCellStateType.NONE;
        }
        setBackground(getFillColor());
    }

    public boolean isObstacleOrGrid(GridCellElementType type) {
        if (type == GridCellElementType.EMPTY || type == GridCellElementType.OBSTACLE) {
            return true;
        }
        return false;
    }

    private void setAllOtherTypesToFalse(GridCellElementType type) {
        for (GridCellElementType e : GridCellElementType.values()) {
            if (e != type) {
                elementTypes.put(e, false);
            }
        }
    }

    private void setEmptyIfNeeded() {
        for (GridCellElementType e : GridCellElementType.values()) {
            if (elementTypes.get(e))
                return;
        }
        elementTypes.put(GridCellElementType.EMPTY, true);
    }

    private void clearEmptyAndObstacle() {
        elementTypes.put(GridCellElementType.EMPTY, false);
        elementTypes.put(GridCellElementType.OBSTACLE, false);
    }

    public void updateType(GridCellElementType type) {
        if (isObstacleOrGrid(type)) {
            setAllOtherTypesToFalse(type);
        } else {
            // clear empty and obstacle
            clearEmptyAndObstacle();
        }
        elementTypes.put(type, !elementTypes.get(type));
        setEmptyIfNeeded();
        setBackground(getFillColor());
    }

    public GridCell(int xpos, int ypos) {
        super();
        for (GridCellElementType e : GridCellElementType.values()) {
            elementTypes.put(e, false);
        }
        elementTypes.put(GridCellElementType.EMPTY, true);
        this.xpos = xpos;
        this.ypos = ypos;
        this.stateType = GridCellStateType.NONE;

        setLayout(new GridLayout());
        Border border = new LineBorder(getLineColor(), 1);
        Dimension labelSize = new Dimension(this.size, this.size);
        setBorder(border);
        setBackground(getFillColor());
        setVisible(true);
        setOpaque(true);
        setText(String.format("%d: %d,%d",xpos*10+ypos,xpos,ypos));
        //  addMouseListener(m);
    }

    Color getLineColor() {
        return Color.BLACK;
    }

    Color getElemFillColor() {
        Color c0 = null;
        // go over all the elements and combine the colors
        for (GridCellElementType e : GridCellElementType.values()) {
            if (elementTypes.get(e)) {
                c0 = combineColors(c0, elemFillColors.get(e));
            }
        }
        return c0;
    }

    Color combineColors(Color c0, Color c1) {
        if (c0 == null && c1 == null)
            return elemFillColors.get(GridCellElementType.EMPTY);
        if (c0 == null)
            return c1;
        if (c1 == null)
            return c0;
        double totalAlpha = c0.getAlpha() + c1.getAlpha();
        double weight0 = c0.getAlpha() / totalAlpha;
        double weight1 = c1.getAlpha() / totalAlpha;

        double r = weight0 * c0.getRed() + weight1 * c1.getRed();
        double g = weight0 * c0.getGreen() + weight1 * c1.getGreen();
        double b = weight0 * c0.getBlue() + weight1 * c1.getBlue();
        double a = Math.max(c0.getAlpha(), c1.getAlpha());

        Color newColor = new Color((int) r, (int) g, (int) b, (int) a);
        return newColor;

    }

    Color getFillColor() {
        if (elementTypes.get(GridCellElementType.EMPTY)) {
            fillColor = stateFillColors.get(stateType);
            return fillColor;
        }
        if (stateType == GridCellStateType.NONE) {
            fillColor = getElemFillColor();
            return fillColor;
        }
        Color c0 = getElemFillColor();
        Color c1 = stateFillColors.get(stateType);
        double totalAlpha = c0.getAlpha() + c1.getAlpha();
        double weight0 = c0.getAlpha() / totalAlpha;
        double weight1 = c1.getAlpha() / totalAlpha;

        double r = weight0 * c0.getRed() + weight1 * c1.getRed();
        double g = weight0 * c0.getGreen() + weight1 * c1.getGreen();
        double b = weight0 * c0.getBlue() + weight1 * c1.getBlue();
        double a = Math.max(c0.getAlpha(), c1.getAlpha());

        fillColor = new Color((int) r, (int) g, (int) b, (int) a);
        return fillColor;

    }

}
