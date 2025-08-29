package gov.nist.capordino.cprt.conversion.sp_800_66;

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
public class SP80066OscalConverterTest {
    /**
     * A CPRT file that contains a subset of 800-66 content.
     */
    final static File Cprt80066Sample = new File("src/test/resources/cprt_SP800_66_2_0_0_08-28-2025.json");
    
    @TempDir(cleanup = CleanupMode.NEVER) // Change to NEVER to keep the temp directory
    static Path tempOutDirectory;

    static Path sampleOutFilePath;
    static Path Cprt80066OutFilePath;

    private static OscalBindingContext bindingContext;
    private static CprtRoot root;
    private static CprtMetadataVersion version = new CprtMetadataVersion();

    @BeforeAll
    static void initialize() throws StreamReadException, DatabindException, IOException {
        bindingContext = OscalBindingContext.instance();
        sampleOutFilePath = tempOutDirectory.resolve("cprt80066-sample_catalog.xml");
        Cprt80066OutFilePath = tempOutDirectory.resolve("cprt80066_catalog.xml");

        System.out.println("Saving output to: " + tempOutDirectory.toString());
        
        ObjectMapper mapper = new ObjectMapper();
        
        root = mapper.readValue(Cprt80066Sample, CprtRoot.class);

        version.name = "Implementing the Health Insurance Portability and Accountability Act (HIPAA) Security Rule: A Cybersecurity Resource Guide";
        version.frameworkIdentifier = "SP800_66";
        version.frameworkWebSite = "https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-66r2.ipd.pdf";
        version.frameworkVersionIdentifier = "SP800_66_2_0_0";
        version.version = "2.0.0";
        version.publicationReleaseDate = new Date();
    }

    @Test
    @Order(1)
    void testConvertSampleToOscal() throws StreamReadException, DatabindException, IOException, InvalidFrameworkIdentifier {        
        SP80066OscalConverter converter = new SP80066OscalConverter(version, root);
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

    // @Test
    // @Order(3)
    // @Tag("Online")
    // void testConvertSP80066ToOscal() throws IOException, InterruptedException, InvalidFrameworkIdentifier {
    //     CprtApiClient client = new CprtApiClient();
    //     CprtMetadataVersion version = client.getMetadata().versions.stream().filter(v -> v.frameworkVersionIdentifier.equals("SP_800_172_1_0_0")).findFirst().orElseThrow();

    //     SP80066OscalConverter converter = new SP80066OscalConverter(version);
    //     Catalog catalog = converter.buildCatalog();

    //     // Write to a file and load again to ensure the serialization and deserialization works
    //     ISerializer<Catalog> serializer = bindingContext.newSerializer(Format.XML, Catalog.class);
    //     serializer.serialize(catalog, Cprt80066OutFilePath);
    //     assertNotNull(bindingContext.loadCatalog(Cprt80066OutFilePath));
    // }

    // @Test
    // @Order(4)
    // @Tag("Online")
    // void testValidateSP80066ToOscal() throws IOException {
    //     IValidationResult results = bindingContext.validateWithConstraints(Cprt80066OutFilePath);
    //     assertTrue(results.isPassing());
    // }
}
