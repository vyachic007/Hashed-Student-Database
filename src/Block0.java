import java.io.Serializable;

public class Block0 implements Serializable {
    public static final int SIZE = 700;

    private String[] fieldNames;
    private String[] fieldTypes;
    private Bucket[] catalog;

    public Block0() {
        this.fieldNames = new String[]{"id_zachet", "id_gr", "surname", "name", "patronymic"};
        this.fieldTypes = new String[]{"int", "int", "String(30)", "String(20)", "String(30)"};
        this.catalog = new Bucket[5];

        for (int i = 0; i < catalog.length; i++) {
            catalog[i] = new Bucket();
        }
    }


    public String[] getFieldNames() {
        return fieldNames;
    }

    public String[] getFieldTypes() {
        return fieldTypes;
    }

    public Bucket[] getCatalog() {
        return catalog;
    }
}