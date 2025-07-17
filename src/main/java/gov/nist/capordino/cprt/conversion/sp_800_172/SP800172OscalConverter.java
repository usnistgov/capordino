package gov.nist.capordino.cprt.conversion.sp_800_172;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import gov.nist.capordino.cprt.conversion.AbstractOscalConverter;
import gov.nist.capordino.cprt.conversion.InvalidFrameworkIdentifier;
import gov.nist.capordino.cprt.pojo.CprtElement;
import gov.nist.capordino.cprt.pojo.CprtMetadataVersion;
import gov.nist.capordino.cprt.pojo.CprtRoot;
import gov.nist.secauto.metaschema.model.common.datatype.markup.MarkupLine;
import gov.nist.secauto.metaschema.model.common.datatype.markup.MarkupMultiline;
import gov.nist.secauto.oscal.lib.model.BackMatter.Resource;
import gov.nist.secauto.oscal.lib.model.Catalog;
import gov.nist.secauto.oscal.lib.model.CatalogGroup;
import gov.nist.secauto.oscal.lib.model.Control;
import gov.nist.secauto.oscal.lib.model.ControlPart;
import gov.nist.secauto.oscal.lib.model.Link;
import gov.nist.secauto.oscal.lib.model.Property;

public class SP800172OscalConverter extends AbstractOscalConverter {
    protected void assertFrameworkIdentifier() throws InvalidFrameworkIdentifier {
        if (!cprtMetadataVersion.frameworkIdentifier.equals("SP800_172")) {
            throw new InvalidFrameworkIdentifier("SP800_172", cprtMetadataVersion.frameworkIdentifier);
        }
    }

    public SP800172OscalConverter(CprtMetadataVersion cprtMetadataVersion, CprtRoot cprtRoot) throws InvalidFrameworkIdentifier {
        super(cprtMetadataVersion, cprtRoot);
        assertFrameworkIdentifier();
    }
    
    public SP800172OscalConverter(CprtMetadataVersion cprtMetadataVersion) throws IOException, InterruptedException, InvalidFrameworkIdentifier {
        super(cprtMetadataVersion);
        assertFrameworkIdentifier();
    }

    public SP800172OscalConverter(String frameworkVersionIdentifier) throws IOException, InterruptedException, InvalidFrameworkIdentifier {
        super(frameworkVersionIdentifier);
        assertFrameworkIdentifier();
    }

    private final String ADVERSARY_EFFECT_ELEMENT_TYPE = "adversary_effect";
    private final String DISCUSSION_ELEMENT_TYPE = "discussion";
    private final String EFFECT_ELEMENT_TYPE = "effect";
    private final String EXAMPLE_ELEMENT_TYPE = "example";
    private final String EXPECTED_RESULT_ELEMENT_TYPE = "expected_result";
    private final String FAMILY_ELEMENT_TYPE = "family";
    private final String IMPACT_ELEMENT_TYPE = "impact";
    private final String PROTECTION_STRATEGY_ELEMENT_TYPE = "protection_strategy";
    private final String REFERENCE_ITEM_ELEMENT_TYPE = "reference_item";
    private final String SECURITY_REQUIREMENT_ELEMENT_TYPE = "security_requirement";
    private final String SORT_ELEMENT_TYPE = "sort";
    private final String TACTIC_ELEMENT_TYPE = "tactic";


    private final String PROJECTION_RELATIONSHIP_TYPE = "projection";
    private final String EXTERNAL_REFERENCE_RELATIONSHIP_TYPE = "external_reference";


    @Override
    protected void hydrateCatalog(Catalog catalog) {
        catalog.setGroups(buildGroups(catalog));
    }



    

    private Property buildSortProp(String parentId) {
        List<CprtElement> sorts = getRelatedElementsBySourceIdWithType(parentId, SORT_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        if (sorts.size() == 0) {
            return null;
        }

        if (sorts.size() > 1) {
            throw new IllegalStateException("More than one sort found for function " + parentId);
        }

        CprtElement sort = sorts.get(0);

        Property sortProp = new Property();
        sortProp.setName("sort-id");
        sortProp.setValue(sort.title);
        return sortProp;
    }
   
}
