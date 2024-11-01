package gov.nist.capordino.cprt.conversion.cprt_800_171;

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
import gov.nist.capordino.cprt.conversion.cprt_800_171.Cprt800171OscalConverter;
import gov.nist.capordino.cprt.conversion.csf20.Csf20CprtOscalConverter;
import gov.nist.capordino.cprt.pojo.CprtMetadataVersion;
import gov.nist.capordino.cprt.pojo.CprtRoot;
import gov.nist.secauto.metaschema.binding.io.Format;
import gov.nist.secauto.metaschema.binding.io.ISerializer;
import gov.nist.secauto.metaschema.model.common.validation.IValidationResult;
import gov.nist.secauto.oscal.lib.OscalBindingContext;
import gov.nist.secauto.oscal.lib.model.Catalog;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Cprt800171OscalConverterTest {
    /**
     * A CPRT file that contains a subset of 800-171 content.
     */
    final static File Cprt800171Sample = new File("src/test/resources/cprt_800-171r3.json");
    
    @TempDir(cleanup = CleanupMode.NEVER) // Change to NEVER to keep the temp directory
    static Path tempOutDirectory;

    static Path sampleOutFilePath;
    static Path Cprt800171OutFilePath;

    private static OscalBindingContext bindingContext;
    private static CprtRoot root;
    private static CprtMetadataVersion version = new CprtMetadataVersion();

    @BeforeAll
    static void initialize() throws StreamReadException, DatabindException, IOException {
        bindingContext = OscalBindingContext.instance();
        sampleOutFilePath = tempOutDirectory.resolve("cprt800171-sample_catalog.xml");
        Cprt800171OutFilePath = tempOutDirectory.resolve("cprt800171_catalog.xml");

        System.out.println("Saving output to: " + tempOutDirectory.toString());
        
        ObjectMapper mapper = new ObjectMapper();
        
        root = mapper.readValue(Cprt800171Sample, CprtRoot.class);

        version.name = "Protecting Controlled Unclassified Information in Nonfederal Systems and Organizations";
        version.frameworkIdentifier = "SP_800_171_3_0_0";
        version.frameworkWebSite = "https://csrc.nist.gov/pubs/sp/800/171/r3/final";
        version.frameworkVersionIdentifier = "SP_800_171_3_0_0";
        // version.interfaceIdentifier = "CSF_2";
        // version.frameworkVersionName = "The NIST Cybersecurity Framework 2.0 Draft (SAMPLE)";
        // version.shortName = "Cybersecurity Framework v2.0 SAMPLE";
        version.version = "Version 3.0.0";
        version.publicationReleaseDate = new Date();
    }

    @Test
    @Order(1)
    void testConvertSampleToOscal() throws StreamReadException, DatabindException, IOException, InvalidFrameworkIdentifier {        
        Cprt800171OscalConverter converter = new Cprt800171OscalConverter(version, root);
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
    // void testConvertCsf20ToOscal() throws IOException, InterruptedException, InvalidFrameworkIdentifier {
    //     CprtApiClient client = new CprtApiClient();
    //     CprtMetadataVersion version = client.getMetadata().versions.stream().filter(v -> v.frameworkVersionIdentifier.equals("CSF_2_0_0")).findFirst().orElseThrow();

    //     Csf20CprtOscalConverter converter = new Csf20CprtOscalConverter(version);
    //     Catalog catalog = converter.buildCatalog();

    //     // Write to a file and load again to ensure the serialization and deserialization works
    //     ISerializer<Catalog> serializer = bindingContext.newSerializer(Format.XML, Catalog.class);
    //     serializer.serialize(catalog, Cprt800171OutFilePath);
    //     assertNotNull(bindingContext.loadCatalog(Cprt800171OutFilePath));
    // }

    // @Test
    // @Order(4)
    // @Tag("Online")
    // void testValidateCsf20Oscal() throws IOException {
    //     IValidationResult results = bindingContext.validateWithConstraints(Cprt800171OutFilePath);
    //     assertTrue(results.isPassing());
    // }
}
