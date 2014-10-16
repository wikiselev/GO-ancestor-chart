package uk.ac.ebi.quickgo.web.graphics;

import java.awt.*;

/**
 * Edge with layout information
 */
public interface LayoutEdge<N extends Node> extends Edge<N> {

    /**
     * Set the shape of the path taken for the edge
     * @param route path
     */
    void setRoute(Shape route);

}
