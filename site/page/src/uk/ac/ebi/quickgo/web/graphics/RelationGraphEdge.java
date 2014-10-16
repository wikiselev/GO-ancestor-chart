package uk.ac.ebi.quickgo.web.graphics;

import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.quickgo.web.render.*;
import uk.ac.ebi.quickgo.web.data.*;

import java.awt.*;
import java.awt.geom.*;

public class RelationGraphEdge extends StrokeEdge<TermGraphNode> implements JSONSerialise {

    private static final Stroke relationStroke = new BasicStroke(2f);
//    private static final Shape parentArrow = null; //StrokeEdge.standardArrow(8,6,2);
//    private static final Shape childArrow = null;
	private static final Shape arrow = StrokeEdge.standardArrow(8,6,2);

    RelationType type;

    public RelationGraphEdge(TermGraphNode parent, TermGraphNode child, RelationType rtype) {
        super(parent, child, Color.black, rtype.stroke == null ? relationStroke : rtype.stroke, (rtype.polarity == RelationType.Polarity.POSITIVE || rtype.polarity == RelationType.Polarity.BIPOLAR) ? arrow : null, (rtype.polarity == RelationType.Polarity.NEGATIVE || rtype.polarity == RelationType.Polarity.BIPOLAR) ? arrow : null);
        this.type = rtype;
        this.colour = rtype.color;
    }

    public class SVGEdge {
        public String svgPath;
        public String colour;

        SVGEdge() {

            PathIterator pi = route.getPathIterator(null);
            double[] locations = new double[6];
            final StringBuilder svgPathSB = new StringBuilder();
            while (!pi.isDone()) {
                int type = pi.currentSegment(locations);
                pi.next();
                switch (type) {
                    case PathIterator.SEG_MOVETO:
                        svgPathSB.append(" M ").append(nf(locations[0])).append(" ").append(nf(locations[1]));
                        break;
                    case PathIterator.SEG_LINETO:
                        svgPathSB.append(" L ").append(nf(locations[0])).append(" ").append(nf(locations[1]));
                        break;
                    case PathIterator.SEG_CUBICTO:
                        svgPathSB.append(" C ").append(nf(locations[0])).append(" ").append(nf(locations[1]))
                                .append(" ").append(nf(locations[2])).append(" ").append(nf(locations[3]))
                                .append(" ").append(nf(locations[4])).append(" ").append(nf(locations[5]));
                        break;
                }

            }

            svgPath = svgPathSB.toString();
            colour = getColourCode(RelationGraphEdge.this.colour);
        }

        private String nf(double location) {
            return String.valueOf(Math.round(location));
        }
    }

    public Object serialise() {
	    return new SVGEdge();
    }

    private String getColourCode(Color color) {
        return StringUtils.encodeHex((byte)color.getRed(), (byte)color.getGreen(), (byte)color.getBlue());
    }

    public static void main(String[] args) throws Exception {

        /*Object x = QuickGOMonitor.x;
        System.out.println(x.getClass().getCanonicalName());
        for (Field field : x.getClass().getDeclaredFields()) {
            System.out.println(field.getName() + "=" + field.get(x));
        }*/

    }
}
