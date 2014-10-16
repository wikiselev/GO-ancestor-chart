package uk.ac.ebi.quickgo.web.servlets.annotation;

import java.io.*;

interface DataAction {
    /**
     * Process a row of annotation.
     *
     * @param row annotation data to be processed.
     * @return true:keep going, false:stop
     * @throws java.io.IOException on failure to process row
     */
    boolean act(AnnotationRow row) throws IOException;
}
