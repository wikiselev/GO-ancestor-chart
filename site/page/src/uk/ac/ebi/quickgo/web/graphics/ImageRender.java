package uk.ac.ebi.quickgo.web.graphics;

import java.awt.image.*;
import java.awt.*;

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
