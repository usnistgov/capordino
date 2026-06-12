package gov.nist.capordino.cprt.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CprtMetadataType {
    public String elementTypeIdentifier;
    public String name;
    public String namePlural;
    public String description;
    public String elementTypeAbbreviation;
    public int level;
    public boolean root;
    public boolean drilldown;
    public boolean detail;
    public boolean canRemove;
    public boolean displayParentElementTitle;
}
