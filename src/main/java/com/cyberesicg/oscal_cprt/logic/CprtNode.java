//
// Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
// Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
//

package com.cyberesicg.oscal_cprt.logic;

//

import java.util.HashMap;

// @author jimlh
//
public class CprtNode {
    public enum CprtNodeType {
      cprtNormal,
      cprtOlir,
      cprtRelated,
      cprtReference,
      cprtRefItem
    };
    
    HashMap<String, String> instanceVars;
    public CprtNodeType cprtType;

    CprtNode() {
        cprtType = CprtNodeType.cprtNormal; 
        instanceVars = new HashMap<>();
        instanceVars.put("parentNodeId", "");
        instanceVars.put("elementIdentifier", "");
        instanceVars.put("elementTypeIdentifier", "");
        instanceVars.put("title", "");
        instanceVars.put("text", "");
    }

    public void setInstanceVar(String name, String val) {
        instanceVars.put(name, val);
    }

    public String getInstanceVar(String name) {
        return instanceVars.get(name);
    }
    
}
