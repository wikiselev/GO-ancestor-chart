package uk.ac.ebi.quickgo.web.graphics;

/**
 * Node with layout information
 */

public interface LayoutNode extends Node {
    

    /**
     * Get node width
     *
     * @return Width
     */
    int getWidth();

    /**
     * Get node height
     * @return height
     */
    int getHeight();

    /**
     * Set the location.
     * The layout algorithm assumes x and y will be the centre of the node.
     *
     * @param x Horizontal location
     * @param y Vertical location
     */
    void setLocation(int x,int y);
}
