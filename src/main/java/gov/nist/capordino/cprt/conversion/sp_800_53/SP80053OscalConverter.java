package gov.nist.capordino.cprt.conversion.sp_800_53;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import gov.nist.capordino.cprt.conversion.AbstractOscalConverter;
import gov.nist.capordino.cprt.conversion.InvalidFrameworkIdentifier;
import gov.nist.capordino.cprt.pojo.CprtElement;
import gov.nist.capordino.cprt.pojo.CprtMetadataVersion;
import gov.nist.capordino.cprt.pojo.CprtRelationship;
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

    private final String SP_800_53_CLASS = "sp800-53";
    private final String SP_800_53_A_CLASS = "sp800-53a";
    private final String SP_800_53_ENHANCEMENT_CLASS = "SP800-53-enhancement";

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
            control.setClazz(SP_800_53_CLASS);
            control.setTitle(MarkupLine.fromMarkdown(elem.title));

            List<ControlPart> parts = new ArrayList<ControlPart>();

            // Control level doesn't contain information about whether this control is withdrawn
            // Must go down one more level to Control Statement
            List<CprtElement> topControlStatements = getElementsSafely(elem.getGlobalIdentifier(), CONTROL_STATEMENT_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE);

            for (CprtElement topControlStatement : topControlStatements) {
                
                // If withdrawn, build a prop
                if (topControlStatement.text.equals("Withdrawn")) {
                    control.addProp(buildWithdrawnProp());

                    // Create links to the control(s) this withdrawn control points to
                    List<Link> links = createWithdrawnLinks(catalog, elem.getGlobalIdentifier());

                    for (Link link : links) {
                        control.addLink(link);
                    }
                }
                // If not withdrawn, build a statement part
                else {
                    // ODPs, assignment parameters
                    // control.setParams(createParams(elem));

                    // Use topControlStatement instead of elem to skip one level in the tree
                    // If this is not done, it creates an extra ControlPart level in the catalog
                    ControlPart statementPart = buildPartFromElementText(topControlStatement, "statement");
                    statementPart.setId(elem.element_identifier + "_smt"); 
                    statementPart.setParts(buildControlStatementParts(catalog, topControlStatement.getGlobalIdentifier()));
                    parts.add(statementPart);

                    // CPRT discussion -> OSCAL guidance
                    parts.addAll(createGuidancePart(catalog, elem.getGlobalIdentifier()));

                    // Assessment objectives
                    List<ControlPart> subObjectives = createAssessmentObjectiveParts(catalog, topControlStatement.getGlobalIdentifier());
                    if (subObjectives.size() == 1) {
                        parts.addAll(subObjectives);
                    }
                    else {
                        ControlPart topObjective = buildAssessmentObjectivePart(topControlStatement);
                        topObjective.setId(elem.element_identifier + "_obj");
                        topObjective.setProse(null);
                        topObjective.setParts(subObjectives);
                        Property prop = buildLabelProp(elem.element_identifier);
                        prop.setClazz(SP_800_53_A_CLASS);
                        topObjective.addProp(prop);
                        parts.add(topObjective);
                    }
                    
                    // Assessment methods and objects
                    // parts.addAll(createAssessmentMethodParts(catalog, elem.getGlobalIdentifier()));



                    // List<Link> links = new ArrayList<Link>();
                    // Source Controls
                    // links.addAll(createSourceControlsLinks(catalog, elem.getGlobalIdentifier()));
                    // Supporting Publications
                    // links.addAll(createSupportingPublicationsLinks(catalog, elem.getGlobalIdentifier()));
                    
                    // control.setLinks(links);
                }
            }

            control.setParts(parts);

            // 800-53 control enhancements
            // group.setControls(buildControls(catalog, elem.getGlobalIdentifier()));

            
            Property sortProp = buildSortProp(elem.getGlobalIdentifier());
            if (sortProp != null) {
                control.addProp(sortProp);
            }

            control.addProp(buildLabelProp(elem.title + " (" + elem.element_identifier + ")"));

            return control;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private List<ControlPart> buildControlStatementParts(Catalog catalog, String parentId) {
        try {
            return getRelatedElementsBySourceIdWithType(parentId, CONTROL_STATEMENT_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
                ControlPart part = buildPartFromElementText(elem, "item");
                part.setId(elem.element_identifier.substring(4)); // Remove CST_ prefix
                
                // Recursively call to get all sub parts
                part.setParts(buildControlStatementParts(catalog, elem.getGlobalIdentifier()));

                part.addProp(buildLabelProp(elem.title));
                return part;
            }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        } catch (IllegalArgumentException e) {
            return new ArrayList<ControlPart>();
        }
    }

    // Get the destination identifier of a given withdraw_reason element (get the control a withdrawn control points to)
    private List<String> getDestWithdrawIdentifiers(String parentId, String relationType) {
        List<String> dest_withdraw_identifiers = getDestinationIdWithType(parentId, relationType).map(identifier -> {
            return "#" + identifier;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        return dest_withdraw_identifiers;
        
    }

    // Create links to the control(s) a given withdrawn control points to
    private List<Link> createWithdrawnLinks(Catalog catalog, String parentId) {
        // Get the withdraw_reason element associated with the given element
        List<String> withdraw_identifiers = getRelatedElementsBySourceIdWithType(parentId, WITHDRAW_REASON_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            return elem.getGlobalIdentifier();
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
    
    // Replace <ODP_id> with <insert> in text
    // Use when ODP id is explicitly stated: asssessment objective
    @Override
    protected String insertExplicitParams(String text) {
        List<String> odp_identifiers = get_odp_identifiers(text, "<(.+?) .+?>");
        
        // Replace ODP with insert param
        for (String odp_identifier : odp_identifiers) {
            String insert = String.format("<insert type=\"param\" id-ref=\"%s\" />", odp_identifier);
            String escaped_odp_identifier = escapeSquareBracketsWithBackslashes(odp_identifier);

            // Only replace the ODP that matches this identifier
            String specific_odp_pattern = "<" + escaped_odp_identifier + " .+?>";
            text = text.replaceAll(specific_odp_pattern, insert);
        }

        return text;
    }

    private List<ControlPart> createGuidancePart(Catalog catalog, String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, DISCUSSION_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            ControlPart gdn_part = buildPartFromElementText(elem, "guidance");
            gdn_part.setId(getEscapedIdentifier(elem.element_identifier.substring(2) + "_gdn"));
            return gdn_part;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    protected String removeSquareBrackets(String input) {
        return input.replaceAll("\\[", "").replaceAll("\\]", "");
    }


    private List<ControlPart> createAssessmentObjectiveParts(Catalog catalog, String parentId) {
        List<CprtElement> elements = getElementsSafely(parentId, DETERMINATION_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE);
        if(! elements.isEmpty()) {
            return elements.stream().map(elem -> {
                ControlPart part = buildAssessmentObjectivePart(elem);
                part.setId(removeSquareBrackets(elem.element_identifier.substring(3) + "_obj"));
                Property prop = buildLabelProp(elem.element_identifier.substring(3));
                prop.setClazz(SP_800_53_A_CLASS);
                part.addProp(prop);


                // Add assessment-for link to the subcontrol item this objective assesses
                part.setLinks(new ArrayList<Link>());
                List<CprtRelationship> assessment_for_relationships = cprtRoot.getRelationshipsByDestinationElementId(elem.getGlobalIdentifier());
                for (CprtRelationship assessment_for_relationship : assessment_for_relationships) {
                    part.addLink(createLink("#" + assessment_for_relationship.source_element_identifier.substring(4), "assessment-for"));
                }

                return part;
            }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        else {
            List<CprtElement> nextControlStatements = getElementsSafely(parentId, CONTROL_STATEMENT_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE);

            List<ControlPart> subObjectives = new ArrayList<ControlPart>();

            for (CprtElement nextControlStatement : nextControlStatements) {
                // Recursively call to get all sub parts
                // Build a tree of assessment objectives
                ControlPart part = buildAssessmentObjectivePart(nextControlStatement);
                part.setId(nextControlStatement.element_identifier.substring(4) + "_obj");
                part.setProse(null);
                Property prop = buildLabelProp(nextControlStatement.element_identifier.substring(4));
                prop.setClazz(SP_800_53_A_CLASS);
                part.addProp(prop);
                part.setParts(createAssessmentObjectiveParts(catalog, nextControlStatement.getGlobalIdentifier()));
                subObjectives.add(part);
            }

            return subObjectives;
        }
    }

    private List<CprtElement> getElementsSafely(String parentId, String elemType, String relationType) {
        try {
            return getRelatedElementsBySourceIdWithType(parentId, elemType, relationType).map(elem -> {
                return elem;
            }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        } catch (Exception e) {
            
            return new ArrayList<CprtElement>();
        }
    }
}