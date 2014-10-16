package uk.ac.ebi.interpro.webutil.tools;

import uk.ac.ebi.interpro.common.performance.*;

import javax.servlet.http.*;
import java.io.*;
import java.nio.charset.*;

@Deprecated
public interface Response {

    void write(HttpServletResponse response) throws IOException;

    int size();

}
