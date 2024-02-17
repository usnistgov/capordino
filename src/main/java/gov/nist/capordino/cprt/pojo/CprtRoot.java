package gov.nist.capordino.cprt.pojo;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CprtRoot {
    public ArrayList<CprtDocument> documents;
    public ArrayList<CprtRelationshipType> relationship_types;
    public ArrayList<CprtElement> elements;
    public ArrayList<CprtRelationship> relationships;
}
