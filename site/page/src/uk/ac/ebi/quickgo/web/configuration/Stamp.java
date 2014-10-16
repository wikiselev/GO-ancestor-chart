package uk.ac.ebi.quickgo.web.configuration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import uk.ac.ebi.interpro.common.IOUtils;

public class Stamp {

    public final String version;

    public final long size; // size is 0 if not supplied in stamp file

    public Stamp(String version, long size) {
        this.version = version;
        this.size = size;
    }



    public Stamp(URL url) throws IOException {
        String content = IOUtils.readString(url);
        String[] text = content.split("\n");
        version = text[0];
        if (text.length > 1) {
            size = Long.parseLong(text[1]);
        } else {
            size=0;
        }
    }

    public void write(File stampFile) throws IOException {
        IOUtils.copy(version + "\n" + size + "\n", stampFile);
    }
}
