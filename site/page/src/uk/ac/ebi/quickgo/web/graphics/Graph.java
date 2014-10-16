package uk.ac.ebi.quickgo.web.graphics;

import java.util.*;

public interface Graph<N extends Node,E extends Edge<N>> {
    Collection<N> getNodes();
    Collection<E> getEdges();
}
