package com.cyberesicg.oscal_cprt.logic;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

// @author jimlh
// OLIR node
public class CprtNodeExternalRelationship extends CprtNode {
    CprtNodeExternalRelationship() {
        super();
        cprtType = CprtNodeType.cprtOlir;
        //Since we store nodes in a flat list, they all need to have the same
        //data organization. These variables contain the data that was in the
        //CPRT response, but needed to be refactored to be the same as all the
        //other nodes.
        instanceVars.put("erElementIdentifier", "");
        instanceVars.put("erElementTypeIdentifier", "");

        //These are extra needed node info for OLIR
        instanceVars.put("relationIdentifier", "");
        instanceVars.put("olirEntryElementId", "");
        instanceVars.put("shortName", "");
    }
    
    public void getOLIRFromDB(String olirIdNum){
        JsonNode olirNode = getJsonFromOlirCache(olirIdNum);
        if(olirNode == null) {
            olirNode = getJsonFromWeb(olirIdNum);
            olirNode = olirNode.get("versions").get(0);
            processCprtOLIR(olirNode);
        } else {
            ObjectMapper om = new ObjectMapper();
            HashMap<String, String> vals = om.convertValue(olirNode, new TypeReference<HashMap<String, String>>(){});
            instanceVars.putAll(vals);
        }
            
    }

    private JsonNode getJsonFromOlirCache(String olirIdNum) {
        JsonNode retNode = null;
        String path = AppUtils.gOutFolder + "\\olirCache\\" + olirIdNum + ".json";
        File f = new File(path);
        if(!f.exists()) return retNode;
        
        ObjectMapper mapper = new ObjectMapper();
        try {
            retNode = mapper.readTree(f);
        } catch (IOException ex) {
            Logger.getLogger(CprtNodeExternalRelationship.class.getName()).log(Level.SEVERE, null, ex);
        }
        return retNode;
    } 
    
    private JsonNode getJsonFromWeb(String olirIdNum) {
        String uriStr = "https://csrc.nist.gov/extensions/nudp/services/json/nudp/cprt-relationship/";
        uriStr = uriStr + olirIdNum;
        URL url;
        try {
            url = new URI(uriStr).toURL();
        } catch (URISyntaxException | MalformedURLException ex) {
            Logger.getLogger(CprtNodeExternalRelationship.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jn;
        try {
            jn = mapper.readTree(url);
        } catch (IOException ex) {
            Logger.getLogger(CprtNodeExternalRelationship.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

        JsonNode resp = jn.get("response");
        System.out.println("OLIR : " + olirIdNum);
        return resp;
    }


    private void processCprtOLIR(JsonNode n){
        putOLIRVar("dbFocalDocTemplateName", "focalDocTemplateName", n);
        putOLIRVar("dbFocalFrameworkVersionName", "focalFrameworkVersionName", n);
        putOLIRVar("dbFocalFrameworkVersionIdentifier", "focalFrameworkVersionIdentifier", n);
        putOLIRVar("dbReferenceFrameworkVersionName", "referenceFrameworkVersionName", n);
        putOLIRVar("dbReferenceFrameworkVersionIdentifier", "referenceFrameworkVersionIdentifier", n);
        putOLIRVar("dbFocalElementIdentifier", "focalElementIdentifier", n);
        putOLIRVar("dbFocalTitle", "focalTitle", n);
        putOLIRVar("dbFocalTextSimple", "focalTextSimple", n);
        putOLIRVar("dbReferenceElementIdentifier", "referenceElementIdentifier", n);
        putOLIRVar("dbReferenceTitle", "referenceTitle", n);
        putOLIRVar("dbReferenceTextSimple", "referenceTextSimple", n);
        putOLIRVar("dbRationaleDescription", "rationaleDescription", n);
        putOLIRVar("dbSetTypeDescription", "setTypeDescription", n);
        putOLIRVar("dbRelationshipStrengthName", "relationshipStrengthName", n);
        putOLIRVar("dbFulfilled", "fulfilled", n);

    }
    
    private void putOLIRVar(String dest, String src, JsonNode nSrc){
        if((dest == null) || (nSrc==null)) return;
        JsonNode srcNode = nSrc.get(src);
        if(srcNode == null) return;
        instanceVars.put(dest, nSrc.get(src).asText());
    }
    
}

//OLIR data in CPRT framework blob
//parentNodeId:             parent node identifier
//elementIdentifier:        given a new unique identifier (OLIRNode + serial num)
//elementTypeIdentifier:    "olirNode"
//title:                    ""
//text:                     if olir_reference: local parent element control statement
//                          if olir_focal: remote element control statement
//erElementIdentifier:      original value from cprt, actually specifiies remote document element id
//erElementTypeIdentifier:  original value from cprt, actually specifies remote element type (inconsistent)
//relationIdentifier:       olir_reference or olir_focal
//olirEntryElementId:       olir database entry number
//shortName:                remote document identifier                
