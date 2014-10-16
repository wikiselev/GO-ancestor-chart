package uk.ac.ebi.interpro.exchange.compress;

import java.io.*;

public class Format<Versions extends Enum<Versions>, CheckPoints extends Enum<CheckPoints>> {

    int checkPointNumber=0xc0ffee;

    String magic="VFDF";//versioned format data file

    private String what;
    private Versions[] versions;

    public Format(String what, Versions[] versions) {
        this.what = what;
        this.versions = versions;
    }

    public Versions getVersion(BitReader br) throws IOException {
        String key=br.readUTF8(256);
        if (!key.equals(magic)) throw new IOException("File is not a recognised file format (expected a "+what+")");
        String load=br.readUTF8(256);
        if (!load.equals(what)) throw new IOException("File is a "+load+" not a "+what);
        int version=br.readInt();
        if (version<0 || version>= versions.length) throw new IOException("Version number "+version+" not permitted");
        return versions[version];
    }
    public void writeVersion(BitWriter bw, Versions version) throws IOException {
        bw.writeUTF8(magic);
        bw.writeUTF8(what);
        bw.writeInt(version.ordinal());
    }
    public void confirmCheckPoint(BitReader br, CheckPoints checkPoint) throws IOException {
        long where = br.bitCount();
        //System.out.println("Check point confirmation "+ where +" "+checkPoint);
        int code=br.readInt();
        if (code!=(checkPointNumber ^ checkPoint.ordinal())) throw new IOException("Check point "+checkPoint+" expected at "+ where +", "+code+" not permitted");
    }
    public void writeCheckPoint(BitWriter bw, CheckPoints checkPoint) throws IOException {
        //System.out.println("Check point set "+ bw.bitCount() +" "+checkPoint);
        bw.writeInt(checkPointNumber ^ checkPoint.ordinal());

    }

}
