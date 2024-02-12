package com.cyberesicg.oscal_cprt.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;



// @author jimlh
//
public class ConversionControlFile {
    public JsonNode cfNode;
    public CprtFramework fw;
    
    public ConversionControlFile(CprtFramework fw_) {
        fw = fw_;
    }

    public void loadConversionControlFile(String fwId){
        String cfPath = "cprtJson/catControlFile_" + fwId + ".json";
        InputStream is = getClass().getClassLoader().getResourceAsStream(cfPath);
        ObjectMapper mapper = new ObjectMapper();
        try {
            cfNode = mapper.readTree(is);
        } catch (IOException ex) {
            Logger.getLogger(ConversionControlFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void loadConversionMappingFile(){
        String cfPath = "cprtJson/mappingControlFile.json";
        InputStream is = getClass().getClassLoader().getResourceAsStream(cfPath);
        ObjectMapper mapper = new ObjectMapper();
        try {
            cfNode = mapper.readTree(is);
        } catch (IOException ex) {
            Logger.getLogger(ConversionControlFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public JsonNode getFwTemplate(String fwId) {
        JsonNode fwsN = cfNode.get("oscalSegments");
        String fwIdStr = cfNode.get("id").asText();
        if(fwId.equals(fwIdStr)) return fwsN;
        return null;
    }

    public String handleTemplateNode(JsonNode segment) throws NoSuchFieldException {
        if(segment == null) return null;
        String xmlStr = makeXmlFromNode(segment, null);
        return xmlStr;
    }

    private String makeXmlFromNode(JsonNode templateNode, CprtNode cn) throws NoSuchFieldException {
        String outStr = "";
        ArrayNode map = (ArrayNode)templateNode.get("segmentMap");
        for(JsonNode mapNode: map)
        {
            String mapStr = mapNode.asText();
            if(mapStr.charAt(0)=='~'){
                String retStr = getValForMapVar(mapStr, templateNode, cn);
                if(retStr != null) {
                    outStr = outStr + retStr;
                }
                continue;
            }
            
            outStr = outStr + mapStr;
        }
        return outStr;
    }

    private String getValForMapVar(String mapVarName, JsonNode templateNode, CprtNode cn) throws NoSuchFieldException{
        JsonNode varNode = templateNode.get(mapVarName);
        String varType = varNode.get(0).asText();
        String val = null;
        switch(varType){
            case "cprtElement":
                val = cn.getInstanceVar(varNode.get(1).asText());
                break;
            case "cprtElementNoSpaces":
                val = cn.getInstanceVar(varNode.get(1).asText());
                val = val.replace(" ", "_");
                break;
            case "cprtElementNoParenths":
                val = cn.getInstanceVar(varNode.get(1).asText());
                val = val.replace("(", "_");
                val = val.replace(")", "_");
                break;
            case "cprtElementDefaultIfEmpty":
                val = cn.getInstanceVar(varNode.get(1).asText());
                if((val == null) || val.isEmpty()){
                    val = varNode.get(2).asText();
                }
                break;
            case "cprtOscalLookup":
                val = getCprtOscalLookup(varNode.get(1), cn);
                break;
            case "cprtOscalLookupProp":
                val = getCprtOscalLookupProp(varNode.get(1).asText(),
                                                varNode.get(2).asText(),
                                                varNode.get(3).asText(),
                                                varNode.get(4).asText());
                break;
            case "cprtOscalList":
                val = getCprtOscalList(varNode.get(1), cn);
                break;
            case "cprtRootOscalList":
                val = getCprtRootOscalList(varNode.get(1));
                break;
            case "cprtElementReqType":
                val = cn.instanceVars.get("elementIdentifier");
                break;
            case "cprtElementReqDiscussion":
                val = cn.instanceVars.get("text");
                break;
            case "oscalId":
                val = cn.instanceVars.get(varNode.get(1).asText());
                val = val + varNode.get(2).asText();
                break;
            case "newUUID":
                UUID uuid = UUID.randomUUID();
                val = uuid.toString();
                String refName = varNode.get(1).asText();
                fw.uuidRefs.put(refName, val);
                break;
            case "getUUID":
                String uuidName = varNode.get(1).asText();
                val = fw.uuidRefs.get(uuidName);
                break;
            case "oscalDateTime":
                val = AppUtils.getDateTime(null);
                break;
            case "fwDisplayName":
                val = fw.fwDisplayName;
                break;
            case "withdrawnControl":
                int numSpaces = varNode.get(1).asInt();
                val = makeWithdrawnControl(cn, numSpaces);
                break;
            case "ifNotEmpty":
                val = cn.instanceVars.get(varNode.get(1).asText());
                if((val != null) && (!val.equals(""))) val = varNode.get(2).asText();
                break;
        }
        
        return val;
    }

    //Returns null if the control is not withdrawn
    //Or 2 lines for the ouutput if it is withdrawn
    private String makeWithdrawnControl(CprtNode cn, int leadingSpaces){
        String wr = cn.instanceVars.get("withdrawReason");
        String outStr = "";
        if(wr != null){
            wr = wr.trim();
            for(int i = 0;i<leadingSpaces; i++){
                outStr = outStr + " ";
            }
            outStr = outStr + "<prop name=\"status\" value=\"withdrawn\"/>\r\n";
            for(int i = 0;i<leadingSpaces; i++){
                outStr = outStr + " ";
            }
            outStr = outStr + "<prop name=\"marking\" class=\"reason\" value=\"" + wr + "\"/>\r\n";
            return outStr;
        }

        return null;
    }
    
    private String getCprtOscalLookup(JsonNode jn, CprtNode cn) throws NoSuchFieldException{
        String cprtElementType = jn.get("cprtElementType").asText();
        String cprtParentId = cn.getInstanceVar("elementIdentifier");

        ArrayList<CprtNode> cprtRet = fw.getCprtNodesByTypeAndParentId(cprtElementType, cprtParentId);
        if(cprtRet.isEmpty()) return null;
        
        CprtNode cprtNode = cprtRet.get(0);
        String xmlStr = makeXmlFromNode(jn, cprtNode);
        
        return xmlStr;
    }

    private String getCprtOscalLookupProp(String elementType, String parentIdentifier, String index, String propName) throws NoSuchFieldException{
        ArrayList<CprtNode> cprtRet = fw.getCprtNodesByTypeAndParentId(elementType, parentIdentifier);
        if(cprtRet.isEmpty()) return null;
        int i = Integer.parseInt(index);
        CprtNode cprtNode = cprtRet.get(i);
        String xmlStr = cprtNode.instanceVars.get(propName);
        return xmlStr;
    }
    
    private String getCprtOscalList(JsonNode jn, CprtNode cn) throws NoSuchFieldException{
        String cprtElementType = jn.get("cprtElementType").asText();
        String cprtParentId = null;
        if(cn != null) cprtParentId = cn.getInstanceVar("elementIdentifier");
        ArrayList<CprtNode> cprtNodeList;
        if((cprtParentId == null) || (cprtParentId.equals(""))) {
            cprtNodeList = fw.getCprtNodesByType(cprtElementType);
        }
        else {
            cprtNodeList = fw.getCprtNodesByTypeAndParentId(cprtElementType, cprtParentId);
        }

        if(cprtNodeList.isEmpty()) return null;
        String xmlStr = "";
        for(CprtNode cNode: cprtNodeList) {
            xmlStr = xmlStr + makeXmlFromNode(jn, cNode);
        }
        
        return xmlStr;
    }
    
    private String getCprtRootOscalList(JsonNode jn) throws NoSuchFieldException{
        String cprtElementType = jn.get("cprtElementType").asText();
        
        if(cprtElementType.equals("reference")) fw.removeDuplicateReferences();
        if(cprtElementType.equals("ref_item")) fw.removeDuplicateRefItems();
        
        ArrayList<CprtNode> cprtNodeList = fw.getCprtNodesByType(cprtElementType);

        if(cprtNodeList.isEmpty()) return null;
        String xmlStr = "";
        for(CprtNode cNode: cprtNodeList) {
            xmlStr = xmlStr + makeXmlFromNode(jn, cNode);
        }
        
        return xmlStr;
    }

}

