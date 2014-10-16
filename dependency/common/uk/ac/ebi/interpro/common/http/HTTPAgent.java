package uk.ac.ebi.interpro.common.http;

import java.io.*;

public interface HTTPAgent {
    HTTPResponse send(HTTPRequest source) throws IOException;
}
