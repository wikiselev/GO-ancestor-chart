package uk.ac.ebi.interpro.common.database;

import java.sql.*;

public class BindVariable {


    public String toString() {
        return value.toString();
    }

    public Object value;
    public int type;

    public BindVariable(String value) {
        this.value = value;
        type= Types.VARCHAR;
    }

    public BindVariable(int value) {
        this.value = value;
        type=Types.NUMERIC;
    }

    public BindVariable(long value) {
        this.value = value;
        type=Types.NUMERIC;
    }


    public BindVariable(Object value, int type) {
        this.value = value;
        this.type = type;
    }
}
