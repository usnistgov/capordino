package gov.nist.capordino.cprt.conversion.sp_800_218;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nist.capordino.cprt.api.CprtApiClient;
import gov.nist.capordino.cprt.conversion.InvalidFrameworkIdentifier;
import gov.nist.capordino.cprt.pojo.CprtMetadataVersion;
import gov.nist.capordino.cprt.pojo.CprtRoot;
import gov.nist.secauto.metaschema.binding.io.Format;
import gov.nist.secauto.metaschema.binding.io.ISerializer;
import gov.nist.secauto.metaschema.model.common.validation.IValidationResult;
import gov.nist.secauto.oscal.lib.OscalBindingContext;
import gov.nist.secauto.oscal.lib.model.Catalog;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SP800218OscalConverterTest {
    /**
     * A CPRT file that contains a subset of 800-218 content.
     */
    final static File Cprt800218Sample = new File("src/test/resources/cprt_SP_800_218_1_1_0.json");
    
    @TempDir(cleanup = CleanupMode.NEVER) // Change to NEVER to keep the temp directory
    static Path tempOutDirectory;

    static Path sampleOutFilePath;
    static Path Cprt800218OutFilePath;

    private static OscalBindingContext bindingContext;
    private static CprtRoot root;
    private static CprtMetadataVersion version = new CprtMetadataVersion();

    @BeforeAll
    static void initialize() throws StreamReadException, DatabindException, IOException {
        bindingContext = OscalBindingContext.instance();
        sampleOutFilePath = tempOutDirectory.resolve("cprt800218-sample_catalog.xml");
        Cprt800218OutFilePath = tempOutDirectory.resolve("cprt800218_catalog.xml");

        System.out.println("Saving output to: " + tempOutDirectory.toString());
        
        ObjectMapper mapper = new ObjectMapper();
        
        root = mapper.readValue(Cprt800218Sample, CprtRoot.class);

        version.name = "Secure Software Development Framework (SSDF): Recommendations for Mitigating the Risk of Software Vulnerabilities";
        version.frameworkIdentifier = "SSDF";
        version.frameworkWebSite = "https://csrc.nist.gov/publications/detail/sp/800-218/final";
        version.frameworkVersionIdentifier = "SP_800_218_1_1_0";
        version.version = "Version 1.1";
        version.publicationReleaseDate = new Date();
    }

    @Test
    @Order(1)
    void testConvertSampleToOscal() throws StreamReadException, DatabindException, IOException, InvalidFrameworkIdentifier {        
        SP800218CprtOscalConverter converter = new SP800218CprtOscalConverter(version, root);
        // System.out.println(root);
        // System.out.println(root.getElements());
        Catalog catalog = converter.buildCatalog();

        // Write to a file and load again to ensure the serialization and deserialization works
        ISerializer<Catalog> serializer = bindingContext.newSerializer(Format.XML, Catalog.class);
        serializer.serialize(catalog, sampleOutFilePath);
        assertNotNull(bindingContext.loadCatalog(sampleOutFilePath));
    }

    @Test
    @Order(2)
    void testValidateSampleOscal() throws IOException {
        IValidationResult results = bindingContext.validateWithConstraints(sampleOutFilePath);
        assertTrue(results.isPassing());
    }

    @Test
    @Order(3)
    @Tag("Online")
    void testConvertSP800218ToOscal() throws IOException, InterruptedException, InvalidFrameworkIdentifier {
        CprtApiClient client = new CprtApiClient();
        CprtMetadataVersion version = client.getMetadata().versions.stream().filter(v -> v.frameworkVersionIdentifier.equals("SP_800_218_1_1_0")).findFirst().orElseThrow();

        SP800218CprtOscalConverter converter = new SP800218CprtOscalConverter(version);
        Catalog catalog = converter.buildCatalog();

        // Write to a file and load again to ensure the serialization and deserialization works
        ISerializer<Catalog> serializer = bindingContext.newSerializer(Format.XML, Catalog.class);
        serializer.serialize(catalog, Cprt800218OutFilePath);
        assertNotNull(bindingContext.loadCatalog(Cprt800218OutFilePath));
    }

    @Test
    @Order(4)
    @Tag("Online")
    void testValidateSP800218ToOscal() throws IOException {
        IValidationResult results = bindingContext.validateWithConstraints(Cprt800218OutFilePath);
        assertTrue(results.isPassing());
    }
}
