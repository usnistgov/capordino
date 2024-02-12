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
