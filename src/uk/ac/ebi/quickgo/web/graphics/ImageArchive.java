package uk.ac.ebi.quickgo.web.graphics;

import uk.ac.ebi.interpro.common.*;
import uk.ac.ebi.interpro.common.collections.*;


public class ImageArchive {


    WeakValueMap<String,ImageRender> data=new WeakValueMap<String, ImageRender>(Interval.seconds(300));

    TimedStorage<ImageRender> imageBucket=new TimedStorage<ImageRender>(Interval.seconds(300));

    public String instance;
    String imageServletAddress="IS";

    int sequence;
    public String store(ImageRender image) {
        String id;
        synchronized(this) {
            id= String.valueOf(sequence);
            data.put(id,image);
            imageBucket.add(image);
            sequence++;
        }
        String src = imageServletAddress + "?u=" + instance + "&id=" + id;
        image.src=src;
        return src;
    }

    public synchronized ImageRender get(String id) {
        return data.get(id);
    }


    public String status() {

        synchronized(this) {
            imageBucket.vacuum();
            data.vacuum();
            return data.values().size()+" available "+imageBucket.size()+" guaranteed";
        }

    }

    public void configure(String uniqueID) {
       this.instance = uniqueID;
    }
}
