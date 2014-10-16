package uk.ac.ebi.quickgo.web.graphics;

/**
 * General graph edge
 */
public interface Edge<N extends Node> {

    /**
     * Parent
     * @return parent
     */
    N getParent();

    /**
     * Child
     * @return child
     */
    N getChild();

}
