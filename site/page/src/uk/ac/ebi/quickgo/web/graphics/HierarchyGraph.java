package uk.ac.ebi.quickgo.web.graphics;

import uk.ac.ebi.interpro.common.collections.AutoMap;
import uk.ac.ebi.interpro.common.collections.CollectionUtils;
import uk.ac.ebi.interpro.common.StringUtils;
import uk.ac.ebi.interpro.common.ColourUtils;
import uk.ac.ebi.quickgo.web.data.*;
import uk.ac.ebi.quickgo.web.configuration.DataManager;
import uk.ac.ebi.quickgo.web.servlets.ImageServlet;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.awt.*;

public class HierarchyGraph {
    private StandardGraph<TermGraphNode, RelationGraphEdge> g = new StandardGraph<TermGraphNode, RelationGraphEdge>();

    private Map<Term, TermGraphNode> termMap = new HashMap<Term, TermGraphNode>();

    private Map<TermRelation, RelationGraphEdge> edgeMap = new HashMap<TermRelation, RelationGraphEdge>();

    int ancestorLimit = 0;
    int pixelLimit = 0;
	int overflow = 0;

    public GraphStyle style;

    public HierarchyGraph(int ancestorLimit, int pixelLimit, GraphStyle style) {
        this.ancestorLimit = ancestorLimit;
        this.pixelLimit = pixelLimit;
        this.style = style;
    }

	void addRelation(TermRelation relation) {
		if (!edgeMap.containsKey(relation)) {
			TermGraphNode pt = termMap.get(relation.parent);
			TermGraphNode ct = termMap.get(relation.child);
			RelationGraphEdge graphEdge = new RelationGraphEdge(pt, ct, relation.typeof);
			edgeMap.put(relation, graphEdge);
			if (pt != null && ct != null) {
				g.getEdges().add(graphEdge);
			}
		}
	}

    public TermGraphNode add(Term term) throws SQLException {
        TermGraphNode tgn = termMap.get(term);
        if (tgn != null) {
	        return tgn;
        }

		List<TermGraphNode> gn = g.getNodes();

	    List<Term> ancestors = term.getAllAncestors();
        for (Term ancestor : ancestors) {
            if (!termMap.containsKey(ancestor)) {
	            TermGraphNode node = new TermGraphNode(ancestor, style);
	            termMap.put(ancestor, node);
	            gn.add(node);
            }
        }

	    if (style.showChildren) {
		    for (TermRelation tr : term.children) {
		        if (!termMap.containsKey(tr.child)) {
			        TermGraphNode node = new TermGraphNode(tr.child, style);
			        termMap.put(tr.child, node);
			        gn.add(node);
		        }
		    }
	    }

        if (ancestorLimit > 0 && gn.size() > ancestorLimit) {
            overflow = gn.size() - ancestorLimit;
            return null;
        }

        for (Term ancestor : ancestors) {
            for (TermRelation relation : ancestor.parents) {
	            addRelation(relation);
            }
        }

	    if (style.showChildren) {
		    for (TermRelation tr : term.children) {
			    addRelation(tr);
		    }
	    }

        return termMap.get(term);
    }

    public HierarchyImage layout(ImageArchive imageArchive) {
        HierarchyImage image;
        if (overflow > 0) {
	        image = new HierarchyImage("Chart too large - there are more than " + ancestorLimit + " ancestor terms (overflow = " + overflow + ")");
        }
        else {
	        image = makeLayout(g, style, null, pixelLimit);
        }
        imageArchive.store(image);
        return image;
    }

    private static HierarchyImage makeLayout(StandardGraph<TermGraphNode, RelationGraphEdge> g, GraphStyle style, PrintWriter log, int pixelLimit) {
        HierarchicalLayout<TermGraphNode, RelationGraphEdge> layout = new HierarchicalLayout<TermGraphNode, RelationGraphEdge>(g, HierarchicalLayout.Orientation.TOP);
        layout.horizontalMargin = 2;
        layout.verticalMargin = 5;
        layout.edgeLengthHeightRatio = 5;
        layout.layout(log);
        long pixelCount = ((long)layout.getWidth()) * layout.getHeight();
        if (pixelLimit > 0 && pixelCount > pixelLimit) {
	        return new HierarchyImage("Chart too large: limit is " + pixelLimit + " pixels, actual size is " + pixelCount + " pixels");
        }
	    else {
	        return new HierarchyImage(layout.getWidth(), layout.getHeight(), g.getNodes(), g.getEdges(), style);
        }
    }

    public static void main(String[] args) throws Exception {
        List<String> goIDs = new ArrayList<String>();
        Map<String,String> parameters = new HashMap<String, String>();
        CollectionUtils.argsMap(args, 0, parameters, goIDs);
        File base = new File(StringUtils.nvl(parameters.get("base"), "data/mini"));

        TermOntology ontology = new TermOntology(new DataManager(base).getDirectory());

        Map<Term.Ontology,Terms> terms = AutoMap.enumMap(Term.Ontology.class, Terms.class, ontology);

        Map<String, Color> colourMap = new HashMap<String, Color>();
        Color currentColour = Color.white;

        String termList = StringUtils.nvl(parameters.get("terms"), "");
        if (!"".equals(termList)) {
            try {
          			BufferedReader reader = new BufferedReader(new FileReader(new File(termList)));
          			String goID;
          			while ((goID = reader.readLine()) != null) {
          				if (goID.startsWith("GO:")) {
                            Term t = ontology.getTerm(goID);

                            if (t != null) {
                                terms.get(t.aspect).add(t);
                                colourMap.put(goID, currentColour);
                            }
          				}
          			}
          		}
          		catch (IOException e) {
          			e.printStackTrace();
          		}
        }

        for (String parameter : goIDs) {
            if (parameter.startsWith("GO:")) {
                Term t = ontology.getTerm(parameter);

                if (t != null) {
                    terms.get(t.aspect).add(t);
                    colourMap.put(parameter, currentColour);
                }
            }
            else if (parameter.startsWith("#")) {
                int[] rgb=ColourUtils.decodeColour(parameter);
                currentColour = new Color(rgb[0], rgb[1], rgb[2], 255);
            }
        }

        String outputName = StringUtils.nvl(parameters.get("name"));

        GraphStyle style = new GraphStyle(parameters);

        for (Term.Ontology o : terms.keySet()) {
            System.out.println(o.text);
            HierarchyGraph graph = new HierarchyGraph(-1, -1, style);
            for (Term term : terms.get(o).getTerms()) {
                if (term.parents.size() > 0) {
                    graph.add(term).fill = colourMap.get(term.id());
                }
            }

            HierarchyImage image = makeLayout(graph.g, style, new PrintWriter(System.out, true), -1);
            FileOutputStream fos = new FileOutputStream(outputName + o.text + ".png");
            ImageServlet.renderPNG(image.render(), fos);
            fos.close();
        }
    }
}
