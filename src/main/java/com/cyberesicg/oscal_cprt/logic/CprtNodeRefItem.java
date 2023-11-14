//
// Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
// Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
//

package com.cyberesicg.oscal_cprt.logic;

//
// @author jimlh
//

public class CprtNodeRefItem extends CprtNode {
    
    CprtNodeRefItem() {
        super();
        cprtType = CprtNodeType.cprtRefItem;
        
        instanceVars.put("docId", "");
        instanceVars.put("docElementId", "");
        instanceVars.put("docInfo", "");
        instanceVars.put("docUrl", "");
    }

    
}
