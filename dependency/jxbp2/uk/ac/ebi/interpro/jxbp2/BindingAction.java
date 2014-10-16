package uk.ac.ebi.interpro.jxbp2;

public interface BindingAction {
    Key getKey();
    void execute(Object object, ObjectProcessor processor) throws ProcessingException;


}
