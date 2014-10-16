package uk.ac.ebi.interpro.jxbp2;

import org.w3c.dom.*;

public interface Rule {
    BindingAction test(Node node);
    BindingAction test(Node node,String in,int[] find);

}
