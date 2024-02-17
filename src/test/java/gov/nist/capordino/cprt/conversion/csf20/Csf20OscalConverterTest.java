package gov.nist.capordino.cprt.conversion.csf20;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nist.capordino.cprt.pojo.CprtRoot;
import gov.nist.secauto.metaschema.binding.io.Format;
import gov.nist.secauto.metaschema.binding.io.ISerializer;
import gov.nist.secauto.oscal.lib.OscalBindingContext;
import gov.nist.secauto.oscal.lib.model.Catalog;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Csf20OscalConverterTest {
    private static OscalBindingContext bindingContext;

    @BeforeAll
    static void initialize() {
        bindingContext = OscalBindingContext.instance();
    }

    /**
     * A CPRT file that contains a subset of CSF 2.0 content.
     */
    final File CsfCprtSample = new File("src/test/resources/cprt_csf20_sample.json");

    @Test
    @Order(1)
    void testSmokeConvertToOscal(@TempDir(cleanup = CleanupMode.DEFAULT) Path tempDir) throws StreamReadException, DatabindException, IOException {
        // Change `cleanup` to `CleanupMode.NEVER` to keep the temp directory after the test
        
        ObjectMapper mapper = new ObjectMapper();
        CprtRoot root = mapper.readValue(CsfCprtSample, CprtRoot.class);

        Csf20OscalConverter converter = new Csf20OscalConverter(root);
        Catalog catalog = converter.toOscal();

        Path out = tempDir.resolve("csf20-sample_catalog.xml");

        ISerializer<Catalog> serializer = bindingContext.newSerializer(Format.XML, Catalog.class);
        serializer.serialize(catalog, out);
        assertNotNull(bindingContext.loadCatalog(out));
    }
}
