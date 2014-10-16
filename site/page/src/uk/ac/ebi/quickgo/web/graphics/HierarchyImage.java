package uk.ac.ebi.quickgo.web.graphics;

import uk.ac.ebi.quickgo.web.data.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

public class HierarchyImage extends ImageRender {
    private static final Font labelFont = new Font("Arial", Font.PLAIN, 10);
    private static final Font infoFont = new Font("Arial", Font.PLAIN, 9);
    private static final Font errorFont = new Font("Arial", Font.PLAIN, 16);

	public static class KeyNode implements Node {
		public int xCentre;
		public int yCentre;
		public int width;
		public int height;

		public static class RelationStroke extends StrokeEdge<Node> {
			private static final Stroke relationStroke = new BasicStroke(2f);
			private static final Shape arrow = StrokeEdge.standardArrow(8,6,2);

			RelationType type;

			public RelationStroke(int xFrom, int yFrom, int xTo, int yTo, RelationType rtype) {
			    super(null, null, Color.black, rtype.stroke == null ? relationStroke : rtype.stroke, (rtype.polarity == RelationType.Polarity.POSITIVE || rtype.polarity == RelationType.Polarity.BIPOLAR) ? arrow : null, (rtype.polarity == RelationType.Polarity.NEGATIVE || rtype.polarity == RelationType.Polarity.BIPOLAR) ? arrow : null);
			    this.type = rtype;
			    this.colour = rtype.color;

				GeneralPath shape = new GeneralPath();
				shape.moveTo(xFrom, yFrom);
				shape.lineTo(xTo, yTo);
				this.setRoute(shape);
			}
		}

		RelationType relType;

		public KeyNode(int xCentre, int yCentre, int width, int height, RelationType relType) {
			this.xCentre = xCentre;
			this.yCentre = yCentre;
			this.width = width;
			this.height = height;
			this.relType = relType;
		}

		Stroke border = new BasicStroke(1);

		public void render(Graphics2D g2) {
			int margin = height / 10;
			int boxSide = height - (2 * margin);
			int offsetY = boxSide / 4;
			new RelationStroke(xCentre + (width / 2) - boxSide - (2 * margin), yCentre + offsetY, xCentre - (width / 2) + boxSide + (2 * margin), yCentre + offsetY, relType).render(g2);

			int left = xCentre - (width / 2) + margin;
			int top = yCentre - (height / 2) + margin;
			drawBox(g2, left, top, boxSide, boxSide, "A");
			left +=  (width - margin - boxSide);
			drawBox(g2, left, top, boxSide, boxSide, "B");

			g2.setFont(labelFont);
			Rectangle2D r = g2.getFontMetrics().getStringBounds(relType.description, g2);
			g2.drawString(relType.description, (float)(xCentre - (r.getWidth() / 2)), (float)(yCentre + offsetY - (r.getHeight() / 2)));
		}

		void drawBox(Graphics2D g2, int left, int top, int width, int height, String label) {
			g2.setColor(Color.black);
			g2.setStroke(border);
			g2.drawRect(left, top, width, height);

			g2.setFont(labelFont);
			Rectangle2D r = g2.getFontMetrics().getStringBounds(label, g2);
			g2.drawString(label, (float)(left + (width / 2) - (r.getWidth() / 2)), (float)(top + (height / 2) + (r.getHeight() / 2)));
		}

		public int left() {
			return xCentre - width / 2;
		}
		public int right() {
			return xCentre + width / 2;
		}
		public int top() {
			return yCentre - height / 2;
		}
		public int bottom() {
			return yCentre + height / 2;
		}

		public String topic() {
			return relType.formalCode;
		}
	}

    public Collection<TermGraphNode> terms = new ArrayList<TermGraphNode>();
    public Collection<RelationGraphEdge> relations = new ArrayList<RelationGraphEdge>();
	public Collection<KeyNode> legend = new ArrayList<KeyNode>();

    private GraphStyle style;

    public String selected;
    public final String errorMessage;

    public String id() {
        return String.valueOf(System.identityHashCode(this));
    }

    public static final int keyMargin = 50;
    public static final int rightMargin = 10;
    public static final int bottomMargin = 16;
    public static final int minWidth = 250;

    public HierarchyImage(String errorMessage) {
        super(500, 100);
        this.errorMessage = errorMessage;
    }

    public HierarchyImage(int width, int height, Collection<TermGraphNode> terms, Collection<RelationGraphEdge> relations, GraphStyle style) {
        super(Math.max(minWidth, width + (style.key ? keyMargin + (style.width * 2) + rightMargin : 0)), height + bottomMargin);

        this.errorMessage = null;

        this.terms = terms;
        this.relations = relations;
        this.style = style;

        if (style.key) {
			Set<Terms> slims = new HashSet<Terms>();
			if (style.slimColours) {
				for (TermGraphNode node : terms) {
					if (node.term != null) {
						slims.addAll(node.term.slims);
					}
				}
			}

	        RelationType rta[] = { RelationType.ISA,  RelationType.PARTOF, RelationType.HASPART, RelationType.REGULATES, RelationType.POSITIVEREGULATES, RelationType.NEGATIVEREGULATES, RelationType.OCCURSIN };

			int knHeight = style.height / 2;
			int knY = knHeight;

	        int yMax = knY;
	        for (RelationType rt : rta) {
	            KeyNode kn = new KeyNode(super.width - style.width - rightMargin, knY, style.width * 2, knHeight, rt);
				legend.add(kn);
		        knY += knHeight;
		        yMax = kn.bottom();
	        }

			int pos = 20;
			for (Terms slim : slims) {
				TermGraphNode bottomNode = slimNode(slim.name, slim.name, pos);
				bottomNode.colours = new int[]{ slim.colour };
				pos += 3;
				yMax = bottomNode.bottom();
			}

			int bottom = yMax + bottomMargin;
			if (this.height < bottom) {
				this.height = bottom;
			}
        }
    }

    private TermGraphNode slimNode(String name, String id, int row) {
        TermGraphNode node = new TermGraphNode(name, id, style);
	    node.width += 26;
        node.height = node.height / 2;
        node.x = width - node.width / 2 - rightMargin;
        node.y = node.height * row / 2;
        terms.add(node);
        return node;
    }

    protected void render(Graphics2D g2) {
        if (errorMessage != null) {
            g2.setFont(errorFont);
            g2.setColor(Color.BLACK);
            g2.drawString(errorMessage, 5, 50);
        }
		else {
	        for (RelationGraphEdge relation : relations) {
	            relation.render(g2);
	        }
	        for (TermGraphNode term : terms) {
	            term.render(g2);
	        }
		    for (KeyNode ke : legend) {
			    ke.render(g2);
		    }

	        g2.setFont(infoFont);
	        g2.setColor(Color.BLACK);
	        g2.drawString("QuickGO - http://www.ebi.ac.uk/QuickGO", 5, height - g2.getFontMetrics().getDescent());
        }
    }
}
