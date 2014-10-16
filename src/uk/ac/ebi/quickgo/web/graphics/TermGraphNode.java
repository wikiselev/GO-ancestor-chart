package uk.ac.ebi.quickgo.web.graphics;

import uk.ac.ebi.quickgo.web.data.*;
import uk.ac.ebi.quickgo.web.render.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class TermGraphNode implements Node, LayoutNode, JSONSerialise {
    public Font font;

    public Term term;
    public String name;
    public String id;

    public int x;
    public int y;
    public int width;
    public int height;
    public int topLine = 0;

    int[] colours = new int[0];
    private GraphStyle style;

    public TermGraphNode(String name, String id, GraphStyle style) {
        this.name = name;
        this.id = id;
        this.style = style;
        if (style.termIds && id.length() > 0) {
	        topLine = style.fontSize+1;
        }
        height = style.height;
        width = style.width;
        font = style.getFont();
    }

    public TermGraphNode(Term term, GraphStyle style) {
        this(term.name().replace('_', ' '), term.id(), style);
        this.term = term;

        if (style.slimColours) {
	        colours = new int[term.slims.size()];
	        for (int i = 0; i < term.slims.size(); i++) {
	            colours[i] = term.slims.get(i).colour;
	        }
        }
    }

    public int getWidth() {
	    return width;
    }

    public int getHeight() {
	    return height;
    }

    public void setLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int left() {
	    return x - (width / 2);
    }

    public int right() {
	    return x + (width / 2);
    }

    public int top() {
	    return y - (height / 2);
    }

    public int bottom() {
	    return y + (height / 2);
    }

    public Color fill = Color.white;

    public Color line = Color.black;
    public Stroke border = new BasicStroke(1);

    public void render(Graphics2D g2) {
        g2.setFont(font);

        g2.setColor(fill);
        g2.fillRect(left(), top(), width, height);

        g2.setColor(line);
        g2.setStroke(border);
        g2.drawRect(left(), top(), width, height);

        for (int i = 0; i < colours.length; i++) {
            g2.setColor(new Color(colours[i]));
            g2.fillRect(left() + (i * 10) + 1, bottom() - 3, 10, 4);
        }
        g2.setColor(line);

        FontMetrics fm = g2.getFontMetrics();

        reflow(name, fm, g2);

        int ypos = y - (yheight / 2) + topLine;
        for (TextLine line : lines) {
            line.draw(g2, x,ypos);
            ypos += line.height();
        }

        if (style.termIds) {
	        renderID(g2);
        }
    }

	//Color idColour = new Color(0x0066cc);
	Color idColour = new Color(0x006363);

    public void renderID(Graphics2D g2) {
        FontMetrics fm = g2.getFontMetrics();
        Rectangle2D r = fm.getStringBounds(id, g2);

	    g2.setColor(idColour);
	    g2.fillRect(left(), top(), width, (int)r.getHeight() + 4);

	    g2.setColor(Color.WHITE);
        g2.drawString(id, (float)(left() + (width - r.getWidth()) / 2), (float)(top() - r.getMinY() + 2));
    }

    static class TextLine {
        String text;
        Rectangle2D bounds;
        private Graphics2D g2;
        private FontMetrics fm;
        private Font f;

        public TextLine(Graphics2D g2, FontMetrics fm) {
            this.g2 = g2;
	        this.fm = fm;
            this.f = fm.getFont();
        }

        boolean fit(String text, int from, int to, int width) {
            String t = text.substring(from, to);

            Rectangle2D r= fm.getStringBounds(t, g2);
            if (r.getWidth() > width) {
	            return false;
            }
	        else {
	            this.text = t;
	            this.bounds = r;

	            return true;
            }
        }

        public int length() {
	        return text == null ? 0 : text.length();
        }

        public void draw(Graphics2D g2, int x, int y) {
            g2.drawString(text, (float)(x - bounds.getWidth() / 2 - bounds.getMinX()), y + f.getSize2D());
        }

        public int height() {
            return f.getSize();
        }
    }

    List<TextLine> lines = new ArrayList<TextLine>();
    int yheight;

    private void reflow(String text, FontMetrics fm, Graphics2D g2) {
	    int hmargin = 2;

        int start = 0;
        lines.clear();
        yheight = topLine;
        while (start < text.length()) {
            TextLine current = new TextLine(g2, fm);
            int end = start;
            while (end <= text.length() && current.fit(text, start, end, width - hmargin)) {
	            end = nextSpace(text, end + 1);
            }

            if (current.length() == 0) {
                end = start;
                while (end <= text.length() && current.fit(text, start, end, width - hmargin)) {
	                end++;
                }
            }

            if ((current.length() == 0) || (yheight + current.height() >= height)) {
	            break;
            }

            yheight += current.height();
            lines.add(current);
            start += current.length();
            while (start < text.length() && text.charAt(start) == ' ') {
	            start++;
            }
        }
    }

    private int nextSpace(String text, int from) {
        if (from > text.length()) {
	        return from;
        }
	    else {
	        int ixSpace = text.indexOf(" ", from);
	        if (ixSpace == -1) {
		        ixSpace = text.length();
	        }
	        return ixSpace;
        }
    }

    public class SVGRectangle {
        public int left = x - (width / 2);
        public int top = y - (height / 2);
        public int right = x + (width / 2);
        public int bottom = y + (height / 2);
        public Term term = TermGraphNode.this.term;
    }

    public Object serialise() {
	    return new SVGRectangle();
    }
}
