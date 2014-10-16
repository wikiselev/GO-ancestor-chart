package uk.ac.ebi.quickgo.web.servlets;

import uk.ac.ebi.interpro.common.http.*;
import uk.ac.ebi.quickgo.web.*;
import uk.ac.ebi.quickgo.web.graphics.*;

import javax.imageio.*;
import javax.imageio.stream.*;
import java.awt.image.*;
import java.io.IOException;
import java.io.OutputStream;

public class ImageServlet implements Dispatchable {

    public void process(Request r) throws Exception {


            String id = r.getParameter("id");
            String u = r.getParameter("u");

            ImageArchive ia = r.configuration.imageArchive;

            ImageRender ri = null;
            if (ia.instance.equals(u)) ri = ia.get(id);

            RenderedImage image;

            if (ri == null) {
                BufferedImage bi = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
                if (!ia.instance.equals(u)) {
                    // cyan indicates wrong unique identifier.
                    // Wrong server or server restarted.
                    bi.setRGB(0, 0, 0xff00c0c0);
                } else {
                    // grey indicates image not found in archive
                    // page probably too old, reload.
                    bi.setRGB(0, 0, 0xffc0c0c0);
                }
                image = bi;
            } else {
                image = ri.render();
            }
        process(r,image);
    }

        public void process(Request r,RenderedImage image) throws Exception {
            HTTPResponse page=new HTTPResponse("image/png");
            renderPNG(image, page.getOutputStream());
            r.write(page);

    }

    public static void renderPNG(RenderedImage image, OutputStream os) throws IOException {
        ImageWriter iw = ImageIO.getImageWritersByFormatName("png").next();
        ImageOutputStream ios = new MemoryCacheImageOutputStream(os);
        iw.setOutput(ios);
        iw.write(image);
    }
}
