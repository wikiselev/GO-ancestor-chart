package uk.ac.ebi.quickgo.web.graphics;

import java.util.*;

/**
 * Graph, contains the nodes, edges and utility methods
 */
public class StandardGraph<N extends Node,E extends Edge<N>> implements Graph<N,E> {

    public List<N> nodes=new ArrayList<N>();
    public List<E> edges=new ArrayList<E>();

    public List<N> getNodes() {
        return nodes;
    }
    public List<E> getEdges() {
        return edges;
    }

    public Set<N> parents(N a) {
        Set<N> results = new HashSet<N>();
        for (E e : edges) if (e.getChild() == a) results.add(e.getParent());
        return results;
    }

    public Set<N> children(N a) {
        Set<N> results = new HashSet<N>();
        for (E e : edges) if (e.getParent() == a) results.add(e.getChild());
        return results;
    }

    public E findEdge(N parent, N child) {
        for (E o : edges) if ((o.getParent() == parent) && (o.getChild() == child)) return o;
        return null;
    }

    public boolean connected(N a, N b) {
        return (findEdge(a, b) != null) || (findEdge(b, a) != null);
    }

}
