package uk.ac.ebi.quickgo.web.update;

import uk.ac.ebi.interpro.exchange.compress.*;
import uk.ac.ebi.interpro.common.collections.*;
import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.quickgo.web.data.*;

import java.io.*;
import java.util.*;

class Nvl implements Output<String> {
    public Nvl(Output<String> underlying) {
        this.underlying = underlying;
    }

    private Output<String> underlying;

    public void write(String s) throws IOException {
        underlying.write(s==null?"":s);
    }

    public void close() throws Exception {
        underlying.close();
    }

    

}
