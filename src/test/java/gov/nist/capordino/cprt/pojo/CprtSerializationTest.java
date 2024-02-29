package gov.nist.capordino.cprt.pojo;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CprtSerializationTest {
    final File CprtSample = new File("src/test/resources/cprt_sample.json");
    
    @Test
    public void shouldDeserializeSampleCprt() throws StreamReadException, DatabindException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        CprtRoot root = mapper.readValue(CprtSample, CprtRoot.class);

        Assert.assertEquals(1, root.getDocuments().size());
        Assert.assertEquals(1, root.getRelationshipTypes().size());
        Assert.assertEquals(2, root.getElements().size());
        Assert.assertEquals(1, root.getRelationships().size());

        Assert.assertEquals(root.getDocuments().get(0).doc_identifier, "Sample");
    }
}
