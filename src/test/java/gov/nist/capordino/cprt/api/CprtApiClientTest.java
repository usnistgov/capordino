package gov.nist.capordino.cprt.api;

import java.io.IOException;

import org.junit.Assert;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import gov.nist.capordino.cprt.pojo.CprtMetadataResponse;

@Tag("Online")
public class CprtApiClientTest {
    CprtApiClient client = new CprtApiClient();

    @Test
    void testGetMetadata() throws IOException, InterruptedException {
        CprtMetadataResponse metadata = client.getMetadata();
        Assert.assertNotNull(metadata);

        // CPRT's API is not well documented, but the requestType for metadata is 2
        Assert.assertEquals(2, metadata.requestType);
    }

    @Test
    void testExportCprt() throws IOException, InterruptedException {
        var export = client.exportCprt("csf_2_0_0");
        Assert.assertNotNull(export);

        // CPRT's API is not well documented, but the requestType for exports is 4
        Assert.assertEquals(4, export.requestType);
    }
}
