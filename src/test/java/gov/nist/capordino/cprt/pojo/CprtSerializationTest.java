package gov.nist.capordino.cprt.pojo;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CprtSerializationTest {
    final File CprtSample = new File("src/test/resources/sample_cprt.json");
    
    @Test
    public void shouldDeserializeSampleCprt() throws StreamReadException, DatabindException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        CprtRoot root = mapper.readValue(CprtSample, CprtRoot.class);

        Assert.assertEquals(1, root.documents.size());
        Assert.assertEquals(1, root.relationship_types.size());
        Assert.assertEquals(2, root.elements.size());
        Assert.assertEquals(1, root.relationships.size());

        Assert.assertEquals(root.documents.get(0).doc_identifier, "Sample");
    }
}
