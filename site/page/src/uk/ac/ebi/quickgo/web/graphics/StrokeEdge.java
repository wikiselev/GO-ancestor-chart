package uk.ac.ebi.quickgo.web.graphics;

import javax.imageio.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.io.*;

/**
 * LayoutEdge which has a render method to draw onto a canvas
 */
public class StrokeEdge<N extends Node> implements LayoutEdge<N> {

    N parent;
    N child;
    protected Shape route;
    protected Color colour;
    Stroke stroke;

    Shape parentArrow;
    Shape childArrow;
	private static final Stroke arrowStroke = new BasicStroke(2f);

    public static Shape standardArrow(float length,float width, float inset)  {
        GeneralPath arrow = new GeneralPath();
        arrow.moveTo(width/2, length);
        arrow.lineTo(0, 0);
        arrow.lineTo(-width/2, length);
        arrow.lineTo(0, length-inset);
        arrow.closePath();
        return arrow;
    }

    public static Shape standardArrow(float length,float width)  {
        return standardArrow(length,width,0);
    }

    public static Shape standardArrow(float length)  {
        return standardArrow(length,length/2,0);
    }


    public StrokeEdge(N parent, N child, Color colour, Stroke stroke, Shape parentArrow, Shape childArrow) {
        this.parent = parent;
        this.child = child;
        this.colour = colour;
        this.stroke = stroke;
        this.parentArrow = parentArrow;
        this.childArrow = childArrow;
    }

    public StrokeEdge(N parent, N child, Color colour, Stroke stroke) {
        this.parent = parent;
        this.child = child;
        this.colour = colour;
        this.stroke = stroke;
    }


    public void setParentArrow(Shape parentArrow) {
        this.parentArrow = parentArrow;
    }

    public void setChildArrow(Shape childArrow) {
        this.childArrow = childArrow;
    }

    public N getParent() {
        return parent;
    }

    public N getChild() {
        return child;
    }

    public void setRoute(Shape route) {
        this.route = route;
    }


    /**
     * Draw the edge.
     *
     * @param g2 Canvas
     */
    public void render(Graphics2D g2) {
        g2.setStroke(stroke);
        g2.setColor(colour);

        g2.draw(route);

	    g2.setStroke(arrowStroke);
        drawArrows(g2, route, parentArrow, childArrow);
    }

    public static void drawArrows(Graphics2D g2, Shape route, Shape parentArrow, Shape childArrow) {
        if (parentArrow == null && childArrow == null) return;

        PathIterator pi = route.getPathIterator(null, 2);


        double[] posn = new double[6];
        pi.currentSegment(posn);
        double x1, y1, xd1 = 0, yd1 = 0, x2, y2, xd2 = 0, yd2 = 0;
        boolean initial = true;
        x1 = posn[0];
        x2 = x1;
        y1 = posn[1];
        y2 = y1;
        pi.next();
        while (!pi.isDone()) {
            int type = pi.currentSegment(posn);
            double x=posn[0];
            double y=posn[1];
            if (type == PathIterator.SEG_CLOSE) {
                x = x1;
                y = y1;
            }

            

            pi.next();

            if (initial) {
                xd1 = x - x1;
                yd1 = y - y1;
                if (xd1!=0 || xd2!=0) initial = false;
            }


            if (x2!=posn[0] || y2!=posn[1]) {
                xd2 = x2 - x;
                yd2 = y2 - y;
            }            

            x2 = x;
            y2 = y;


        }


        drawArrow(g2, x1, y1, xd1, yd1, parentArrow);

        drawArrow(g2, x2, y2, xd2, yd2, childArrow);


    }

    private static void drawArrow(Graphics2D g2, double x, double y, double xd, double yd, Shape arrowhead) {



        if (xd == 0 && yd == 0) return;
        if (arrowhead == null) return;

        AffineTransform saveXform = g2.getTransform();

        g2.translate(x, y);
        g2.rotate(-Math.atan2(xd, yd));

        g2.fill(arrowhead);
        g2.draw(arrowhead);

        g2.setTransform(saveXform);
    }

    public static void main(String[] args) throws Exception {

        Shape parentArrow=standardArrow(20,10,10);
        Shape childArrow=standardArrow(20,10);

        //parentArrow=null;childArrow=null;

        BufferedImage image = new BufferedImage(800, 400, BufferedImage.TYPE_INT_ARGB);

        final Graphics2D g2 = image.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,RenderingHints.VALUE_STROKE_NORMALIZE);
        g2.setColor(Color.white);

        g2.fillRect(0, 0, 800, 400);


        draw(g2, parentArrow, childArrow, 200, 200, 300, 100);
        draw(g2, parentArrow, childArrow, 200, 200, 300, 200);
        draw(g2, parentArrow, childArrow, 200, 200, 300, 300);
        draw(g2, parentArrow, childArrow, 200, 200, 200, 100);
        draw(g2, parentArrow, childArrow, 200, 200, 200, 200);
        draw(g2, parentArrow, childArrow, 200, 200, 200, 300);
        draw(g2, parentArrow, childArrow, 200, 200, 100, 100);
        draw(g2, parentArrow, childArrow, 200, 200, 100, 200);
        draw(g2, parentArrow, childArrow, 200, 200, 100, 300);

        draw(g2, parentArrow, childArrow, 600, 200, 700, 300, 675, 250, 625, 250);


        ImageIO.write(image, "png", new FileOutputStream("arrows.png"));

    }

    private static void draw(Graphics2D g2, Shape parentArrow,Shape childArrow, int x1, int y1, int x2, int y2) {
        GeneralPath r = new GeneralPath();
        r.moveTo(x1, y1);
        r.lineTo(x2, y2);

        drawArrows(g2, r, parentArrow, childArrow);
        g2.draw(r);
    }

    private static void draw(Graphics2D g2, Shape parentArrow, Shape childArrow, int x1, int y1, int x2, int y2, int xh1, int yh1, int xh2, int yh2) {
        GeneralPath r = new GeneralPath();
        r.moveTo(x1, y1);
        r.curveTo(xh1, yh1, xh2, yh2, x2, y2);
        drawArrows(g2, r, parentArrow, childArrow);
        g2.draw(r);
    }
}

