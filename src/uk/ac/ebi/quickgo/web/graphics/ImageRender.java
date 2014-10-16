package uk.ac.ebi.quickgo.web.graphics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

public abstract class ImageRender {
    public String src;



    public RenderedImage render() {

        BufferedImage image = prepare();
        final Graphics2D g2 = image.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Color.white);

        g2.fillRect(0, 0, width, height);

        g2.setColor(Color.black);

        render(g2);

        return image;
    }


    public SVGGraphics2D renderSvg() {

      // Get a DOMImplementation.
      DOMImplementation domImpl =
        GenericDOMImplementation.getDOMImplementation();

      // Create an instance of org.w3c.dom.Document.
      String svgNS = "http://www.w3.org/2000/svg";
      Document document = domImpl.createDocument(svgNS, "svg", null);

      // Create an instance of the SVG Generator.
      final SVGGraphics2D g2 = new SVGGraphics2D(document);
      

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Color.white);

        g2.fillRect(0, 0, width, height);

        g2.setColor(Color.black);

        render(g2);

        return g2;
    }

    protected void render(Graphics2D g2) {}


    public int width;
    public int height;

    protected BufferedImage prepare() {
        return new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
    }


    protected ImageRender(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
