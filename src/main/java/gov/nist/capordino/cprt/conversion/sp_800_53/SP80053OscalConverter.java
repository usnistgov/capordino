package gov.nist.capordino.cprt.conversion.sp_800_53;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gov.nist.capordino.cprt.conversion.AbstractOscalConverter;
import gov.nist.capordino.cprt.conversion.InvalidFrameworkIdentifier;
import gov.nist.capordino.cprt.pojo.CprtElement;
import gov.nist.capordino.cprt.pojo.CprtMetadataVersion;
import gov.nist.capordino.cprt.pojo.CprtRoot;
import gov.nist.secauto.metaschema.model.common.datatype.markup.MarkupLine;
import gov.nist.secauto.metaschema.model.common.datatype.markup.MarkupMultiline;
import gov.nist.secauto.oscal.lib.model.Catalog;
import gov.nist.secauto.oscal.lib.model.CatalogGroup;
import gov.nist.secauto.oscal.lib.model.Control;
import gov.nist.secauto.oscal.lib.model.ControlPart;
import gov.nist.secauto.oscal.lib.model.Parameter;
import gov.nist.secauto.oscal.lib.model.Property;
import gov.nist.secauto.oscal.lib.model.BackMatter.Resource;
import gov.nist.secauto.oscal.lib.model.Link;
import gov.nist.secauto.oscal.lib.model.BackMatter.Resource.Rlink;
import gov.nist.secauto.oscal.lib.model.BackMatter.Resource.Citation;

public class SP80053OscalConverter extends AbstractOscalConverter {
    protected void assertFrameworkIdentifier() throws InvalidFrameworkIdentifier {
        if (!cprtMetadataVersion.frameworkIdentifier.equals("SP_800_53")) {
            throw new InvalidFrameworkIdentifier("SP_800_53", cprtMetadataVersion.frameworkIdentifier);
        }
    }

    public SP80053OscalConverter(CprtMetadataVersion cprtMetadataVersion, CprtRoot cprtRoot) throws InvalidFrameworkIdentifier {
        super(cprtMetadataVersion, cprtRoot);
        assertFrameworkIdentifier();
    }
    
    public SP80053OscalConverter(CprtMetadataVersion cprtMetadataVersion) throws IOException, InterruptedException, InvalidFrameworkIdentifier {
        super(cprtMetadataVersion);
        assertFrameworkIdentifier();
    }

    public SP80053OscalConverter(String frameworkVersionIdentifier) throws IOException, InterruptedException, InvalidFrameworkIdentifier {
        super(frameworkVersionIdentifier);
        assertFrameworkIdentifier();
    }

    private final String CONTROL_ELEMENT_TYPE = "control";
    private final String CONTROL_ENHANCEMENT_ELEMENT_TYPE = "control_enhancement";
    private final String CONTROL_NAME_SORT_ELEMENT_TYPE = "control_name_sort";
    private final String CONTROL_STATEMENT_ELEMENT_TYPE = "control_statement";
    private final String DETERMINATION_ELEMENT_TYPE = "determination";
    private final String DISCUSSION_ELEMENT_TYPE = "discussion";
    private final String EXAMINE_ELEMENT_TYPE = "examine";
    private final String FAMILY_ELEMENT_TYPE = "family";
    private final String INTERVIEW_ELEMENT_TYPE = "interview";
    private final String ODP_ELEMENT_TYPE = "odp";
    private final String ODP_STATEMENT_ELEMENT_TYPE = "odp_statement";
    private final String ODP_TYPE_ELEMENT_TYPE = "odp_type";
    private final String PRIVACY_BASELINE_ELEMENT_TYPE = "privacy_baseline";
    private final String PUBLIC_COMMENT_ELEMENT_TYPE = "public_comment";
    private final String REFERENCE_ELEMENT_TYPE = "reference";
    private final String SECURITY_BASELINE_ELEMENT_TYPE = "security_baseline";
    private final String SORT_ELEMENT_TYPE = "sort";
    private final String TEST_ELEMENT_TYPE = "test";
    private final String WITHDRAW_REASON_ELEMENT_TYPE = "withdraw_reason";
    
    
    

    private final String PROJECTION_RELATIONSHIP_TYPE = "projection";
    private final String RELATED_RELATIONSHIP_TYPE = "related";
    private final String INCORPORATED_INTO_RELATIONSHIP_TYPE = "incorporated_into";
    private final String MOVED_TO_RELATIONSHIP_TYPE = "moved_to";

    private final String[] WITHDRAW_RELATIONSHIPS = new String[] {INCORPORATED_INTO_RELATIONSHIP_TYPE, MOVED_TO_RELATIONSHIP_TYPE};

    /**
     * The URI to use for CSF-specific props.
     */
    private final URI CSF_URI = URI.create("https://csrc.nist.gov/ns/csf");

    @Override
    protected void hydrateCatalog(Catalog catalog) {
        catalog.setGroups(buildFamilyGroups(catalog));
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
    

    /**
     * Build the top level group of the catalog, represented in CPRT as families.
     */
    private List<CatalogGroup> buildFamilyGroups(Catalog catalog) {
        // Recursively go down tree of elements, to build family groups
        return cprtRoot.getElements().stream()
            .filter(elem -> elem.element_type.equals(FAMILY_ELEMENT_TYPE))
            .map(elem -> {
                // For each 800-53 family, create an OSCAL group
                CatalogGroup group = new CatalogGroup();
                group.setId(elem.element_identifier);
                group.setClazz(elem.element_type);
                group.setTitle(MarkupLine.fromMarkdown(elem.title));

                // For 800-171 control, create an OSCAL control within this overall family group
                group.setControls(buildControls(catalog, elem.getGlobalIdentifier()));

                Property sortProp = buildSortProp(elem.getGlobalIdentifier());
                if (sortProp != null) {
                    group.addProp(sortProp);
                }

                group.addProp(buildLabelProp(elem.title + " (" + elem.element_identifier + ")"));

                return group;
            }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }


    // For 800-53 control, create an OSCAL control
    private List<Control> buildControls(Catalog catalog, String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, CONTROL_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            Control control = new Control();
            control.setId(elem.element_identifier);
            control.setClazz("SP800-53");
            control.setTitle(MarkupLine.fromMarkdown(elem.title));


           

            List<ControlPart> parts = new ArrayList<ControlPart>();
            parts.add(buildPartFromElementText(elem, "statement"));
            
            
            // For 800-171 control, create an OSCAL control within this overall family group
            // group.setControls(buildControls(catalog, elem.getGlobalIdentifier()));

            // For 800-171 control enhancement, create an OSCAL control within this overall family group

            Property sortProp = buildSortProp(elem.getGlobalIdentifier());
            if (sortProp != null) {
                control.addProp(sortProp);
            }

            control.addProp(buildLabelProp(elem.title + " (" + elem.element_identifier + ")"));
            
            return control;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
}
