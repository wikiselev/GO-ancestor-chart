package uk.ac.ebi.interpro.common;

import java.lang.ref.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: dbinns
 * Date: 30-Sep-2005
 * Time: 16:21:30
 * To change this template use File | Settings | File Templates.
 */
public class ExpiringReference {

    interface Destroyer {
        void destroy(Object o);
    }

    Object item;

    Destroyer destroyer;

    TimerTask tt=new TimerTask(){
        public void run() {
            if (destroyer!=null) destroyer.destroy(item);
            item=null;
        }
    };



    public ExpiringReference(Object item) {
        this.item = item;
    }

    public ExpiringReference(Object item, Destroyer destroyer) {
        this.item = item;
        this.destroyer = destroyer;
    }


}
