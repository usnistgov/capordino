package gov.nist.capordino.cprt.conversion.cprt_800_171;

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
import gov.nist.secauto.oscal.lib.model.Catalog;
import gov.nist.secauto.oscal.lib.model.CatalogGroup;
import gov.nist.secauto.oscal.lib.model.Control;
import gov.nist.secauto.oscal.lib.model.ControlPart;
import gov.nist.secauto.oscal.lib.model.Property;
import gov.nist.secauto.oscal.lib.model.BackMatter.Resource;
import gov.nist.secauto.oscal.lib.model.Link;
import gov.nist.secauto.oscal.lib.model.BackMatter.Resource.Rlink;

public class Cprt800171OscalConverter extends AbstractOscalConverter {
    protected void assertFrameworkIdentifier() throws InvalidFrameworkIdentifier {
        if (!cprtMetadataVersion.frameworkIdentifier.equals("SP_800_171_3_0_0")) {
            throw new InvalidFrameworkIdentifier("SP_800_171_3_0_0", cprtMetadataVersion.frameworkIdentifier);
        }
    }

    public Cprt800171OscalConverter(CprtMetadataVersion cprtMetadataVersion, CprtRoot cprtRoot) throws InvalidFrameworkIdentifier {
        super(cprtMetadataVersion, cprtRoot);
        assertFrameworkIdentifier();
    }
    
    public Cprt800171OscalConverter(CprtMetadataVersion cprtMetadataVersion) throws IOException, InterruptedException, InvalidFrameworkIdentifier {
        super(cprtMetadataVersion);
        assertFrameworkIdentifier();
    }

    public Cprt800171OscalConverter(String frameworkVersionIdentifier) throws IOException, InterruptedException, InvalidFrameworkIdentifier {
        super(frameworkVersionIdentifier);
        assertFrameworkIdentifier();
    }

    private final String FAMILY_ELEMENT_TYPE = "family";
    private final String REQUIREMENT_ELEMENT_TYPE = "requirement";
    private final String SECURITY_REQUIREMENT_ELEMENT_TYPE = "security_requirement";
    private final String DETERMINATION_ELEMENT_TYPE = "determination";
    private final String ODP_ELEMENT_TYPE = "odp";
    private final String ODP_STATEMENT_ELEMENT_TYPE = "odp_statement";
    private final String ODP_TYPE_ELEMENT_TYPE = "odp_type";
    private final String EXAMINE_ELEMENT_TYPE = "examine";
    private final String INTERVIEW_ELEMENT_TYPE = "interview";
    private final String TEST_ELEMENT_TYPE = "test";
    private final String DISCUSSION_ELEMENT_TYPE = "discussion";
    private final String REFERENCE_ELEMENT_TYPE = "reference";
    private final String WITHDRAW_REASON_ELEMENT_TYPE = "withdraw_reason";

    private final String PROJECTION_RELATIONSHIP_TYPE = "projection";
    private final String INCORPORATED_INTO_RELATIONSHIP_TYPE = "incorporated_into";
    private final String EXTERNAL_REFERENCE_RELATIONSHIP_TYPE = "external_reference";
    private final String ADDRESSED_BY_RELATIONSHIP_TYPE = "addressed_by";
    private final String[] WITHDRAW_RELATIONSHIPS = new String[] {INCORPORATED_INTO_RELATIONSHIP_TYPE, ADDRESSED_BY_RELATIONSHIP_TYPE};

    /**
     * The URI to use for CSF-specific props.
     */
    private final URI CSF_URI = URI.create("https://csrc.nist.gov/ns/csf");

    @Override
    protected void hydrateCatalog(Catalog catalog) {
        catalog.setGroups(buildFamilyGroups(catalog));
    }

    

    /**
     * Build the top level group of the catalog, represented in CPRT as families.
     */
    private List<CatalogGroup> buildFamilyGroups(Catalog catalog) {
        // Recursively go down tree of elements, to build family groups
        return cprtRoot.getElements().stream()
            .filter(elem -> elem.element_type.equals(FAMILY_ELEMENT_TYPE))
            .map(elem -> {
                // For each 800-171 family, create an OSCAL group
                CatalogGroup group = new CatalogGroup();
                group.setId(elem.element_identifier);
                group.setClazz(elem.element_type);
                group.setTitle(MarkupLine.fromMarkdown(elem.title));

                group.addPart(buildPartFromElementText(elem, "overview"));
                // For 800-171 requirement, create an OSCAL group within this overall family group
                group.setControls(buildRequirementControls(catalog, elem.getGlobalIdentifier()));

                group.addProp(buildLabelProp(elem.title + " (" + elem.element_identifier + ")"));

                return group;
            }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

   

    /**
     * Build the second level group of the catalog, represented in CPRT as requirements.
     */
    // For 800-171 requirement, create an OSCAL control
    private List<Control> buildRequirementControls(Catalog catalog, String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, REQUIREMENT_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            Control control = new Control();
            control.setId(elem.element_identifier);
            control.setClazz(elem.element_type);
            

            // If title is blank, then this control is withdrawn
            if (elem.title.isEmpty()) {
                control.addProp(buildWithdrawnProp());

                // Create links to the control(s) this withdrawn control points to
                List<Link> links = createWithdrawnLinks(catalog, elem.getGlobalIdentifier());

                for (Link link : links) {
                    control.addLink(link);
                }
            }
            else {
                control.setTitle(MarkupLine.fromMarkdown(elem.title));       
                
                control.addProp(buildLabelProp(elem.title + " (" + elem.element_identifier + ")"));

                List<ControlPart> parts = new ArrayList<ControlPart>();
                parts.add(buildPartFromElementText(elem, "statement"));
                // CPRT discussion -> OSCAL guidance
                parts.addAll(createGuidancePart(catalog, elem.getGlobalIdentifier()));
                // Assessment methods and objects
                parts.addAll(createAssessmentMethodParts(catalog, elem.getGlobalIdentifier()));

                control.setParts(parts);

                // For 800-171 security requirement, create OSCAL control
                control.setControls(buildSecurityRequirementControls(catalog, elem.getGlobalIdentifier()));
            }
            
            return control;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private List<ControlPart> createGuidancePart(Catalog catalog, String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, DISCUSSION_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            return buildPartFromElementText(elem, "guidance");
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    // Assessment methods are EXAMINE, INTERVIEW, TEST
    private List<ControlPart> createAssessmentMethodParts(Catalog catalog, String parentId) {
        ArrayList<ControlPart> examine_parts = getRelatedElementsBySourceIdWithType(parentId, EXAMINE_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            return buildAssessmentMethodPart(elem, ";", "[SELECT FROM: ", "]");
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        ArrayList<ControlPart> interview_parts = getRelatedElementsBySourceIdWithType(parentId, INTERVIEW_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            return buildAssessmentMethodPart(elem, ";", "[SELECT FROM: ", "]");
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        ArrayList<ControlPart> test_parts = getRelatedElementsBySourceIdWithType(parentId, TEST_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            return buildAssessmentMethodPart(elem, ";", "[SELECT FROM: ", "]");
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        examine_parts.addAll(interview_parts);
        examine_parts.addAll(test_parts);

        return examine_parts;
    }

    // Get the destination identifier of a given withdraw_reason element (get the control a withdrawn control points to)
    private List<String> getDestWithdrawIdentifiers(String parentId, String relationType) {
        List<String> dest_withdraw_identifiers = getDestinationIdWithType(parentId, relationType).map(identifier -> {
            return identifier;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        return dest_withdraw_identifiers;
        
    }
    
    // Create links to the control(s) a given withdrawn control points to
    private List<Link> createWithdrawnLinks(Catalog catalog, String parentId) {
        // Get the withdraw_reason element associated with the given element
        List<String> withdraw_identifiers = getRelatedElementsBySourceIdWithType(parentId, WITHDRAW_REASON_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            return elem.element_identifier;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        List<Link> links = new ArrayList<Link>();
        
        // For each withdraw relationship type, create links of that type
        for (String relationType : WITHDRAW_RELATIONSHIPS) {
            List<String> dest_withdraw_identifiers = new ArrayList<String>();

            // Get the control a withdrawn control points to
            for (String withdraw_identifier : withdraw_identifiers) {
                dest_withdraw_identifiers.addAll(getDestWithdrawIdentifiers(withdraw_identifier, relationType));
            }

            links.addAll(createLinks(dest_withdraw_identifiers, relationType));
        }

        return links;
    }

    // Create a list of relative Links
    private List<Link> createLinks(List<String> identifiers, String relationType) {
        List<Link> links = new ArrayList<Link>();

        for (String identifier : identifiers) {
            Link link = new Link();
            link.setHref(URI.create(identifier));
            link.setRel(relationType);
            links.add(link);
        }

        return links;
    }

    /**
     * Build the third level control of the catalog, represented in CPRT as security requirements.
     */
    private List<Control> buildSecurityRequirementControls(Catalog catalog, String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, SECURITY_REQUIREMENT_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            Control control = new Control();
            control.setId(elem.element_identifier);
            control.setClazz(elem.element_type);

            // Some 800-171 security requirements don't have titles (overall security requirement VS a subsection of a security requirement)
            // Don't add titles unless it's human-readable form

            ArrayList<ControlPart> parts = new ArrayList<ControlPart>();
            ControlPart part = buildPartFromElementText(elem, "statement");

            // Security requirements (a,b,...) within overall security requirement
            part.setParts(buildSubcategoryImplementationExamples(catalog, elem.getGlobalIdentifier()));

            parts.add(part);
            
            control.setParts(parts);

            control.addProp(buildLabelProp(elem.element_identifier));

            return control;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private List<ControlPart> buildSubcategoryImplementationExamples(Catalog catalog, String parentId) {
        try {
            return getRelatedElementsBySourceIdWithType(parentId, SECURITY_REQUIREMENT_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
                ControlPart part = buildPartFromElementText(elem, "item");
                part.setId(elem.element_identifier);
                
                part.setParts(buildSubcategoryImplementationExamples(catalog, elem.getGlobalIdentifier()));

                part.addProp(buildLabelProp(elem.element_identifier));
                return part;
            }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        } catch (IllegalArgumentException e) {
            return new ArrayList<ControlPart>();
        }
    }
}
