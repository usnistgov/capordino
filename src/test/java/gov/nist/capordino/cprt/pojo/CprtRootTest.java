package gov.nist.capordino.cprt.pojo;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class CprtRootTest {
    final static File CprtSample = new File("src/test/resources/cprt_sample.json");

    static CprtRoot root;

    @BeforeAll
    static void initialize() throws StreamReadException, DatabindException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        root = mapper.readValue(CprtSample, CprtRoot.class);
    }

    @Test
    void testGetElementById() {
        Assert.assertNotNull(root.getElementById("Sample:SampleElement"));

        Assert.assertNull(root.getElementById("Invalid:NotAnElement"));
    }

    @Test
    void testGetRelationshipsBySourceElementId() {
        Assert.assertEquals(1, root.getRelationshipsBySourceElementId("Sample:SampleElement").size());

        Assert.assertEquals(0, root.getRelationshipsBySourceElementId("Sample:SampleElement2").size());

        Assert.assertEquals(0, root.getRelationshipsBySourceElementId("Invalid:NotAnElement").size());
    }

    @Test
    void testGetRelationshipsByDestinationElementId() {
        Assert.assertEquals(0, root.getRelationshipsByDestinationElementId("Sample:SampleElement").size());

        Assert.assertEquals(1, root.getRelationshipsByDestinationElementId("Sample:SampleElement2").size());

        Assert.assertEquals(0, root.getRelationshipsByDestinationElementId("Invalid:NotAnElement").size());
    }
}
