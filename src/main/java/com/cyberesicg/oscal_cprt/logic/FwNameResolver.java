//
// Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
// Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
//

package com.cyberesicg.oscal_cprt.logic;

//

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

// @author jimlh
//
public class FwNameResolver {
    static private Map<String, String> fwIds = new HashMap<>();
    static private Map<String, String> fwDisplayNames = new HashMap<>();
    static private ArrayList<String> unresolvedNames = new ArrayList<>();
    static public ArrayList<String> cprtFrameworks = new ArrayList<>();
    
    FwNameResolver(){
    }
    
    private static void fillTables(){
        FwNameResolver nr = new FwNameResolver();
        InputStream is = nr.getClass().getClassLoader().getResourceAsStream("cprtJson/toolData.json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode tdNode = null;
        try {
            tdNode = mapper.readTree(is);
        } catch (IOException ex) {
            Logger.getLogger(ConversionControlFile.class.getName()).log(Level.SEVERE, null, ex);
        }

        if(tdNode == null) return;
        JsonNode jn = tdNode.get("frameworkNameResolver");
        if(!jn.isArray()) return;
        ArrayNode an = (ArrayNode)jn;
        for(JsonNode n: an){
            ArrayNode fwNameArray = (ArrayNode)n;
            String fwIdStr = n.get(0).asText();
            fwDisplayNames.put(fwIdStr, n.get(1).asText());
            for(JsonNode fwName: fwNameArray)
            {
                fwIds.put(fwName.asText(), fwIdStr);
            }
        }
        
        JsonNode fwsNode = tdNode.get("cprtFrameworks");
        ArrayNode an2  = (ArrayNode)fwsNode;
        for(JsonNode n: an2){
            String str = n.asText();
            cprtFrameworks.add(str);
        }
        
        
    }
    
    public static String resolve(String fwId) {
        if(fwIds.isEmpty()) fillTables();

        String fwIdOut = fwIds.get(fwId);

        if(fwIdOut == null){
            if(!unresolvedNames.contains(fwId)){
                unresolvedNames.add(fwId);
            }
        }
        return fwIdOut;
    }
    
    public static String resolveDisplayName(String fwId) {
        return fwDisplayNames.get(fwId);
    }
    
    public static void showUnresolvedNames(){
        for(String str: unresolvedNames){
            String newLine = String.format(
                    "        fwIds.put(\"%s\", \"%s\");",
                    str, str);
            System.out.println(newLine);
        }
    }

}
