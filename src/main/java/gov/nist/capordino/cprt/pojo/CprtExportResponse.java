package gov.nist.capordino.cprt.pojo;

import com.fasterxml.jackson.annotation.JsonRootName;

/**
 * The response object returned from a CPRT export request.
 */
@JsonRootName(value = "response")
public class CprtExportResponse {
    public int requestType;
    public CprtRoot elements;
}
