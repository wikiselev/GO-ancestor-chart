package uk.ac.ebi.interpro.jxbp2;

class SkipAction implements BindingAction {

    public Key getKey() {
        return null;
    }

    public void execute(Object object, ObjectProcessor processor) throws ProcessingException {
        return;
    }
}
