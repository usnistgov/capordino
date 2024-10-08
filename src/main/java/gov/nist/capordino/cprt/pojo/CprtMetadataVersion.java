package gov.nist.capordino.cprt.pojo;

import java.util.ArrayList;
import java.util.Date;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown = true)
public class CprtMetadataVersion {
    public String name;
    public String frameworkIdentifier;
    public String frameworkWebSite;
    /**
     * This identifier is used in export requests (@see CprtApiClient.exportCprt)
     */
    public String frameworkVersionIdentifier;
    public String interfaceIdentifier;
    public String frameworkVersionName;
    public String shortName;
    public String description;
    public String note;
    public String toolTip;
    public String pocEmailAddress;
    public String frameworkVersionWebSite;
    public String version;
    public String breadcrumb;
    public boolean withdrawn;
    public String publicationStatus;
    public Date publicationReleaseDate;
    public boolean publiclyViewable;
    public boolean expandFrameworkVersion;
    public ArrayList<CprtMetadataType> types;
}
