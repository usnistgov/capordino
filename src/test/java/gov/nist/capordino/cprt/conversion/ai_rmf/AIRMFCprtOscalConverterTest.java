package gov.nist.capordino.cprt.conversion.ai_rmf;

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
public class AIRMFCprtOscalConverterTest {

    final static File AIRMFSample = new File("src/test/resources/cprt_json/ai_rmf/cprt_AI_100_1_0_0_05-21-2026.json");
    
    @TempDir(cleanup = CleanupMode.NEVER) // Change to NEVER to keep the temp directory
    static Path tempOutDirectory;

    static Path sampleOutFilePath;
    static Path AIRMFOutFilePath;

    private static OscalBindingContext bindingContext;
    private static CprtRoot root;
    private static CprtMetadataVersion version = new CprtMetadataVersion();

    @BeforeAll
    static void initialize() throws StreamReadException, DatabindException, IOException {
        bindingContext = OscalBindingContext.instance();
        sampleOutFilePath = tempOutDirectory.resolve("airmf-sample_catalog.xml");
        AIRMFOutFilePath = tempOutDirectory.resolve("airmf_catalog.xml");

        System.out.println("Saving output to: " + tempOutDirectory.toString());
        
        ObjectMapper mapper = new ObjectMapper();
        
        root = mapper.readValue(AIRMFSample, CprtRoot.class);

        version.name = "Artificial Intelligence Risk Management Framework";
        version.frameworkIdentifier = "AI_100";
        version.frameworkWebSite = "https://www.nist.gov/itl/ai-risk-management-framework";
        version.frameworkVersionIdentifier = "AI_100_1_0_0";
        version.version = "1.1.0";
        version.publicationReleaseDate = new Date();
    }

    @Test
    @Order(1)
    void testConvertSampleToOscal() throws StreamReadException, DatabindException, IOException, InvalidFrameworkIdentifier {        
        AIRMFCprtOscalConverter converter = new AIRMFCprtOscalConverter(version, root);
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
    void testConvertAIRMFToOscal() throws IOException, InterruptedException, InvalidFrameworkIdentifier {
        CprtApiClient client = new CprtApiClient();
        CprtMetadataVersion version = client.getMetadata().versions.stream().filter(v -> v.frameworkVersionIdentifier.equals("AI_100_1_0_0")).findFirst().orElseThrow();

        AIRMFCprtOscalConverter converter = new AIRMFCprtOscalConverter(version);
        Catalog catalog = converter.buildCatalog();

        // Write to a file and load again to ensure the serialization and deserialization works
        ISerializer<Catalog> serializer = bindingContext.newSerializer(Format.XML, Catalog.class);
        serializer.serialize(catalog, AIRMFOutFilePath);
        assertNotNull(bindingContext.loadCatalog(AIRMFOutFilePath));
    }

    @Test
    @Order(4)
    @Tag("Online")
    void testValidateAIRMFToOscal() throws IOException {
        IValidationResult results = bindingContext.validateWithConstraints(AIRMFOutFilePath);
        assertTrue(results.isPassing());
    }
}
