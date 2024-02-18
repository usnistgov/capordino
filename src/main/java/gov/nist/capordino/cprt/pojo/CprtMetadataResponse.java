package gov.nist.capordino.cprt.pojo;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonRootName;

/**
 * The response object returned from a CPRT metadata request.
 */
@JsonRootName(value = "response")
public class CprtMetadataResponse {
    public int requestType;
    public ArrayList<CprtMetadataVersion> versions;
}
