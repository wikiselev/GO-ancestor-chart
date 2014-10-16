package uk.ac.ebi.quickgo.web.graphics;

import java.awt.geom.*;
import java.util.*;
import java.io.PrintWriter;

/**
 * Layout a hierarchical graph.
 * <p/>
 * Layout similar to the algorithm implemented by
 * <a href="http://www.graphviz.org/">GraphViz, AT&T laboratories</a>.
 * </p>
 * <pre>
 * Usage:
 * Graph g=....
 * HierarchicalLayout layout = new HierarchicalLayout(g,HierarchicalLayout.PARENT_TOP);
 * layout.layout();
 * ...layout.getWidth()...layout.getHeight()...
 * </pre>
 * <p/>
 * See SimpleGraphDraw.java for an example.
 * </p>
 * <p/>
 * The following documentation assumes orientation is PARENT_TOP,
 * in which case the parent nodes are at the top and
 * a level is a horizontal group of nodes.
 * Normally the magic field values should be left at their default values.
 * </p>
 */
public class HierarchicalLayout<N extends LayoutNode, E extends LayoutEdge<N>> {


// Public interface

    public enum Orientation {
        TOP, LEFT, BOTTOM, RIGHT
    }


    /**
     * Prepare for layout
     *
     * @param graph       Graph containing LayoutNodes and LayoutEdges, will not be modified.
     * @param orientation indicates which side of the graph the parents will be placed.
     */

    public HierarchicalLayout(Graph<N, E> graph, Orientation orientation) {
        this.orientation = orientation;

        this.originalGraph=graph;

        for (N n : graph.getNodes()) {
            HierarchicalNode hnode;
            if (orientation == Orientation.TOP || orientation == Orientation.BOTTOM)
                hnode = new HierarchicalNode(n, n.getHeight(), n.getWidth());
            else
                hnode = new HierarchicalNode(n, n.getWidth(), n.getHeight());
            nodeMap.put(n, hnode);
            hierarchicalGraph.nodes.add(hnode);
        }

        for (E e : graph.getEdges()) {

            HierarchicalNode parent = nodeMap.get(e.getParent());
            HierarchicalNode child = nodeMap.get(e.getChild());


            if (parent==null || child==null)
                System.out.println("Failed edge "+e.getParent()+(parent==null?":NOT FOUND ":" ")+e.getChild()+(child==null?":NOT FOUND ":" "));
            else
                originalEdges.add(new EdgeMapping(e,parent,child));


        }
    }

    /**
     * Compute layout.
     * This method finally calls setLocation on all the nodes and
     * setRoute on all the edges.
     */
    public void layout() {
        layout(null);
    }

    public void layout(PrintWriter log) {

        if (log!=null) log.println("Finding levels");

        findLevels();

        if (log!=null) log.println("Rationalizing");

        rationalise();

        if (log!=null) log.println("Find initial positions");

        for (Level l : levels) l.calcInitialPositions();

        if (log!=null) log.println("Order nodes in levels");

        orderNodesInLevels(log);

        if (log!=null) log.println("Calculate level locations");

        if (edgeRouting)
            edgeRoute();
        else
            calcLevelLocations();

        int minStart = Integer.MAX_VALUE;

        if (log!=null) log.println("Shifting levels");

        for (Level l : levels) minStart = Math.min(minStart, l.getStart());

        for (Level l : levels) l.shiftLeft(minStart);

        for (Level l : levels) withinLevelSize = Math.max(withinLevelSize, l.getWidth());

        if (log!=null) {
            for (Level l : levels) {
                log.println("Start:"+l.getStart()+" Width:"+l.getWidth()+" sorted:"+l.checkSorted());
            }
        }

        storeLayoutNodes();

        if (edgeRouting)
            storeLayoutEdgeRouting();
        else
            storeLayoutEdgesOld();


        if (log!=null) log.println("Layout calculated size:"+getWidth()+"x"+getHeight());
    }



    /**
     * After calling layout() call getWidth and getHeight
     * <br/>
     * All nodes will be in this bounding box.
     * <br/>
     * 0&lt;node.x+/-node.getWidth/2&lt;layout.getWidth
     * <br/>
     * Noting that x and y are the centres of the nodes.
     * All edge routes will also be in the bounding box.
     *
     * @return width of layout
     */

    public int getWidth() {
        return horizontalMargin*2+(orientation == Orientation.TOP || orientation == Orientation.BOTTOM ? withinLevelSize : betweenLevelSize);
    }


    /**
     * See getWidth()
     * <p/>
     * All nodes will be:
     * <p/>
     * 0&lt;node.y+/-node.getHeight/2&lt;layout.getHeight
     *
     * @return height of layout
     */
    public int getHeight() {
        return verticalMargin*2+(orientation == Orientation.TOP || orientation == Orientation.BOTTOM ? betweenLevelSize : withinLevelSize);
    }
//
// Magic constants.
// WARNING! Playing with these constants will mess with your head.
//

    /**
     * Ratio of maximum edge vertical distance to horizontal distance
     */
    public int edgeLengthHeightRatio = 3;
    /**
     * Number of passes up and down the levels to attempt to optimise node positions
     */
    public int reorderIterations = 25;
    /**
     * Minimum gap between levels
     */
    public int minLevelGap = 10;
    /**
     * Levels may be split if they have more than this number of nodes
     */
    public int maxLevelSize = 100;
    /**
     * Edges running though levels will be allocated this much horizontal space
     */
    public int insertedEdgeWidth = 20;
    /**
     * Minimum gap between nodes within a level
     */
    public int withinLevelGap = 20;
    /**
     * Extra gap between lines and nodes in a level
     */
    public int edgeRouteGap=10;
    /**
     * Extra gap between lines and nodes in a level
     */
    public int betweenLevelExtraGap=1;
    /**
     * Horizontal margin
     */
    public int horizontalMargin=2;
    /**
     * Vertical margin
     */
    public int verticalMargin=2;
//

// Internal implementation

// fields

    private StandardGraph<HierarchicalNode, HierarchicalEdge> hierarchicalGraph = new StandardGraph<HierarchicalNode, HierarchicalEdge>();
    private Orientation orientation;
    private boolean edgeRouting=false;
    private ArrayList<Level> levels = new ArrayList<Level>();
    private List<EdgeMapping> originalEdges=new ArrayList<EdgeMapping>();
    private int betweenLevelSize;
    private int withinLevelSize;
    private final Map<N, HierarchicalNode> nodeMap = new HashMap<N, HierarchicalNode>();
    private Graph<N,E> originalGraph;


// classes

    private class HierarchicalNode implements Node {

        // Underlying node will be null for inserted nodes
        LayoutNode underlying;

        HierarchicalNode[] above;
        HierarchicalNode[] below;

        /**
         * Minimum level on which this node could be located.
         * Used only by findLevel
         */
        int minLevelNumber=-1;

        /**
         * Level on which node is located
         */
        Level level;

        /**
         * Location within level
         */
        int location;

        /**
         * Size of node expressed wrt level
         */
        int withinLevelSize;

        /**
         * Size of node expressed wrt level
         */
        int betweenLevelSize;

        /**
         * sequence code - used to stabilise graph
         */
        int code;


        /**
         * Create nodedata for node
         *
         * @param node             Original node
         * @param betweenLevelSize size between levels (x or y depending on orientation)
         * @param withinLevelSize  size within levels
         */
        HierarchicalNode(LayoutNode node, int betweenLevelSize, int withinLevelSize) {
            underlying = node;
            this.betweenLevelSize = betweenLevelSize;
            this.withinLevelSize = withinLevelSize;
        }

        HierarchicalNode(int betweenLevelSize, int withinLevelSize) {
            this.betweenLevelSize = betweenLevelSize;
            this.withinLevelSize = withinLevelSize;
        }


        public void swap(HierarchicalNode other) {
            int temp=location;
            location=other.location;
            other.location=temp;
        }

        public Set<HierarchicalNode> getRealParents() {
            Set<HierarchicalNode> parents=new HashSet<HierarchicalNode>();
            for (EdgeMapping e : originalEdges) {
                if (e.child==this) parents.add(e.parent);
            }
            return parents;
        }

        public Set<HierarchicalNode> getRealChildren() {
            Set<HierarchicalNode> children=new HashSet<HierarchicalNode>();
            for (EdgeMapping e : originalEdges) {
                if (e.parent==this) children.add(e.child);
            }
            return children;
        }

        private int findLevel() {

            if (minLevelNumber != -1) return minLevelNumber;

            int maxParentLevel = -1;

            for (HierarchicalNode parent : getRealParents()) {
                maxParentLevel=Math.max(parent.findLevel(),maxParentLevel);
            }

            return minLevelNumber = maxParentLevel + 1;
        }

        private int assignLevel() {

            int minChildLevel=Integer.MAX_VALUE;

            for (HierarchicalNode child : getRealChildren()) {
                minChildLevel=Math.min(child.findLevel(),minChildLevel);
            }

            if (minChildLevel==Integer.MAX_VALUE) minChildLevel=minLevelNumber+1;

            int levelNumber=(minLevelNumber+minChildLevel)/2;

            while (levelNumber >= levels.size()) levels.add(new Level(hierarchicalGraph, levels.size()));

            level = levels.get(levelNumber);

            level.nodes.add(this);

            return levelNumber;
        }
    }

    private class EdgeMapping  {
        E underlying = null;
        List<HierarchicalEdge> componentEdges = new ArrayList<HierarchicalEdge>();
        private HierarchicalNode parent;
        private HierarchicalNode child;


        public EdgeMapping(E underlying, HierarchicalNode parent, HierarchicalNode child) {
            this.underlying = underlying;

            this.parent = parent;
            this.child = child;
        }
    }

    private class HierarchicalEdge implements Edge<HierarchicalNode>,Comparable<HierarchicalEdge> {
        HierarchicalNode parent, child;
        public int route;

        public HierarchicalNode getParent() {
            return parent;
        }

        public HierarchicalNode getChild() {
            return child;
        }

        public HierarchicalEdge(HierarchicalNode parent, HierarchicalNode child) {
            this.parent = parent;
            this.child = child;
        }

        public boolean overlaps(HierarchicalEdge edge) {
            if (edge.parent==parent || edge.child==child) return false;
            return minLocation() < edge.maxLocation() && maxLocation() > edge.minLocation();
        }

        private int maxLocation() {
            return Math.max(parent.location,child.location);
        }

        private int minLocation() {
            return Math.min(parent.location,child.location);
        }

        private int direction() {
            return parent.location-child.location;
        }

        private int centre() {
            return (parent.location+child.location)/2;
        }

        public int compareTo(HierarchicalEdge e) {
            if (direction()>0 && e.direction()>0) return centre()-e.centre();
            if (direction()<0 && e.direction()<0) return e.centre()-centre();
            return 0;
        }
    }


    private class Level {

        int levelNumber;
        //int size;
        int location, height;
        List<HierarchicalNode> nodes = new ArrayList<HierarchicalNode>();
        StandardGraph<HierarchicalNode, HierarchicalEdge> hierarchicalGraph;

        public Level(StandardGraph<HierarchicalNode, HierarchicalEdge> hierarchicalGraph, int levelNumber) {
            this.hierarchicalGraph = hierarchicalGraph;

            this.levelNumber = levelNumber;
        }

        private double crossingFraction(HierarchicalNode[] ns1, HierarchicalNode[] ns2) {
            if (ns1.length==0 || ns2.length==0) return 0.5;
                    int count=0;
                    for (HierarchicalNode n1 : ns1) {
                        for (HierarchicalNode n2 : ns2) {
                            if (n1.location>n2.location) count++;
                        }
                    }
                    return count/(ns1.length*ns2.length);
                }



        void reorderSwap(boolean down) {
            for (int i = 1; i < nodes.size(); i++) {
                HierarchicalNode prev = nodes.get(i-1);

                HierarchicalNode next = nodes.get(i);

                if ((down && crossingFraction(prev.above,next.above)>0.5) ||
                        (!down && crossingFraction(prev.below,next.below)>0.5)) {
/*

                if (crossingFraction(prev.above,next.above)+crossingFraction(prev.below,next.below)>=1) {
*/
                    prev.swap(next);
                    nodes.set(i-1,next);
                    nodes.set(i,prev);
                }            
            }
        }

        void removeOverlaps() {
            while (true) {

                Collections.sort(nodes, nodeLayoutComparator);

                boolean foundOverlap = false;
                for (int i = 1; i < nodes.size(); i++) {
                    HierarchicalNode a = nodes.get(i - 1);
                    HierarchicalNode b = nodes.get(i);

                    int overlap = minLevelGap + (a.location + a.withinLevelSize / 2) - (b.location - b.withinLevelSize / 2);
                    if (overlap > 0) {
                        foundOverlap = true;
                        a.location = a.location - overlap / 2 - 1;
                        b.location = b.location + overlap / 2 + 1;
                    }
                }


                if (!foundOverlap) break;
            }
        }


        void reorder(boolean down) {
            reorderAveragePosition(down);
            removeOverlaps();
        }

        private void reorderAveragePosition(boolean down) {
            for (HierarchicalNode node : nodes) {

                double total = 0;
                int connected = 0;


                if (node.above != null && down)
                    for (HierarchicalNode cf : node.above) {
                        connected++;
                        total += cf.location;
                    }

                if (node.below != null && !down)
                    for (HierarchicalNode cf : node.below) {
                        connected++;
                        total += cf.location;

                    }

                if (connected == 0) {
                    continue;
                    //throw new RuntimeException("No connected Nodes");
                } else {
                    total /= connected;
                }

                node.location = (int) total;
            }
        }


        void calcInitialPositions() {

            int width = 0;
            for (HierarchicalNode node : nodes) {

                node.location = width + node.withinLevelSize / 2;
                width += node.withinLevelSize + withinLevelGap;
            }


        }

        boolean checkSorted() {
            for (int i = 1; i < nodes.size(); i++) {
                if (nodes.get(i-1).location>=nodes.get(i).location) return false;
            }
            return true;

        }

        void shiftLeft(int delta) {
            for (HierarchicalNode node : nodes) node.location -= delta;
        }

        void getHeight() {
            int maxHeight = 0;

            for (HierarchicalNode node : nodes) {
                maxHeight = Math.max(maxHeight, node.betweenLevelSize);
            }

            maxHeight+=betweenLevelExtraGap*2;

            this.height =maxHeight;
        }

        void setLocation(int location) {
            this.location = location;
        }

        int getWidth() {
            final HierarchicalNode nd = nodes.get(nodes.size() - 1);
            return nd.location + nd.withinLevelSize / 2;
        }


        int getStart() {
            final HierarchicalNode nd = nodes.get(0);
            return nd.location - nd.withinLevelSize / 2;
        }


        public void attach(Level above, Level below) {

            List<HierarchicalNode> attached=new ArrayList<HierarchicalNode>();

            for (int j = 0; j < nodes.size(); j++) {
                HierarchicalNode nj = nodes.get(j);

                attached.clear();

                if (above != null)
                    for (HierarchicalNode na : above.nodes)
                        if (hierarchicalGraph.connected(na, nj)) attached.add(na);


                nj.above= attached.toArray(new HierarchicalLayout.HierarchicalNode[attached.size()]);

                attached.clear();

                if (below != null)
                    for (HierarchicalNode na : below.nodes)
                        if (hierarchicalGraph.connected(na, nj)) attached.add(na);

                nj.below= attached.toArray(new HierarchicalLayout.HierarchicalNode[attached.size()]);
            }

        }

    }

    private Comparator<HierarchicalNode> nodeLayoutComparator = new Comparator<HierarchicalNode>() {

        public int compare(HierarchicalNode h1, HierarchicalNode h2) {
            return h1.location - h2.location;
        }
    };

// methods

    private void findLevels() {
        for (HierarchicalNode n : hierarchicalGraph.nodes) n.findLevel();
        for (HierarchicalNode n : hierarchicalGraph.nodes) n.assignLevel();
    }


    private void rationalise(EdgeMapping e, StandardGraph<HierarchicalNode, HierarchicalEdge> g) {
        int parentLevel = e.parent.level.levelNumber;
        int childLevel = e.child.level.levelNumber;


            //System.out.println("Rationalise "+parentLevel+" "+childLevel);
            HierarchicalNode a = e.parent;
            for (int i = parentLevel + 1; i <= childLevel; i++) {

                HierarchicalNode b;
                if (i == childLevel) {
                    b = e.child;
                } else {
                    b = new HierarchicalNode(-1, insertedEdgeWidth);
                    b.level = levels.get(i);
                    b.level.nodes.add(b);

                }
                HierarchicalEdge insertedEdge = new HierarchicalEdge(a, b);
                g.edges.add(insertedEdge);
                g.nodes.add(b);
                e.componentEdges.add(insertedEdge);

                a = b;
            }


    }

    private void rationalise() {




        for (EdgeMapping e : originalEdges) rationalise(e, hierarchicalGraph);

        int s = levels.size();

        for (int i = 0; i < s; i++) {
            Level p = (i == 0) ? null : levels.get(i - 1);
            Level l = levels.get(i);
            Level n = (i == s - 1) ? null : levels.get(i + 1);
            l.attach(p, n);
        }


    }

    private void orderNodesInLevels(PrintWriter log) {
        for (int j = 0; j < reorderIterations; j++) {
            if (log!=null) log.println("phase:"+j);
            int s = levels.size();

            for (int i = 0; i < s; i++) {
                Level l = levels.get(i);
                l.reorder(true);
                //l.reorderSwap(true);
            }

            for (int i = s - 1; i >= 0; i--) {
                Level l = levels.get(i);
                l.reorder(false);
                //l.reorderSwap(false);
            }

        }



        for (Level level : levels) {
            /*level.reorder(true);
            level.reorder(false);*/
            level.removeOverlaps();
        }
    }


    private void calcLevelLocations() {
        int height = -betweenLevelExtraGap;

        Level p = null;

        for (Level l : levels) {

            int maxLength = 0;

            // Calculate maximum edge length

            if (p != null) {
                for (HierarchicalNode n1 : l.nodes) {
                    for (HierarchicalNode n2 : p.nodes) {
                        if (hierarchicalGraph.connected(n1, n2)) {
                            maxLength = Math.max(maxLength, Math.abs(n1.location - n2.location));
                        }
                    }
                }
                //System.out.println("Max Length "+maxLength);
                height += Math.max(minLevelGap, maxLength / edgeLengthHeightRatio);
            }



            int maxHeight = 0;

            for (HierarchicalNode node : l.nodes) {
                maxHeight = Math.max(maxHeight, (node).betweenLevelSize);
            }

            maxHeight+=betweenLevelExtraGap*2;

            l.getHeight();

            height += l.height / 2;

            l.setLocation(height);

            height += l.height /2;

            p = l;
        }

        betweenLevelSize = height-betweenLevelExtraGap;
    }

    private void edgeRoute() {



        int height = -betweenLevelExtraGap;
        Level p = null;
        for (Level l : levels) {

            height+=edgeRouteGap;

            List<List<HierarchicalEdge>> routes=new ArrayList<List<HierarchicalEdge>>();

            if (p!=null) {

                List<HierarchicalEdge> edges=new ArrayList<HierarchicalEdge>();
                for (HierarchicalNode n1 : l.nodes) {
                    for (HierarchicalNode n2 : p.nodes) {
                        HierarchicalEdge edge = hierarchicalGraph.findEdge(n2, n1);
                        if (edge!=null) edges.add(edge);
                    }
                }

                for (HierarchicalEdge edge : edges) {
                    int chosenRoute=0;
                    while  (chosenRoute<routes.size()) {
                        boolean overlaps = false;
                        for (HierarchicalEdge e : routes.get(chosenRoute)) {
                            overlaps |= e.overlaps(edge);
                        }
                        if (!overlaps) break;
                        chosenRoute++;
                    }
                    if (chosenRoute>=routes.size()) routes.add(new ArrayList<HierarchicalEdge>());
                    routes.get(chosenRoute).add(edge);
                    edge.route=chosenRoute*edgeRouteGap+height;
                }


            }

            height+=routes.size()*edgeRouteGap;

            l.getHeight();

            height += l.height / 2;

            l.setLocation(height);

            height += l.height /2;

            p=l;
        }
        betweenLevelSize = height-betweenLevelExtraGap;
    }



    private int x(int within,int between) {
        switch (orientation) {
            case LEFT:return horizontalMargin+between;
            case RIGHT:return horizontalMargin+betweenLevelSize-between;
            case TOP:case BOTTOM:return horizontalMargin+within;
        }
        return 0;
    }

    private int y(int within,int between) {
        switch (orientation) {
            case TOP:return verticalMargin+between;
            case BOTTOM:return verticalMargin+betweenLevelSize-between;
            case LEFT:case RIGHT:return verticalMargin+within;
        }
        return 0;
    }

    private void storeLayoutNodes() {


            for (HierarchicalNode n : hierarchicalGraph.nodes) {
                if (n.underlying == null) continue;
                n.underlying.setLocation(x(n.location,n.level.location), y(n.location,n.level.location));
            }
    }


    private void storeLayoutEdgesOld() {


        for (EdgeMapping e : originalEdges) {
            GeneralPath shape = new GeneralPath();
            boolean first=true;

            for (HierarchicalEdge edge : e.componentEdges) {

                HierarchicalNode parent = edge.getParent();
                HierarchicalNode child = edge.getChild();

                int parentLocation = parent.location;
                int childLocation = child.location;

                int levelParent = parent.level.location + parent.level.height / 2;
                int levelChild = child.level.location - child.level.height / 2;

                int levelCentre = (levelParent + levelChild) / 2;

                int nodeParent = parent.level.location + parent.betweenLevelSize / 2;
                int nodeChild = child.level.location - child.betweenLevelSize / 2;



                if (first) shape.moveTo(x(parentLocation, nodeParent),y(parentLocation, nodeParent));
                shape.lineTo(x(parentLocation, levelParent),y(parentLocation, levelParent));

                shape.curveTo(x(parentLocation, levelCentre),y(parentLocation, levelCentre),
                        x(childLocation, levelCentre),y(childLocation, levelCentre),
                        x(childLocation, levelChild),y(childLocation, levelChild));


/*
                shape.curveTo(x(parentLocation, levelChild),y(parentLocation, levelChild),
                        x(childLocation, levelParent),y(childLocation, levelParent),
                        x(childLocation, levelChild),y(childLocation, levelChild));
*/

                shape.lineTo(x(childLocation, nodeChild),y(childLocation, nodeChild));
                first=false;
            }

            e.underlying.setRoute(shape);

        }


    }

    private void storeLayoutEdgeRouting() {


        for (HierarchicalNode n : hierarchicalGraph.nodes) {
            if (n.underlying == null) continue;
            n.underlying.setLocation(x(n.location,n.level.location), y(n.location,n.level.location));
        }

        for (EdgeMapping e : originalEdges) {
            GeneralPath shape = new GeneralPath();
            boolean first=true;

            for (HierarchicalEdge edge : e.componentEdges) {

                HierarchicalNode parent = edge.getParent();
                HierarchicalNode child = edge.getChild();

                int parentLocation = parent.location;
                int childLocation = child.location;

                int levelParent = parent.level.location + parent.level.height / 2;
                int levelChild = child.level.location - child.level.height / 2;

                int routePosition=edge.route;

                /*int levelParent=routePosition-edgeRouteGap;
                int levelChild=routePosition+edgeRouteGap;
*/

                //int curveSize=Math.abs(parentLocation-childLocation);
                int curveSize=edgeRouteGap;

                int curve;
                if (parentLocation<childLocation) curve=Math.min(curveSize,(childLocation-parentLocation)/2);
                else curve=Math.max(-curveSize,(childLocation-parentLocation)/2);

                int nodeParent = parent.level.location + parent.betweenLevelSize / 2;
                int nodeChild = child.level.location - child.betweenLevelSize / 2;



                if (first) shape.moveTo(x(parentLocation, nodeParent),y(parentLocation, nodeParent));
                shape.lineTo(x(parentLocation, levelParent),y(parentLocation, levelParent));

                shape.quadTo(x(parentLocation, routePosition),y(parentLocation,routePosition),
                        x(parentLocation+curve, routePosition),y(parentLocation+curve, routePosition));
                shape.lineTo(x(childLocation-curve, routePosition),y(childLocation-curve, routePosition));
                shape.quadTo(x(childLocation, routePosition),y(childLocation,routePosition),
                        x(childLocation, levelChild),y(childLocation, levelChild));

/*
                shape.curveTo(x(parentLocation, levelChild),y(parentLocation, levelChild),
                        x(childLocation, levelParent),y(childLocation, levelParent),
                        x(childLocation, levelChild),y(childLocation, levelChild));
*/

                shape.lineTo(x(childLocation, nodeChild),y(childLocation, nodeChild));
                first=false;
            }

            e.underlying.setRoute(shape);

        }
    }

}
