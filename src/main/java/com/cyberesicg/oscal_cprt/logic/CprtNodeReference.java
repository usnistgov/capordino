package com.cyberesicg.oscal_cprt.logic;

//
// @author jimlh
//
public class CprtNodeReference extends CprtNode {
    
    CprtNodeReference() {
        super();
        cprtType = CprtNodeType.cprtReference;

        //Since we store nodes in a flat list, they all need to have the same
        //data organization. These variables contain the data that was in the
        //CPRT response, but needed to be refactored to be the same as all the
        //other nodes.
        instanceVars.put("refElementIdentifier", "");
        instanceVars.put("refElementTypeIdentifier", "");
    }

    
}
