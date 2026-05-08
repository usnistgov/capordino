package gov.nist.capordino.cprt.conversion.sp_800_172;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gov.nist.capordino.cprt.conversion.AbstractOscalConverter;
import gov.nist.capordino.cprt.conversion.InvalidFrameworkIdentifier;
import gov.nist.capordino.cprt.pojo.CprtElement;
import gov.nist.capordino.cprt.pojo.CprtMetadataVersion;
import gov.nist.capordino.cprt.pojo.CprtRoot;
import gov.nist.secauto.metaschema.model.common.datatype.markup.MarkupLine;
import gov.nist.secauto.metaschema.model.common.datatype.markup.MarkupMultiline;
import gov.nist.secauto.oscal.lib.model.BackMatter.Resource;
import gov.nist.secauto.oscal.lib.model.BackMatter.Resource.Rlink;
import gov.nist.secauto.oscal.lib.model.Catalog;
import gov.nist.secauto.oscal.lib.model.CatalogGroup;
import gov.nist.secauto.oscal.lib.model.Control;
import gov.nist.secauto.oscal.lib.model.ControlPart;
import gov.nist.secauto.oscal.lib.model.Link;
import gov.nist.secauto.oscal.lib.model.Parameter;
import gov.nist.secauto.oscal.lib.model.ParameterGuideline;
import gov.nist.secauto.oscal.lib.model.ParameterSelection;
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
    private final String ENHANCED_SECURITY_REQUIREMENT_ELEMENT_TYPE = "enhanced_security_requirement";
    private final String SORT_ELEMENT_TYPE = "sort";
    private final String TACTIC_ELEMENT_TYPE = "tactic";
    private final String WITHDRAW_REASON_ELEMENT_TYPE = "withdraw_reason";

    private final String DETERMINATION_ELEMENT_TYPE = "determination";
    private final String EXAMINE_ELEMENT_TYPE = "examine";
    private final String INTERVIEW_ELEMENT_TYPE = "interview";
    private final String TEST_ELEMENT_TYPE = "test";
    private final String ODP_ELEMENT_TYPE = "odp";
    private final String ODP_STATEMENT_ELEMENT_TYPE = "odp_statement";
    private final String ODP_TYPE_ELEMENT_TYPE = "odp_type";

    private final String PROJECTION_RELATIONSHIP_TYPE = "projection";
    private final String EXTERNAL_REFERENCE_RELATIONSHIP_TYPE = "external_reference";

    private final String INCORPORATED_INTO_RELATIONSHIP_TYPE = "incorporated_into";
    private final String[] WITHDRAW_RELATIONSHIPS = new String[] {INCORPORATED_INTO_RELATIONSHIP_TYPE, EXTERNAL_REFERENCE_RELATIONSHIP_TYPE};

    private final String DISCUSSION_PREFIX = "D-";

    private Map<String, Link> createdReferences = new HashMap<String, Link>();

    private Map<String, String> aggregateParamsMap = new HashMap<String, String>(); // Map a param to its aggregate param

    /**
     * The URI to use for 800-172-specific props.
     */
    private final URI SP_800_172_URI = URI.create("https://csrc.nist.gov/ns/SP-800-172");

    private final String SP_800_171_r3_IDENTIFIER = "SP_800_171_3_0_0";

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
            .filter(elem -> elem.doc_identifier.equals("SP_800_172_3_0_0"))
            .filter(elem -> elem.element_type.equals(FAMILY_ELEMENT_TYPE))
            .map(elem -> {
                // For each 800-172 family, create an OSCAL group
                CatalogGroup group = new CatalogGroup();
                group.setId("SP_800_172_" + elem.element_identifier);
                group.setClazz(elem.element_type);
                group.setTitle(MarkupLine.fromMarkdown(elem.title));

                // For 800-172 control, create an OSCAL control within this overall family group
                group.setControls(buildSecurityRequirementControls(catalog, elem.getGlobalIdentifier()));

                Property sortProp = buildSortProp(elem.getGlobalIdentifier());
                if (sortProp != null) {
                    group.addProp(sortProp);
                }

                group.addProp(buildLabelProp(elem.title + " (" + elem.element_identifier + ")"));

                return group;
            })
            // Sort based on group id
            // .sorted(Comparator.comparing(CatalogGroup::getId, (id1, id2) -> {
            //     // IDs are Strings but the last part should be numerically compared to be in correct order: 3.1, 3.2, 3.11
            //     String[] idPrefix1 = id1.split("\\.");
            //     String[] idPrefix2 = id2.split("\\.");

            //     if (idPrefix1.length < 2 || idPrefix2.length < 2) {
            //         return id1.compareTo(id2);
            //     }

            //     // First, string-compare the IDs
            //     int comparePrefix = idPrefix1[0].compareTo(idPrefix2[0]);

            //     // If prefix are same, numerically compare the last part
            //     if (comparePrefix == 0) {
            //         return Integer.compare(Integer.parseInt(idPrefix1[1]), Integer.parseInt(idPrefix2[1]));
            //     }

            //     return comparePrefix;
            // }))
            .sorted(Comparator.comparing(group -> 
                group.getProps().stream()
                    // Get the sort-id prop
                    .filter(prop -> prop.getName().equals("sort-id"))
                    // Compare based on value of sort-id like "00001"
                    .map(Property::getValue)
                    .findFirst().orElse("")
            ))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
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

    private String removeCprtPrefix(String text, String prefix) {
        int index = text.indexOf(prefix);
        if (index > -1) {
            return text.substring(0, index) + text.substring(index + prefix.length());
        }

        return text;
    }

    /**
     * Build the second level group of the catalog, represented in CPRT as security requirements.
     */
    // For 800-172 requirement, create an OSCAL control
    private List<Control> buildSecurityRequirementControls(Catalog catalog, String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, SECURITY_REQUIREMENT_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            Control control = new Control();
            control.setId("SP_800_172_" + elem.element_identifier);
            control.setClazz(elem.element_type);
            
            control.addProp(buildProp("sort-id", elem.element_identifier));
            control.addProp(buildLabelProp(elem.element_identifier));


            if (elem.title.equals("Withdrawn")) {
                control.setTitle(createMarkupLineEscaped(elem.element_identifier));
                control.addProp(buildWithdrawnProp());
                // control.setParts(buildEnhancedSecurityRequirementParts(catalog, elem.getGlobalIdentifier()));
                // Create links to the control(s) this withdrawn control points to
                List<Link> links = createWithdrawnLinks(catalog, elem.getGlobalIdentifier());

                for (Link link : links) {
                    control.addLink(link);
                }
            }
            else {
                control.setTitle(createMarkupLineEscaped(elem.title));
                List<Property> protectionStrategyProps = createProtectionStrategyProps(catalog, elem.getGlobalIdentifier());
                for (Property p : protectionStrategyProps) {
                    control.addProp(p);
                }

                // ODPs, assignment parameters
                control.setParams(createParams(elem));

                List<ControlPart> parts = new ArrayList<ControlPart>();
                // ControlPart statementPart = buildPartFromElementText(elem, "statement");
                // statementPart.setId("SP_800_172_" + elem.element_identifier + "_smt");
                // parts.add(statementPart);
                parts.addAll(buildEnhancedSecurityRequirementParts(catalog, elem.getGlobalIdentifier()));

                // CPRT discussion -> OSCAL guidance
                parts.addAll(createGuidancePart(catalog, elem.getGlobalIdentifier()));
                parts.addAll(createAdversaryEffectParts(catalog, elem.getGlobalIdentifier(), ADVERSARY_EFFECT_ELEMENT_TYPE));

                // Assessment objectives
                parts.addAll(createAssessmentObjectiveParts(catalog, elem.element_identifier));

                // Assessment methods and objects
                parts.addAll(createAssessmentMethodParts(catalog, elem.getGlobalIdentifier()));
                
                control.setParts(parts);

                List<Link> links = new ArrayList<Link>();
                // Source Controls
                links.addAll(createSourceControlsLinks(catalog, elem.getGlobalIdentifier()));

                
                control.setLinks(links);

                // For 800-172 security requirement, create OSCAL control
            }
            
            return control;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private List<ControlPart> buildEnhancedSecurityRequirementParts(Catalog catalog, String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, ENHANCED_SECURITY_REQUIREMENT_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            ControlPart statementPart = buildPartFromElementText(elem, "statement");
            statementPart.setId("SP_800_172_" + elem.element_identifier + "_smt");
            statementPart.setParts(buildEnhancedSecurityRequirementSubParts(catalog, elem.getGlobalIdentifier()));
            return statementPart;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private List<ControlPart> buildEnhancedSecurityRequirementSubParts(Catalog catalog, String parentId) {
        try {
            return getRelatedElementsBySourceIdWithType(parentId, ENHANCED_SECURITY_REQUIREMENT_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
                ControlPart part = buildPartFromElementText(elem, "item");
                part.setId("SP_800_172_" + elem.element_identifier);
                
                part.setParts(buildEnhancedSecurityRequirementSubParts(catalog, elem.getGlobalIdentifier()));

                part.addProp(buildLabelProp(elem.element_identifier));
                return part;
            }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        } catch (IllegalArgumentException e) {
            return new ArrayList<ControlPart>();
        }
    }

    private List<ControlPart> createGuidancePart(Catalog catalog, String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, DISCUSSION_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            ControlPart gdn_part = buildPartFromElementText(elem, "guidance");
            gdn_part.setId(getEscapedIdentifier("SP_800_172_" + removeCprtPrefix(elem.element_identifier, DISCUSSION_PREFIX) + "_gdn"));
            gdn_part.setClazz(elem.element_type);
            gdn_part.setNs(SP_800_172_URI);
            return gdn_part;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private List<ControlPart> createAdversaryEffectParts(Catalog catalog, String parentId, String elemType) {
        List<CprtElement> adversaryEffectElements = getRelatedElementsBySourceIdWithType(parentId, elemType, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            return elem;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        List<ControlPart> adversaryEffectParts = new ArrayList<ControlPart>();
        if (adversaryEffectElements.size() > 1) {
            CprtElement SP_800_160_adversary_element = adversaryEffectElements.get(0);
            List<CprtElement> SP_800_160_element_list = getAdversaryEffectElements(SP_800_160_adversary_element, REFERENCE_ITEM_ELEMENT_TYPE);

            CprtElement SP_800_160_element = SP_800_160_element_list.get(0);

            ControlPart topLevelAEPart = new ControlPart();
            topLevelAEPart.setName(elemType);
            topLevelAEPart.setNs(SP_800_172_URI);
            topLevelAEPart.setId(SP_800_160_adversary_element.element_identifier);
            
            if (! createdReferences.containsKey(SP_800_160_element.element_identifier)) {
                Resource SP_800_160_Resource = buildResource(SP_800_160_element);
                Link link = newLinkRel(catalog, SP_800_160_Resource, EXTERNAL_REFERENCE_RELATIONSHIP_TYPE);
                createdReferences.put(SP_800_160_element.element_identifier, link);
                topLevelAEPart.addLink(link);
            }
            else {
                Link link = createdReferences.get(SP_800_160_element.element_identifier);
                topLevelAEPart.addLink(link);
            }
            adversaryEffectParts.add(topLevelAEPart);
            
            for (int adversary_index = 1; adversary_index < adversaryEffectElements.size(); adversary_index++) {
                CprtElement adversaryElement = adversaryEffectElements.get(adversary_index);

                List<CprtElement> effectElementList = getAdversaryEffectElements(adversaryElement, EFFECT_ELEMENT_TYPE);
                ControlPart effectPart = parseAdversaryEffectElement(effectElementList, adversaryElement.element_identifier);
                adversaryEffectParts.add(effectPart);

                List<CprtElement> tacticElementList = getAdversaryEffectElements(adversaryElement, TACTIC_ELEMENT_TYPE);
                ControlPart tacticPart = parseAdversaryEffectElement(tacticElementList, adversaryElement.element_identifier);
                adversaryEffectParts.add(tacticPart);
            }
        }

        return adversaryEffectParts;
    }

    private ControlPart parseAdversaryEffectElement(List<CprtElement> effectElementList, String adversaryElementId) {
        ControlPart effectPart = new ControlPart();
        for (CprtElement effectElement : effectElementList) {
            effectPart = buildAdversaryEffectPart(effectElement, adversaryElementId);

            // impact
            List<CprtElement> impactElementList = getAdversaryEffectElements(effectElement, IMPACT_ELEMENT_TYPE);
            for (CprtElement impactElement : impactElementList) {
                ControlPart impactPart = buildAdversaryEffectPart(impactElement, effectPart.getId());
                effectPart.addPart(impactPart);
            }

            //expected results
            List<CprtElement> expectedResultElementList = getAdversaryEffectElements(effectElement, EXPECTED_RESULT_ELEMENT_TYPE);
            for (int expectedResultIndex = 1; expectedResultIndex <= expectedResultElementList.size(); expectedResultIndex++) {
                CprtElement expectedResultElement = expectedResultElementList.get(expectedResultIndex-1);
                ControlPart expectedResultPart = buildAdversaryEffectPart(expectedResultElement, effectPart.getId());
                expectedResultPart.setId(expectedResultPart.getId() + "-" + expectedResultIndex);
                effectPart.addPart(expectedResultPart);
            }

            //examples
            List<CprtElement> exampleElementList = getAdversaryEffectElements(effectElement, EXAMPLE_ELEMENT_TYPE);
            for (int exampleIndex = 1; exampleIndex <= exampleElementList.size(); exampleIndex++) {
                CprtElement exampleElement = exampleElementList.get(exampleIndex-1);
                ControlPart examplePart = buildAdversaryEffectPart(exampleElement, effectPart.getId());
                examplePart.setId(examplePart.getId() + "-" + exampleIndex);
                effectPart.addPart(examplePart);
            }
        }

        return effectPart;
    }

    private ControlPart buildAdversaryEffectPart(CprtElement elem, String idPrefix) {
        ControlPart part = buildPartFromElementText(elem, elem.element_identifier, SP_800_172_URI);
        part.setClazz(elem.element_type);
        part.setProse(createMarkupMultilineEscaped(elem.title + "\n\n" + elem.text));
        part.setId(idPrefix + "-" + elem.element_type);
        return part;
    }

    private List<CprtElement> getAdversaryEffectElements(CprtElement adversaryElement, String elemType) {
        return getRelatedElementsBySourceIdWithType(adversaryElement.getGlobalIdentifier(), elemType).map(elem -> {
            return elem;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private List<Property> createProtectionStrategyProps(Catalog catalog, String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, PROTECTION_STRATEGY_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            Property ps_prop = buildProp(PROTECTION_STRATEGY_ELEMENT_TYPE, elem.element_identifier, SP_800_172_URI.toString());
            ps_prop.setRemarks(createMarkupMultilineEscaped(elem.title));
            return ps_prop;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    // Build RLinks to references to 800-53 controls, represented in CPRT site as Source Controls (no element type, external reference relationship type)
    private List<Link> createSourceControlsLinks(Catalog catalog, String parentId) {
        List<String> source_control_identifiers = getDestinationIdWithType(parentId, EXTERNAL_REFERENCE_RELATIONSHIP_TYPE).map(identifier -> {
            return identifier;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        List<Link> source_controls_links = new ArrayList<Link>();

        for (String source_control_identifier : source_control_identifiers) {
            if (! createdReferences.containsKey(source_control_identifier)) {
                Resource source_control_resource = new Resource();
                source_control_resource.setTitle(MarkupLine.fromMarkdown(source_control_identifier));
                Rlink rlink = new Rlink();
                rlink.setHref(URI.create("https://csrc.nist.gov/projects/cprt/catalog#/cprt/framework/version/SP_800_53_5_2_0/home?element=" + source_control_identifier));
                source_control_resource.addRlink(rlink);

                Link link = newLinkRel(catalog, source_control_resource, EXTERNAL_REFERENCE_RELATIONSHIP_TYPE);
                createdReferences.put(source_control_identifier, link);

                source_controls_links.add(link);
            }
            else {
                source_controls_links.add(createdReferences.get(source_control_identifier));
            }
        }

        return source_controls_links;
    }

    private List<ControlPart> createAssessmentObjectiveParts(Catalog catalog, String parentId) {
        List<ControlPart> objective_parts = getRelatedElementsByType(DETERMINATION_ELEMENT_TYPE, parentId).map(elem -> {
            ControlPart part =  buildAssessmentObjectivePart(elem);
            return part;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        return objective_parts;
    }
   
    // Assessment methods are EXAMINE, INTERVIEW, TEST
    private List<ControlPart> createAssessmentMethodParts(Catalog catalog, String parentId) {

        ArrayList<ControlPart> examine_parts = getRelatedElementsBySourceIdWithType(parentId, EXAMINE_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            return buildAssessmentMethodPart(elem, ";", "[SELECT FROM: ", "]", "http://csrc.nist.gov/ns/rmf");
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        ArrayList<ControlPart> interview_parts = getRelatedElementsBySourceIdWithType(parentId, INTERVIEW_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            return buildAssessmentMethodPart(elem, ";", "[SELECT FROM: ", "]", "http://csrc.nist.gov/ns/rmf");
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        ArrayList<ControlPart> test_parts = getRelatedElementsBySourceIdWithType(parentId, TEST_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            return buildAssessmentMethodPart(elem, ";", "[SELECT FROM: ", "]", "http://csrc.nist.gov/ns/rmf");
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        examine_parts.addAll(interview_parts);
        examine_parts.addAll(test_parts);

        return examine_parts;
    }

    private List<Parameter> createParams(CprtElement parent) {
        // Get all assessment objectives associated with this control
        // Then get all ODPs in the assessment objective
        String parentId = parent.element_identifier;
        List<String> odp_identifiers = getRelatedElementsByType(DETERMINATION_ELEMENT_TYPE, parentId).map(elem -> {
            return get_odp_identifiers(elem.text, "<(.+?):?\\s+.+?>");
        }).collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll); // Flatten the list of param lists

        String parent_doc_identifier = parent.doc_identifier;

        // ODPs within ODPs
        List<String> additional_odps = new ArrayList<String>();
        for (String odp_identifier : odp_identifiers) {
            String odp_global_identifier = parent_doc_identifier + ":" + odp_identifier;
            
            List<String> odps_within_odp = getRelatedElementsBySourceIdWithType(odp_global_identifier, ODP_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
                return elem.element_identifier;
            }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

            additional_odps.addAll(odps_within_odp);
        }
        odp_identifiers.addAll(additional_odps);

        // LinkedHashSet to keep order and account for same ODPs in different objectives
        Set<String> odp_identifiers_set = new LinkedHashSet<String>(odp_identifiers);
        List<Parameter> odp_params = buildAggregateParams(odp_identifiers_set, parentId);
        odp_params.addAll(buildParams(parent_doc_identifier, odp_identifiers_set, ODP_TYPE_ELEMENT_TYPE));
        
        
        return odp_params;
    }

    protected List<Parameter> buildAggregateParams(Set<String> odp_identifiers, String parentId) {
        List<Parameter> aggregateParams = new ArrayList<Parameter>();

        // Map<String, List<CprtElement>> odpStatementMatches = new LinkedHashMap<String, List<CprtElement>>();
        // // build a hashmap with key odp statement and value list of odps that have that statement
        // for (String odp_identifier : odp_identifiers) {
        //     String odp_global_identifier = "SP_800_172_3_0_0" + ":" + odp_identifier;
        //     // Get the ODP element associated with the ODP id
        //     CprtElement odp_element = cprtRoot.getElementById(odp_global_identifier);
        //     // Get the ODP statement element associated with this ODP
        //     CprtElement odp_statement_element = cprtRoot.getElementById("SP_800_172_3_0_0:OS-" + odp_identifier.toLowerCase());

        //     if (odp_element == null || odp_statement_element == null) {
        //         System.out.println(odp_global_identifier);
        //         continue;
        //     }

        //     String odpStatement = odp_statement_element.text;

        //     if (odpStatementMatches.containsKey(odpStatement)) {
        //         // Also check if ODP title is contained in odp statement (protects against CPRT bug where unrelated ODPs have same statement)
        //         if (odpStatement.contains(odp_element.title) || odp_element.title.equals("SELECTED PARAMETER VALUE(S)") || odp_element.title.isBlank()) {
        //             odpStatementMatches.get(odpStatement).add(odp_element);
        //         }
        //     } else {
        //         List<CprtElement> odpMatches = new ArrayList<CprtElement>();
        //         odpMatches.add(odp_element);
        //         odpStatementMatches.put(odpStatement, odpMatches);
        //     }
        // }

        // int prmId = 1;
        // // build an aggregate param for each pair in the hashmap, only if there are multiple ODPs that match to same statement
        // for (Map.Entry<String, List<CprtElement>> entry : odpStatementMatches.entrySet()) {
        //     String odpStatement = entry.getKey();
        //     List<CprtElement> odpMatches = entry.getValue();

        //     if (odpMatches.size() > 1) {
        //         // Create an aggregate param for ODPs that share the same statement
        //         Parameter aggregateParam = new Parameter();

        //         aggregateParam.setId("SP_800_172_A." + parentId + "_prm_" + prmId);
        //         prmId++;

        //         for (CprtElement odpMatch : odpMatches) {
        //             String odp_identifier = odpMatch.element_identifier;
        //             aggregateParam.addProp(buildProp("aggregates", odp_identifier, "http://csrc.nist.gov/ns/rmf"));

        //             aggregateParamsMap.put(odp_identifier, aggregateParam.getId());
        //         }
        //         aggregateParam.setLabel(createMarkupLineEscaped(odpStatement));

        //         aggregateParams.add(aggregateParam);
        //     }
        // }

        return aggregateParams;
    }

    @Override
    // Builds a OSCAL Param for a given ODP id
    protected Parameter buildParam(String odp_identifier, String doc_identifier, String odp_type_element_type) {
        // Convert to global identifier, because of how elements map stores elements
        String odp_global_identifier = doc_identifier + ":" + odp_identifier;

        // Get the ODP element associated with the ODP id
        CprtElement odp_element = cprtRoot.getElementById(odp_global_identifier);
        // Get the ODP statement element associated with this ODP
        CprtElement odp_statement_element = cprtRoot.getElementById(doc_identifier + ":" + "OS-" + odp_identifier.toLowerCase());

        // Create a Parameter object
        Parameter odp_param = new Parameter();

        if (odp_element == null || odp_statement_element == null) {
            System.out.println(odp_global_identifier);
        }
        else {
        
            odp_param.addProp(buildLabelProp(odp_identifier));
            odp_param.setLabel(createMarkupLineEscaped(odp_element.title));

            // Build param based on type
            List<String> odp_types = getRelatedElementsBySourceIdWithType(odp_global_identifier, odp_type_element_type).map(elem -> {
                return elem.element_identifier;
            }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

            String odp_type = odp_types.get(0);
            if (odp_type == null) {
                throw new IllegalArgumentException("ODP " + odp_global_identifier + "has no ODP type");
            }
            
            if (odp_type.equals("single_entry")) {
                // Assignment type param
                ParameterGuideline odp_param_guideline = new ParameterGuideline();
                odp_param_guideline.setProse(createMarkupMultilineEscaped(odp_element.text));
                odp_param.addGuideline(odp_param_guideline);

                if (odp_statement_element != null) {
                    odp_param.setUsage(createMarkupMultilineEscaped(odp_statement_element.text));
                }
            }
            else {
                // Selection type param
                ParameterSelection odp_param_selection = new ParameterSelection();
                if (odp_type.equals("multi_select")) {
                    odp_param_selection.setHowMany("one-or-more");
                }
                else if (odp_type.equals("single_select")) {
                    odp_param_selection.setHowMany("one");
                }
                

                List<String> odp_param_choices = parseParamChoices(odp_statement_element.text, odp_element.text, odp_global_identifier);

                for (String choice : odp_param_choices) {
                    odp_param_selection.addChoice(createMarkupLineEscaped(choice));
                }
                odp_param.setSelect(odp_param_selection);
            }
            
            // Param id must be escaped to be consistent with how params are inserted in controls and assessment objectives, which require escaped square brackets
            // String escaped_odp_identifier = escapeSquareBracketsWithParentheses(odp_identifier);
            String escaped_odp_identifier = escapeSquareBracketsWithPeriods(odp_identifier);
            odp_param.setId("SP_800_172_" + escaped_odp_identifier);
        }

        return odp_param;
    }

    @Override
    protected String parseODPInElementText(CprtElement element) {
        String text = element.text;

        // ODPs in controls are implicit. Get the assessment objectives related to this control, because ODPS are explicitly stated in assessment objectives.
        List<CprtElement> related_assessment_objectives = getRelatedElementsBySourceIdWithType(element.getGlobalIdentifier(), DETERMINATION_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            return elem;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        Set<CprtElement> all_related_odps = new LinkedHashSet<CprtElement>();

        // For the assessment objectives related to this control, get the related ODP(s)
        for (CprtElement related_assessment_objective : related_assessment_objectives) {
            
            List<CprtElement> related_odps = getRelatedElementsBySourceIdWithType(related_assessment_objective.getGlobalIdentifier(), ODP_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
                return elem;
            }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

            all_related_odps.addAll(related_odps);
        }

        // Replaced implicitly stated ODP with <insert odp_id>
        if (!all_related_odps.isEmpty()) {
            text = insertImplicitParamsWithElements(text, new ArrayList<CprtElement>(all_related_odps));
        }
        
        return text;
    }


    // Replace <ODP_id> with <insert> in text
    // Use when ODP id is explicitly stated: asssessment objective
    @Override
    protected String insertExplicitParams(String text) {
        List<String> odp_identifiers = get_odp_identifiers(text, "<(.+?):?\\s+.+?>");
        
        // Replace ODP with insert param
        for (String odp_identifier : odp_identifiers) {
            String insert = String.format("<insert type=\"param\" id-ref=\"%s\" />", "SP_800_172_" + odp_identifier) ;
            String escaped_odp_identifier = escapeSquareBracketsWithBackslashes(odp_identifier);

            // Only replace the ODP that matches this identifier
            String specific_odp_pattern = "<" + escaped_odp_identifier + ":?\\s+.+?>"; 
            text = text.replaceAll(specific_odp_pattern, insert);
        }

        return text;
    }

    
    // Replace [Selection: ...] or [Assignment: ...] with <insert> in text
    // Use when ODP id is implicit: control items, ODP contained inside another ODP
    protected String insertImplicitParamsWithElements(String text, List<CprtElement> related_odps) {
        // After Selection (one or more): can't use greedy regex that matches everything up to the last ] of the text.
        // The last ] of the text is not necessarily the closing bracket of the selection block. It may be another assignment/selection block that comes after.
        // For all text that comes after selection block, match to two possible options
        // 1. Any character that isn't a [ or ]
        // 2. If character is [, then match up to the next encountered ]
        // This captures assignment blocks within selection blocks, and stops if it encounters a ] without a [ that comes before. This signals the end of the selection block.
        // NOTE: Assumes maximum of 1 nested level. No assignment blocks within a selection block within a selection block.

        String odp_multi_select_pattern = "(\\[Selection:?\\s+\\((?:[^)]+)\\):\\s+(?:[^\\[\\]]|\\[[^\\]]*\\])+\\])";
        // Need non-greedy regex for minimum possible match, otherwise it matches multiple ODPs as one.
        String odp_assign_pattern = "(\\[Assignment:\\s+.+?(?:\\]|$))";

        // Replace ODP with insert param
        // NOTE: assumes ODPs are non-repeating and in order in the text
        for (CprtElement related_odp : related_odps) {
            String odp_identifier = related_odp.element_identifier;

            String insert;
            if (aggregateParamsMap.containsKey(odp_identifier)) {
                insert = String.format("<insert type=\"param\" id-ref=\"%s\" />", aggregateParamsMap.get(odp_identifier)); // Assumes that params that are part of an aggregate param are not referenced individually in a statement
            }
            else {
                insert = String.format("<insert type=\"param\" id-ref=\"%s\" />", "SP_800_172_" + odp_identifier);
            }

            // replaceFirst instead of replaceAll, because there may be multiple assignments that match due to same ODP statement, yet are different ODPs
            // Match multi select pattern first, so any "assignment" type param within "select" type param are incorporated
            Pattern multi_select_pattern = Pattern.compile(odp_multi_select_pattern);
            Matcher multi_select_matcher = multi_select_pattern.matcher(text);
            
            // Replace either a select or assignment pattern
            if (multi_select_matcher.find()) {
                text = text.replaceFirst(odp_multi_select_pattern, insert);
            }
            else {
                text = text.replaceFirst(odp_assign_pattern, insert);
            }
        }

        return text;
    }

    // Parse choices in a multi_select type ODP
    protected List<String> parseParamChoices(String odp_statement_text, String odp_text, String topLevelODPIdentifier) {
        // For multi_select ODPs, choices are in odp_statement_text but nested ODP ids are NOT in odp_text, as in 800-171. Find the assignment ODPs that have relationships with the top level selection ODP.
        List<String> nested_odps = getRelatedElementsBySourceIdWithType(topLevelODPIdentifier, ODP_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            return elem.element_identifier;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        String choices = insertImplicitParams(odp_statement_text, nested_odps);
        String[] choices_list = choices.split(";");

        return Arrays.asList(choices_list);
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

            if (relationType.equals(EXTERNAL_REFERENCE_RELATIONSHIP_TYPE) && dest_withdraw_identifiers.size() > 0) {
                if (! createdReferences.containsKey(SP_800_171_r3_IDENTIFIER)) {
                     Resource SP_800_171_r3_resource = new Resource();
                    SP_800_171_r3_resource.setTitle(MarkupLine.fromMarkdown(SP_800_171_r3_IDENTIFIER));
                    Rlink rlink = new Rlink();
                    rlink.setHref(URI.create("https://csrc.nist.gov/pubs/sp/800/171/r3/final"));
                    SP_800_171_r3_resource.addRlink(rlink);

                    Link link = newLinkRel(catalog, SP_800_171_r3_resource, EXTERNAL_REFERENCE_RELATIONSHIP_TYPE);
                    createdReferences.put(SP_800_171_r3_IDENTIFIER, link);

                    links.add(link);
                }
                else {
                    Link link = createdReferences.get(SP_800_171_r3_IDENTIFIER);
                    links.add(link);
                }
            }

            links.addAll(createLinks(dest_withdraw_identifiers, relationType));
        }

        return links;
    }
}
