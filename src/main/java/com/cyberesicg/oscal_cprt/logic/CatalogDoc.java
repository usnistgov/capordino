package com.cyberesicg.oscal_cprt.logic;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

// @author jimlh
//
public class CatalogDoc {
    CprtFramework framework;
    String xmlContent;
    
    public CatalogDoc(String fwName){
        framework = new CprtFramework(fwName, false);
        framework.getFrameworkDataFromCPRT();
    }
    
    public void makeCatalogDocument() throws NoSuchFieldException {
        ConversionControlFile ccf = new ConversionControlFile(framework);
        ccf.loadConversionControlFile(framework.fwId);
        JsonNode fwN = ccf.getFwTemplate(framework.fwId);
        if(fwN == null){
            System.out.println("ERROR: The CPRT Framework " + 
                                framework.fwId +
                                " conversion control file was not found.");
            System.out.println("The Catalog file cannot be created."); 
            return;
        }
        if(!fwN.isArray()) return;
        for(JsonNode segmentNode: fwN) {
            xmlContent = ccf.handleTemplateNode(segmentNode);
        }
        
    }

    public void writeCatalog() {
        String path = AppUtils.gOutFolder;
        String dt =  AppUtils.getFNameDateTime(null);
        String fName = "OSCAL_Cat_" +
                       framework.fwId + "_" +
                       dt +
                       ".xml";
        String fPath = path +
                       File.separator +
                       fName;
        
        checkMinorDetails();
        
        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(fPath);
            fileWriter.write(xmlContent);
            fileWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(CatalogDoc.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        
        JOptionPane.showMessageDialog(
                    null,
                    fPath,
                    "Catalog written successfully.",
                    JOptionPane.PLAIN_MESSAGE);
    }

    private void checkMinorDetails(){
        xmlContent = xmlContent.replace("\\mailto", "/mailto");
        xmlContent = xmlContent.replace(" xmlns=\"\"", "");
        xmlContent = xmlContent.replace("&", "&#038;");
    }
    
}
