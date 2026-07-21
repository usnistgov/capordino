package gov.nist.capordino.cprt.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nist.capordino.cprt.api.CprtApiClient;
import gov.nist.capordino.cprt.conversion.AbstractOscalConverter;
import gov.nist.capordino.cprt.conversion.InvalidFrameworkIdentifier;
import gov.nist.capordino.cprt.conversion.UnimplementedFrameworkIdentifier;
import gov.nist.capordino.cprt.conversion.ai_rmf.AIRMFCprtOscalConverter;
import gov.nist.capordino.cprt.conversion.csf.Csf20CprtOscalConverter;
import gov.nist.capordino.cprt.conversion.sp_800_171.SP800171OscalConverter;
import gov.nist.capordino.cprt.conversion.sp_800_172.SP800172OscalConverter;
import gov.nist.capordino.cprt.conversion.sp_800_172.SP800172R3OscalConverter;
import gov.nist.capordino.cprt.conversion.sp_800_218.SP800218CprtOscalConverter;
import gov.nist.capordino.cprt.conversion.sp_800_66.SP80066OscalConverter;
import gov.nist.capordino.cprt.pojo.CprtMetadataVersion;
import gov.nist.capordino.cprt.pojo.CprtRoot;
import gov.nist.secauto.metaschema.binding.io.Format;
import gov.nist.secauto.metaschema.binding.io.ISerializer;
import gov.nist.secauto.oscal.lib.OscalBindingContext;
import gov.nist.secauto.oscal.lib.model.Catalog;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "capordino", version = "Capordino 1.2", mixinStandardHelpOptions = true) 
public class Capordino implements Runnable {
    public final String CSF_2_0_0_IDENTIFIER = "CSF_2_0_0";
    public final String SP_800_171_3_0_0_IDENTIFIER = "SP_800_171_3_0_0";
    public final String SP_800_218_1_1_0_IDENTIFIER = "SP_800_218_1_1_0";
    public final String SP_800_66_2_0_0_IDENTIFIER = "SP800_66_2_0_0";
    public final String SP_800_172_1_0_0_IDENTIFIER = "SP_800_172_1_0_0";
    public final String SP_800_172_3_0_0_IDENTIFIER = "SP_800_172_3_0_0";
    public final String AI_RMF_1_0_0_IDENTIFIER = "AI_100_1_0_0";
    public final String[] IMPLEMENTED_IDENTIFIERS = {
        CSF_2_0_0_IDENTIFIER, 
        SP_800_171_3_0_0_IDENTIFIER, 
        SP_800_218_1_1_0_IDENTIFIER, 
        SP_800_66_2_0_0_IDENTIFIER, 
        SP_800_172_1_0_0_IDENTIFIER,
        SP_800_172_3_0_0_IDENTIFIER,
        AI_RMF_1_0_0_IDENTIFIER
    };

    // // File path if -f option is used
    // @Option(names = {"-f", "--file-path"}, defaultValue = "",
    //             description = "File path for framework json, if already downloaded from CPRT")
    // private String filepath;

    // Framework version identifier to build catalog
    @Parameters(paramLabel = "<framework version identifier>",
                description = "REQUIRED: framework version identifier to build catalog for\n" + 
                "Implemented: " + 
                CSF_2_0_0_IDENTIFIER + ", " + 
                SP_800_171_3_0_0_IDENTIFIER + ", " +
                SP_800_218_1_1_0_IDENTIFIER + ", " +
                SP_800_66_2_0_0_IDENTIFIER + ", " +
                SP_800_172_1_0_0_IDENTIFIER + ", " +
                SP_800_172_3_0_0_IDENTIFIER + ", " +
                AI_RMF_1_0_0_IDENTIFIER
                )
    private String framework_version_identifier;
    // CSF_2_0_0
    // SP_800_171_3_0_0
    // SP_800_218_1_1_0
    // SP800_66_2_0_0
    // SP_800_172_1_0_0
    // SP_800_172_3_0_0
    // AI_100_1_0_0

    // Output directory
    @Option(names = {"-o", "--output-directory"}, defaultValue = "./catalogs/",
                description = "Directory for capordino tool output (built catalog), default is \"./catalogs/\"")
    private String output_directory;

    @Override
    public void run() { 
        // Initialize
        // File cprtSample = new File(filepath);

        OscalBindingContext bindingContext = OscalBindingContext.instance();

        Path tempOutDirectory = FileSystems.getDefault().getPath(output_directory);

        Path outFilePath = tempOutDirectory.resolve(framework_version_identifier + "_catalog.xml");

        System.out.println("Saving output to: " + tempOutDirectory.toString());

        // ObjectMapper mapper = new ObjectMapper();
        
        //Convert CPRT to OSCAL
        CprtApiClient client = new CprtApiClient();
        CprtMetadataVersion version = new CprtMetadataVersion();

        try {
            if (Arrays.asList(IMPLEMENTED_IDENTIFIERS).contains(framework_version_identifier)) {
                // Take in framework version from CLI
                version = client.getMetadata().versions.stream().filter(v -> v.frameworkVersionIdentifier.equals(framework_version_identifier)).findFirst().orElseThrow();
    
                // Build catalog
                AbstractOscalConverter converter = null;
                if (framework_version_identifier.equals(CSF_2_0_0_IDENTIFIER)) {
                    converter = new Csf20CprtOscalConverter(version);
                }
                else if (framework_version_identifier.equals(SP_800_171_3_0_0_IDENTIFIER)) {
                    converter = new SP800171OscalConverter(version);
                }
                else if (framework_version_identifier.equals(SP_800_218_1_1_0_IDENTIFIER)) {
                    converter = new SP800218CprtOscalConverter(version);
                }
                else if (framework_version_identifier.equals(SP_800_66_2_0_0_IDENTIFIER)) {
                    converter = new SP80066OscalConverter(version);
                }
                else if (framework_version_identifier.equals(SP_800_172_1_0_0_IDENTIFIER)) {
                    converter = new SP800172OscalConverter(version);
                }
                else if (framework_version_identifier.equals(SP_800_172_3_0_0_IDENTIFIER)) {
                    converter = new SP800172R3OscalConverter(version);
                }
                else if (framework_version_identifier.equals(AI_RMF_1_0_0_IDENTIFIER)) {
                    converter = new AIRMFCprtOscalConverter(version);
                }
                else {
                    throw new UnimplementedFrameworkIdentifier(framework_version_identifier);
                }
                
                Catalog catalog = converter.buildCatalog();
    
                // Serialize and write catalog to a file
                ISerializer<Catalog> serializer = bindingContext.newSerializer(Format.XML, Catalog.class);
                serializer.serialize(catalog, outFilePath);
            }
            else {
                throw new UnimplementedFrameworkIdentifier(framework_version_identifier);
            }
        } catch (IOException | InterruptedException ie) {
            ie.printStackTrace();
        } catch (InvalidFrameworkIdentifier ifi) {
            // Additional handling of InvalidFrameworkIdentifier

            ifi.printStackTrace();
        } catch (UnimplementedFrameworkIdentifier ufi) {
            ufi.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Capordino()).execute(args);
        System.exit(exitCode);
    }
}