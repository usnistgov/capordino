package gov.nist.capordino.cprt.pojo;

public class CprtRelationship {
    public String source_element_identifier;
    public String source_doc_identifier;
    public String dest_element_identifier;
    public String dest_doc_identifier;
    public String relationship_identifier;
    public String provenance_doc_identifier;

    public String getSourceGlobalIdentifier() {
        return source_doc_identifier + ":" + source_element_identifier;
    }

    public String getDestGlobalIdentifier() {
        return dest_doc_identifier + ":" + dest_element_identifier;
    }
}
