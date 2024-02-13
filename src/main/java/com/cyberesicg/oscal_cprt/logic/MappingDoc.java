package com.cyberesicg.oscal_cprt.logic;

import com.fasterxml.jackson.databind.JsonNode;
import java.awt.Dialog;
import java.io.BufferedWriter;
import java.util.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

public class MappingDoc {
    public CprtFramework fwObj;
    public CprtFramework tgtFwObj;
    public ArrayList<CprtFramework> targets;
    public String srcFw;
    public String tgtFw;
    public String srcCatFName;
    public String srcCatUuid;
    public String tgtCatFName;
    public String tgtCatUuid;
    public UUID uuid;
    public boolean bUpdateCache;
    
    public MappingDoc(String srcFw_, String tgtFw_) {
        tgtFw = tgtFw_;
        srcFw = srcFw_;
        srcCatFName = "";
        srcCatUuid = "";
        tgtCatFName = "";
        tgtCatUuid = "";
        uuid = UUID.randomUUID();
        bUpdateCache = false;
        getCatalogUuids();
    }

    private void getCatalogUuids() {
        String lastSrcTime = null;
        String newestSrcFile = null;
        String lastTgtTime = null;
        String newestTgtFile = null;
        String catPath = AppUtils.gOutFolder;
        File dir = new File(catPath);
        File[] files = dir.listFiles();
        if(files != null) {
            for (File f: files) {
                if(f.isFile()){
                    String fName = f.getName();
                    String fwName = fName.substring(0, fName.length() - 17);
                    String datePart = fName.substring(fName.length()-17);
                    
                    //Check source cat file
                    String srcCheckStr = "OSCAL_Cat_" + srcFw;
                    if(fwName.equals(srcCheckStr)) {
                        if((lastSrcTime == null) || (lastSrcTime.compareTo(datePart) < 0)) {
                            lastSrcTime = datePart;
                            newestSrcFile = fName;
                        }
                    }
                    
                    //Check target cat file
                    String tgtCheckStr = "OSCAL_Cat_" + tgtFw;
                    if(fwName.equals(tgtCheckStr)) {
                        if((lastTgtTime == null) || (lastTgtTime.compareTo(datePart) < 0)) {
                            lastTgtTime = datePart;
                            newestTgtFile = fName;
                        }
                    }
                }
            }
        }

        if(newestSrcFile == null){
            JDialog d = new JDialog((Dialog)null);
            d.setVisible(false);
            JOptionPane.showMessageDialog(
                    null,
                    "No source framework catalog was found to generate the mapping from.",
                    "Please use the tool to generate a source catalog.",
                    JOptionPane.PLAIN_MESSAGE);
            return;
        }
        srcCatFName = newestSrcFile;

        if(newestTgtFile == null){
            JDialog d = new JDialog((Dialog)null);
            d.setVisible(false);
            JOptionPane.showMessageDialog(
                    null,
                    "No target framework catalog was found to generate the mapping from.",
                    "Please use the tool to generate a target catalog.",
                    JOptionPane.PLAIN_MESSAGE);
            return;
        }
        tgtCatFName = newestTgtFile;

        
        Path path = Path.of(AppUtils.gOutFolder + "\\" + newestSrcFile);
        String content = null;
        try {
            content = Files.readString(path,Charset.defaultCharset());
        } catch (IOException ex) {
            Logger.getLogger(MappingDoc.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if(content == null) return;
        int i = content.indexOf("<catalog", 0);
        i = content.indexOf("uuid=\"", i);
        srcCatUuid = content.substring(i+6, i+42);

        path = Path.of(AppUtils.gOutFolder + "\\" + newestTgtFile);
        content = null;
        try {
            content = Files.readString(path,Charset.defaultCharset());
        } catch (IOException ex) {
            Logger.getLogger(MappingDoc.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if(content == null) return;
        i = content.indexOf("<catalog", 0);
        i = content.indexOf("uuid=\"", i);
        tgtCatUuid = content.substring(i+6, i+42);

    }
    
    public void populate() {
        fwObj = new CprtFramework(srcFw, bUpdateCache);
        fwObj.tgtFwId = tgtFw;
        fwObj.getFrameworkOlirDataFromCPRT(this);
        fwObj.fwId = tgtFw;
        fwObj.tgtFwId = srcFw;
        fwObj.getFrameworkOlirDataFromCPRT(this);
        
        FwNameResolver.showUnresolvedNames();
    }

    
    public void writeMappingsFile() throws NoSuchFieldException{

        if(fwObj.nodes.isEmpty()){
            String fwsStr = "Source: " + fwObj.fwDisplayName + "\r\n";
            fwsStr = fwsStr + "Target: " + FwNameResolver.resolveDisplayName(fwObj.fwId);
            JOptionPane.showMessageDialog(
                   null,
                   fwsStr,
                   "No OLIRs for these frameworks.",
                   JOptionPane.PLAIN_MESSAGE);
            return;
        }

        String path = AppUtils.gOutFolder;
        String dt =  AppUtils.getFNameDateTime(null);
        String fPath = path +
                       File.separator +
                       "OSCAL_Mapping_" +
                       tgtFw + "-" +
                       srcFw + "_" +
                       dt +
                       ".xml";

        ConversionControlFile srcCcf = new ConversionControlFile(fwObj);
        srcCcf.loadConversionMappingFile();
        
        JsonNode srcMcfN = srcCcf.cfNode.get("oscalSegments");
        if(srcMcfN == null){
            System.out.println("ERROR: There was a problem loading the source CPRT Mapping conversion control file.");
            System.out.println("The Mapping file cannot be created."); 
            return;
        }

        String xmlStr = "";
        for(JsonNode segmentNode: srcMcfN) {
            xmlStr = xmlStr + srcCcf.handleTemplateNode(segmentNode);
        }
        
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(fPath));
            writer.write(xmlStr);
            writer.close();        
        } catch (IOException ex) {
            Logger.getLogger(MappingDoc.class.getName()).log(Level.SEVERE, null, ex);
        }

        JOptionPane.showMessageDialog(
                null,
                fPath,
                "Successfully wrote mapping file.",
                JOptionPane.PLAIN_MESSAGE);
        
    }    
}
