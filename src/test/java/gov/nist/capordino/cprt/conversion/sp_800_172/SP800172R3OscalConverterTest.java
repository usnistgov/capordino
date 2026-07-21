package gov.nist.capordino.cprt.conversion.sp_800_172;

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
public class SP800172R3OscalConverterTest {
    /**
     * A CPRT file that contains a subset of 800-172 content.
     */
    final static File Cprt800172Sample = new File("src/test/resources/cprt_json/sp_800_172/cprt_SP_800_172_3_0_0_05-20-2026.json");
    
    @TempDir(cleanup = CleanupMode.NEVER) // Change to NEVER to keep the temp directory
    static Path tempOutDirectory;

    static Path sampleOutFilePath;
    static Path Cprt800172OutFilePath;

    private static OscalBindingContext bindingContext;
    private static CprtRoot root;
    private static CprtMetadataVersion version = new CprtMetadataVersion();

    @BeforeAll
    static void initialize() throws StreamReadException, DatabindException, IOException {
        bindingContext = OscalBindingContext.instance();
        sampleOutFilePath = tempOutDirectory.resolve("cprt800172-sample_catalog.xml");
        Cprt800172OutFilePath = tempOutDirectory.resolve("cprt800172_catalog.xml");

        System.out.println("Saving output to: " + tempOutDirectory.toString());
        
        ObjectMapper mapper = new ObjectMapper();
        
        root = mapper.readValue(Cprt800172Sample, CprtRoot.class);

        version.name = "Enhanced Security Requirements for Protecting Controlled Unclassified Information";
        version.frameworkIdentifier = "SP800_172";
        version.frameworkWebSite = "https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-172.pdf";
        version.frameworkVersionIdentifier = "SP_800_172_3_0_0";
        version.version = "3.0.0";
        version.publicationReleaseDate = new Date();
    }

    @Test
    @Order(1)
    void testConvertSampleToOscal() throws StreamReadException, DatabindException, IOException, InvalidFrameworkIdentifier {        
        SP800172R3OscalConverter converter = new SP800172R3OscalConverter(version, root);
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
    void testConvertSP800172ToOscal() throws IOException, InterruptedException, InvalidFrameworkIdentifier {
        CprtApiClient client = new CprtApiClient();
        CprtMetadataVersion version = client.getMetadata().versions.stream().filter(v -> v.frameworkVersionIdentifier.equals("SP_800_172_3_0_0")).findFirst().orElseThrow();

        SP800172R3OscalConverter converter = new SP800172R3OscalConverter(version);
        Catalog catalog = converter.buildCatalog();

        // Write to a file and load again to ensure the serialization and deserialization works
        ISerializer<Catalog> serializer = bindingContext.newSerializer(Format.XML, Catalog.class);
        serializer.serialize(catalog, Cprt800172OutFilePath);
        assertNotNull(bindingContext.loadCatalog(Cprt800172OutFilePath));
    }

    @Test
    @Order(4)
    @Tag("Online")
    void testValidateSP800172ToOscal() throws IOException {
        IValidationResult results = bindingContext.validateWithConstraints(Cprt800172OutFilePath);
        assertTrue(results.isPassing());
    }
}
