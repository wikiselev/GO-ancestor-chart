package uk.ac.ebi.quickgo.web.graphics;

import java.awt.*;
import java.awt.geom.*;

/**
 * LayoutNode with methods to render as a rectangle containing text and obtain an HTML image map.
 */
public class RectangularNode implements LayoutNode {

// Set by constructor
    int width;
    int height;
    String altText,url,target,text;
    Color fill,line;
    Stroke border;

// Set by the layout algorithm
    int x;
    int y;


    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

/**
 * Draw node
 * @param g2 target for drawing
 */
    public void render(Graphics2D g2) {
        g2.setColor(fill);
        g2.fillRect(x-width/2,y-height/2,width,height);
        g2.setColor(line);
        g2.setStroke(border);
        g2.drawRect(x-width/2,y-height/2,width,height);
        Rectangle2D r=g2.getFontMetrics().getStringBounds(text,g2);
        g2.drawString(text,(float)(x-r.getWidth()/2-r.getMinX()), (float) (y-r.getHeight()/2-r.getMinY()));
    }

/** Called by the layout algorithm */
    public void setLocation(int x,int y) {
        this.x = x;
        this.y = y;
    }

    public RectangularNode(int width, int height, String altText, String url, String target, String text, Color fill, Color line, Stroke border) {
        this.width = width;
        this.height = height;
        this.altText = altText;
        this.url = url;
        this.target = target;
        this.text = text;
        this.fill = fill;
        this.line = line;
        this.border = border;
    }

/**
 * Get HTML image map
 * @return image map as text string
 */
    public String getImageMap() {
        int left=x-width/2;
        int right=x+width/2;
        int top=y-height/2;
        int bottom=y+height/2;
        return "<area alt=\"" + altText + "\" "+((target!=null)?("target=\""+target+"\" "):"")+"title=\"" + altText + "\" shape=\"Rect\" href=\"" + url + "\" coords=\"" + left + "," + top + " " + right + "," + bottom + "\"/>";

    }
}
