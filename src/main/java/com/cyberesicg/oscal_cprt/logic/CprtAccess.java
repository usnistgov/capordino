/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cyberesicg.oscal_cprt.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

/**
 *
 * @author jimlh
 */
public class CprtAccess {

    //This method is used to ask the CPRT endpoints for a current
    //list of supported frameworks. This can be derived from
    //using the CPRT metadata endpoint
    public ArrayList<String> getFwList() throws URISyntaxException, IOException{
        String uriStr = "https://csrc.nist.gov/extensions/nudp/services/json/nudp/metadata";
        URL url = new URI(uriStr).toURL();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jn = mapper.readTree(url);
        JsonNode resp = jn.get("response");
        JsonNode vers = resp.get("versions");
        ArrayNode versions = (ArrayNode)vers;
        ArrayList<String> fws = new ArrayList();
        for(JsonNode node: versions) {
            if(!node.get("publiclyViewable").asBoolean()) continue;
            String str = node.get("frameworkVersionIdentifier").asText();
            fws.add(str.toLowerCase());
        }
        return fws;
    }

    //This method calls CPRT endpoints to build framework objects,
    //checks them for references to the sourceFramework, and returns
    //a list of frameworkObjects that contain target references.
    ArrayList<CprtFramework> buildTgtFwsList(String sourceFramework){
        return null;
    }

}
