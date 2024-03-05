package gov.nist.capordino.cprt.cli;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Date;

import com.cyberesicg.oscal_cprt.gui.MainGuiFrame;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nist.capordino.cprt.conversion.csf20.Csf20CprtOscalConverter;
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

@Command(name = "capordino", version = "Capordino 1.0", mixinStandardHelpOptions = true,
    subcommands = { CapordinoGUI.class }
    ) 
public class Capordino implements Runnable { 
    // Specify source or target
    // @Option(names = { "-tgt", "--target" }, description = "Target framework", required = true)
    // private boolean target;
    
    // @Option(names = { "-src", "--source" }, description = "Source framework", required = true)
    // private boolean source;

    @Parameters(paramLabel = "<file path>", 
               description = "File path for framework json")
    private String filepath; // = "src/test/resources/cprt_csf20_sample.json";

    // Framework version identifier to build catalog
    @Parameters(paramLabel = "<framework version identifier>")
    private String framework_version_identifier; // = "CSF_2_0_0";

    @Override
    public void run() { 
        // Csf20CprtOscalConverterTest.java

        // Initialize
        File csfCprtSample = new File(filepath);

        OscalBindingContext bindingContext = OscalBindingContext.instance();

        String tempOutDirectoryString = "src/test/output";
        Path tempOutDirectory = FileSystems.getDefault().getPath(tempOutDirectoryString);

        Path sampleOutFilePath = tempOutDirectory.resolve("csf20-sample_catalog.xml");
        Path csf20OutFilePath = tempOutDirectory.resolve("csf20_catalog.xml");

        System.out.println("Saving output to: " + tempOutDirectory.toString());

        ObjectMapper mapper = new ObjectMapper();
        CprtRoot root = mapper.readValue(csfCprtSample, CprtRoot.class);

        // Take in framework version from CLI
        CprtMetadataVersion version = new CprtMetadataVersion();
        version.frameworkVersionIdentifier = framework_version_identifier;

        // Convert CSF 20 sample to OSCAL
        Csf20CprtOscalConverter converter = new Csf20CprtOscalConverter(version, root);
        Catalog catalog = converter.buildCatalog();
        ISerializer<Catalog> serializer = bindingContext.newSerializer(Format.XML, Catalog.class);
        serializer.serialize(catalog, sampleOutFilePath);
        bindingContext.loadCatalog(sampleOutFilePath);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Capordino()).execute(args); 
        System.exit(exitCode); 
    }
}



@Command(name = "gui",
  description = "Runs capordino GUI instead of CLI") 
class CapordinoGUI implements Runnable {

    @Override
    public void run() {
        // Run MainGuiFrame.java
        MainGuiFrame.main(null);
    }
}
