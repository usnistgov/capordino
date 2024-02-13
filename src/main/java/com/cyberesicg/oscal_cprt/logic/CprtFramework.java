package com.cyberesicg.oscal_cprt.logic;

import static com.cyberesicg.oscal_cprt.logic.CprtNode.CprtNodeType.cprtRefItem;
import static com.cyberesicg.oscal_cprt.logic.CprtNode.CprtNodeType.cprtReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jimlh
 */
public class CprtFramework {
    public ArrayList<CprtNode> nodes = new ArrayList<>();
    public ArrayList<Integer> OLIRIds;
    public HashMap<String, String> uuidRefs = new HashMap<>();

    public String fwId;
    public String tgtFwId; //Only used during mapping document creation
    public String fwDisplayName;
    private static int idSerialNum = 0;
    private boolean bUpdateCache;
    
    //Source framework is the framework that all references are made from
    public CprtFramework(String _fwId, boolean bUpdateCache_) {
        OLIRIds = new ArrayList<>();
        fwId = FwNameResolver.resolve(_fwId);
        fwDisplayName = FwNameResolver.resolveDisplayName(_fwId);
        bUpdateCache = bUpdateCache_; 
    }

    public ArrayList<CprtNode> getCprtNodesByType(String type) {
        ArrayList<CprtNode> retList = new ArrayList<>();
        for(CprtNode n: nodes) {
            if(n.instanceVars.get("elementTypeIdentifier").equals(type)) {
                retList.add(n);
            }
        }
        return retList;
    }


    public ArrayList<CprtNode> getCprtNodesByTypeAndParentId(String type, String parentId) {
        ArrayList<CprtNode> retList = new ArrayList<>();
        for(CprtNode n: nodes) {
            if(n.instanceVars.get("elementTypeIdentifier").equals(type)) {
                if(n.instanceVars.get("parentNodeId").equals(parentId) ||
                                        parentId.equals("*")) {
                    retList.add(n);
                }
            }
        }
        return retList;
    }
    
    //Use CPRT endpoint to populate this framework object
    //This method calls processCprtElement for every root element in the framework
    public void getFrameworkDataFromCPRT(){
        JsonNode responseJson = getJsonFromWeb();
        JsonNode rootElements = responseJson.get("elements");
        if(!rootElements.isArray()) return;
        ArrayNode an = (ArrayNode)rootElements;
        for(JsonNode n: an){
            processCprtElement(n, "", null);
        }
    }


    //Use CPRT endpoint to populate this framework object
    //This method traverses the entire framework tree recursively and adds
    //Olirs to the nodes table
    public void getFrameworkOlirDataFromCPRT(MappingDoc mapDoc){
        JsonNode responseJson = getJsonFromWeb();
        JsonNode rootEles = responseJson.get("elements");
        ArrayNode an = (ArrayNode)rootEles;
        if(rootEles.isArray()) {
            for(JsonNode n: an){
                gatherOlirs(n, "", mapDoc);
            }
        }
    }

    private void gatherOlirs(JsonNode n, String parentElementId, MappingDoc mapDoc){
    
        String parentId = n.get("elementIdentifier").asText();
        JsonNode olirNodes = n.get("externalRelationships");
        if((olirNodes != null) && olirNodes.isArray()) {
            ArrayNode an = (ArrayNode)olirNodes;
            for(JsonNode olirNode: an) {
                CprtNode cn = processOLIRNode(olirNode, parentId, mapDoc);
                if(cn != null) {
                    String str = cn.instanceVars.get("olirEntryElementId");
                    int i = Integer.parseInt(str);
                    OLIRIds.add(i);
                    nodes.add(cn);
                }
            }
        }
        
        JsonNode childEles = n.get("elements");
        if(childEles != null) {
            ArrayNode an = (ArrayNode)childEles;
            for(JsonNode child: an){
                gatherOlirs(child, parentId, mapDoc);
            }
        }
    }
    
    private void processCprtElement(JsonNode n, String parentId, Object obj){
        CprtNode cn;
        JsonNode olirId = n.get("olirEntryElementId");
        JsonNode relatedId = n.get("relationIdentifier");
        String elementTypeIdentifier = n.get("elementTypeIdentifier").asText();
        if(olirId != null) {
            MappingDoc mapDoc = (MappingDoc)obj;
            cn = processOLIRNode(n, parentId, mapDoc);
        } else if(relatedId != null){
            cn = processRelatedNode(n, parentId);
        } else if(elementTypeIdentifier.equals("reference")){
            cn = processReferenceNode(n, parentId);
        } else if(elementTypeIdentifier.equals("ref_item")){
            ArrayList<CprtNode> cl = processRefItemNode(n, parentId);
            nodes.addAll(cl);
            return;

        } else {
            //This is a normal CPRT Node
            cn = new CprtNode();
            cn.instanceVars.put("elementIdentifier", n.get("elementIdentifier").asText());
            cn.instanceVars.put("elementTypeIdentifier", n.get("elementTypeIdentifier").asText());
            cn.instanceVars.put("title", n.get("title").asText());
            cn.instanceVars.put("text", n.get("text").asText());
            cn.instanceVars.put("parentNodeId", parentId);
        
            JsonNode childJson = n.get("elements");
            if(childJson != null){
                childJson = childJson.get(0);
                if(childJson != null){
                    String checkType = childJson.get("elementTypeIdentifier").asText();
                    if(checkType.equals("withdraw_reason")) {
                        cn.instanceVars.put("withdrawReason", childJson.get("text").asText());
                    }
                }
            }
        }    

        if(cn != null) {
            nodes.add(cn);
        }
        
        //Now process children
        JsonNode eleNode = n.get("elements");
        if(eleNode != null){
            if(eleNode.isArray()){
                ArrayNode an = (ArrayNode)eleNode;
                for(JsonNode cen: an){
                    if(cn != null){
                        processCprtElement(cen, cn.instanceVars.get("elementIdentifier"), obj);
                    }
                }
            }
        }

        JsonNode olirsNode = n.get("externalRelationships");
        if(olirsNode != null){
            if(olirsNode.isArray()){
                ArrayNode an = (ArrayNode)olirsNode;
                for(JsonNode cen: an){
                    if(cn != null){
                        processCprtElement(cen, cn.instanceVars.get("elementIdentifier"), obj);
                    }
                }
            }
        }
    }

    private CprtNode processOLIRNode(JsonNode n, String parentId, MappingDoc mapDoc){
        CprtNodeExternalRelationship cn = new CprtNodeExternalRelationship();
        cn.instanceVars.put("erElementIdentifier", n.get("elementIdentifier").asText());
        cn.instanceVars.put("erElementTypeIdentifier", n.get("elementTypeIdentifier").asText());
        cn.instanceVars.put("title", n.get("title").asText());
        cn.instanceVars.put("text", n.get("text").asText());
        cn.instanceVars.put("relationIdentifier", n.get("relationIdentifier").asText());
        String olirDbElementId = n.get("olirEntryElementId").asText();
        cn.instanceVars.put("olirEntryElementId", olirDbElementId);
        cn.instanceVars.put("shortName", n.get("shortName").asText());
        cn.instanceVars.put("parentNodeId", parentId);
        idSerialNum++;
        cn.instanceVars.put("elementIdentifier", "OLIRNode" + idSerialNum); 
        cn.instanceVars.put("elementTypeIdentifier", "olirNode");

        if(mapDoc != null){
            cn.instanceVars.put("srcCatFName", mapDoc.srcCatFName);
            cn.instanceVars.put("srcCatUuid", mapDoc.srcCatUuid);
            cn.instanceVars.put("tgtCatFName", mapDoc.tgtCatFName);
            cn.instanceVars.put("tgtCatUuid", mapDoc.tgtCatUuid);
        }
        
        if(mapDoc != null){
            cn.getOLIRFromDB(olirDbElementId);
            try {
                cn = checkSrcTgtAndDupOLIR(cn);
            } catch (IOException ex) {
                Logger.getLogger(CprtFramework.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return cn;
    }
    
    private CprtNodeExternalRelationship checkSrcTgtAndDupOLIR(CprtNodeExternalRelationship cn) throws IOException{
        //Write olir data to olirCache
        String str = cn.instanceVars.get("olirEntryElementId");
        if(bUpdateCache) {
            ObjectMapper om = new ObjectMapper();
            String path = AppUtils.gOutFolder + "\\olirCache\\" + str + ".json";
            om.writeValue(new File(path), cn.instanceVars);
        }

        str = cn.instanceVars.get("dbFocalFrameworkVersionIdentifier").toLowerCase();
        if(!fwId.equals(str)) {
            return null;
        }
        str = cn.instanceVars.get("dbReferenceFrameworkVersionIdentifier").toLowerCase();
        if(str == null) {
            return null;
        }
        if(!tgtFwId.equals(str)) {
            return null;
        } 
        str = cn.instanceVars.get("olirEntryElementId");
        int i = Integer.parseInt(str);
        if(OLIRIds.contains(i)) {
            return null;
        }
        
        return cn;
    }
    
    private CprtNode processRelatedNode(JsonNode n, String parentId){
        CprtNodeRelated cn = new CprtNodeRelated();
        cn.instanceVars.put("relElementIdentifier", n.get("elementIdentifier").asText());
        cn.instanceVars.put("relElementTypeIdentifier", n.get("elementTypeIdentifier").asText());
        cn.instanceVars.put("title", n.get("title").asText());
        cn.instanceVars.put("text", n.get("text").asText());
        cn.instanceVars.put("relationIdentifier", n.get("relationIdentifier").asText());
        cn.instanceVars.put("parentNodeId", parentId);
        idSerialNum++;
        cn.instanceVars.put("elementIdentifier", "related" + idSerialNum); 
        cn.instanceVars.put("elementTypeIdentifier", "related");

        return cn;
    }
    
    private CprtNode processReferenceNode(JsonNode n, String parentId){
        String uuidStr = checkForExistingReference(n);
        
        CprtNodeReference cn = new CprtNodeReference();
        cn.instanceVars.put("refElementIdentifier", n.get("elementIdentifier").asText());
        cn.instanceVars.put("refElementTypeIdentifier", n.get("elementTypeIdentifier").asText());
        cn.instanceVars.put("title", n.get("title").asText());
        cn.instanceVars.put("text", n.get("text").asText());
        cn.instanceVars.put("parentNodeId", parentId);
        idSerialNum++;
        cn.instanceVars.put("elementIdentifier", "reference_" + idSerialNum); 
        cn.instanceVars.put("elementTypeIdentifier", "reference");

        if(uuidStr.equals("")){
            uuidStr = UUID.randomUUID().toString();
        }
        cn.instanceVars.put("uuid", uuidStr);

        return cn;
    }
    
    private String checkForExistingReference(JsonNode checkNode){
        for(CprtNode n: nodes){
            if(n.cprtType == cprtReference){
                CprtNodeReference nr = (CprtNodeReference)n;
                String str = nr.instanceVars.get("text");
                String checkStr = checkNode.get("text").asText();
                if(!str.equals(checkStr)) continue;
                str = nr.instanceVars.get("title");
                checkStr = checkNode.get("title").asText();
                if(!str.equals(checkStr)) continue;
                str = nr.instanceVars.get("refElementIdentifier");
                checkStr = checkNode.get("elementIdentifier").asText();
                if(!str.equals(checkStr)) continue;
                return n.instanceVars.get("uuid");
            }
        }
        return "";
    }
    
    private ArrayList<CprtNode> processRefItemNode(JsonNode n, String parentId){
        CprtNodeRefItem cn = new CprtNodeRefItem();
        cn.instanceVars.put("title", n.get("title").asText());
        cn.instanceVars.put("text", n.get("text").asText());
        cn.instanceVars.put("parentNodeId", parentId);
        idSerialNum++;
        cn.instanceVars.put("elementIdentifier", "refItemNode" + idSerialNum); 
        cn.instanceVars.put("elementTypeIdentifier", "ref_item");

        //TODO: fix this cn.instanceVars.put("refElementIdentifier", n.get("elementIdentifier").asText());
        //TODO: fix this cn.instanceVars.put("refElementTypeIdentifier", n.get("elementTypeIdentifier").asText());
        //TODO: get fields from child element
        cn.instanceVars.put("docId", n.get("title").asText());
        JsonNode childEle = n.get("elements").get(0);
        cn.instanceVars.put("docInfo", childEle.get("title").asText());
        cn.instanceVars.put("docUrl", childEle.get("text").asText());
        
        //Some ref_items do not put the docId on the parentEle:title
        //If that is the case use the childEle:elementIdentifier as the docId
        if(cn.instanceVars.get("title").equals("")){
            cn.instanceVars.put("title", childEle.get("elementIdentifier").asText());
        }

        //docElementId
        ArrayList<CprtNode> newNodes = new ArrayList<>();
        String docEleIdStr = n.get("text").asText();
        String[] strs = docEleIdStr.split(",");
        
        for(String str : strs) {
            str = str.trim();
            CprtNodeRefItem newNode = new CprtNodeRefItem();
            newNode.instanceVars.put("title", cn.instanceVars.get("title"));
            newNode.instanceVars.put("text", cn.instanceVars.get("text"));
            newNode.instanceVars.put("parentNodeId", cn.instanceVars.get("parentNodeId"));
            idSerialNum++;
            newNode.instanceVars.put("elementIdentifier", "refItemNode" + idSerialNum);
            newNode.instanceVars.put("elementTypeIdentifier", cn.instanceVars.get("elementTypeIdentifier"));
            newNode.instanceVars.put("docId", cn.instanceVars.get("docId"));
            newNode.instanceVars.put("docInfo", cn.instanceVars.get("docInfo"));
            newNode.instanceVars.put("docUrl", cn.instanceVars.get("docUrl"));
            newNode.instanceVars.put("docElementId", str);

            String uuidStr = checkForExistingRefItem(newNode);
            if((uuidStr == null) || (uuidStr.equals(""))) uuidStr = UUID.randomUUID().toString();
            newNode.instanceVars.put("uuid", uuidStr);
            
            newNodes.add(newNode);
        }
        
        return newNodes;
    }
    
    private String checkForExistingRefItem(CprtNode cnr){
        for(CprtNode n: nodes){
            if(n.cprtType == cprtRefItem){
                CprtNodeRefItem nr = (CprtNodeRefItem)n;
                
                if(cnr.instanceVars.get("docElementId").equals(nr.instanceVars.get("docElementId"))){
                    int x = 1;
                    x++;
                }
                String checkStr = nr.instanceVars.get("title");
                if(!checkStr.equals(cnr.instanceVars.get("title"))) continue;
                checkStr = nr.instanceVars.get("elementTypeIdentifier");
                if(!checkStr.equals(cnr.instanceVars.get("elementTypeIdentifier"))) continue;
                checkStr = nr.instanceVars.get("docId");
                if(!checkStr.equals(cnr.instanceVars.get("docId"))) continue;
                checkStr = nr.instanceVars.get("docInfo");
                if(!checkStr.equals(cnr.instanceVars.get("docInfo"))) continue;
                checkStr = nr.instanceVars.get("docUrl");
                if(!checkStr.equals(cnr.instanceVars.get("docUrl"))) continue;
                checkStr = nr.instanceVars.get("docElementId");
                if(!checkStr.equals(cnr.getInstanceVar("docElementId"))) continue;
                return n.instanceVars.get("uuid");
            }
        }
        return "";
    }

    private JsonNode getJsonFromWeb(){
        String uriStr = "https://csrc.nist.gov/extensions/nudp/services/json/nudp/framework/version/";
        uriStr = uriStr + fwId;
        uriStr = uriStr + "/element/all/graph";
        URL url;
        try {
            url = new URI(uriStr).toURL();
        } catch (URISyntaxException | MalformedURLException ex) {
            Logger.getLogger(CprtFramework.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jn;
        try {
            jn = mapper.readTree(url);
        } catch (IOException ex) {
            Logger.getLogger(CprtFramework.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        //Check for failure

        JsonNode resp = jn.get("response");
        
        if(resp == null)
        {
            resp = getJsonFromLocalFile();
        }
        return resp;
    }

    private JsonNode getJsonFromLocalFile() {
        String filePath = "cprtJson/cprt_" + fwId.toLowerCase() + ".json";
        InputStream is = getClass().getClassLoader().getResourceAsStream(filePath);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode retNode = null;
        try {
            retNode = mapper.readTree(is);
        } catch (IOException ex) {
            Logger.getLogger(CprtFramework.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

        retNode = retNode.get("response");
        return retNode;
    }

    public void removeDuplicateReferences(){
        ArrayList<CprtNode> cprtNodeList = getCprtNodesByType("reference");
        ArrayList<String> uuids = new ArrayList<>();
        ArrayList<String> texts = new ArrayList<>();

        for(CprtNode n: cprtNodeList){
            CprtNodeReference nr = (CprtNodeReference)n;
            String checkStr = nr.instanceVars.get("uuid");
            if(uuids.contains(checkStr)){
                nodes.remove(n);
            } else {
                uuids.add(checkStr);
                texts.add(n.instanceVars.get("refElementIdentifier"));
            }
            
            for(String str:texts){
                System.out.println(str);
            }
                
        }
    }
    
    public void removeDuplicateRefItems(){
        ArrayList<CprtNode> cprtNodeList = getCprtNodesByType("ref_item");
        ArrayList<String> uuids = new ArrayList<>();
        ArrayList<String> texts = new ArrayList<>();

        for(CprtNode n: cprtNodeList){
            CprtNodeRefItem nr = (CprtNodeRefItem)n;
            String checkStr = nr.instanceVars.get("uuid");
            if(uuids.contains(checkStr)){
                nodes.remove(n);
            } else {
                uuids.add(checkStr);
                texts.add(n.instanceVars.get("docElementId"));
            }
            
            for(String str:texts){
                System.out.println(str);
            }
                
        }
    }
    
}

