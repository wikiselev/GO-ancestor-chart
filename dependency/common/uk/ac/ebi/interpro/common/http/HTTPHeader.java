package uk.ac.ebi.interpro.common.http;

public class HTTPHeader {
    public String name;
    public String value;

    public HTTPHeader(String key, String value) {
        this.name = key;
        this.value = value;
    }
}
