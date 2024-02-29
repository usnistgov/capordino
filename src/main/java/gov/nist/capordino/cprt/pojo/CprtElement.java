package gov.nist.capordino.cprt.pojo;

public class CprtElement {
    public String element_type;
    public String element_identifier;
    public String title;
    public String text;
    public String doc_identifier;

    public String getGlobalIdentifier() {
        return doc_identifier + ":" + element_identifier;
    }
}
