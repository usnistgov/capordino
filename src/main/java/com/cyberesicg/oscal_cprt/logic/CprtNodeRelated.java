//
// Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
// Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
//

package com.cyberesicg.oscal_cprt.logic;

//
// @author jimlh
//

public class CprtNodeRelated extends CprtNode {
    
    CprtNodeRelated() {
        super();
        cprtType = CprtNodeType.cprtRelated;
        
        //Since we store nodes in a flat list, they all need to have the same
        //data organization. These variables contain the data that was in the
        //CPRT response, but needed to be refactored to be the same as all the
        //other nodes.
        instanceVars.put("relElementIdentifier", "");
        instanceVars.put("relElementTypeIdentifier", "");
        instanceVars.put("relationIdentifier", "");
    }

    
}
