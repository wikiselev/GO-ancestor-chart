package uk.ac.ebi.interpro.exchange.compress;

import java.io.*;

public class Type {
    public String name;
    public int cardinality;

    public Type(String name, int cardinality) {
        this.name = name;
        this.cardinality = cardinality;
    }

    public Type(BitReader br) throws IOException {
        name=br.readUTF8(256);
        cardinality=br.readInt();
    }
    public void write(BitWriter bw) throws IOException {
        bw.writeUTF8(name);
        bw.writeInt(cardinality);
    }

    public String toString() {
        return name+"("+cardinality+")";
    }
}
