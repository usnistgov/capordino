package gov.nist.capordino.cprt.conversion.sp_800_172;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final String DISCUSSION_PREFIX = "D-";

    private Map<String, Link> createdReferences = new HashMap<String, Link>();

    /**
     * The URI to use for 800-172-specific props.
     */
    private final URI SP_800_172_URI = URI.create("https://csrc.nist.gov/ns/SP-800-172");

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
            .sorted(Comparator.comparing(CatalogGroup::getId))
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
            List<Property> protectionStrategyProps = createProtectionStrategyProps(catalog, elem.getGlobalIdentifier());
            for (Property p : protectionStrategyProps) {
                control.addProp(p);
            }

            List<ControlPart> parts = new ArrayList<ControlPart>();
            ControlPart statementPart = buildPartFromElementText(elem, "statement");
            statementPart.setId(elem.element_identifier + "_smt"); 
            parts.add(statementPart);

            // CPRT discussion -> OSCAL guidance
            parts.addAll(createGuidancePart(catalog, elem.getGlobalIdentifier()));
            parts.addAll(createAdversaryEffectParts(catalog, elem.getGlobalIdentifier()));

            
            control.setParts(parts);

            List<Link> links = new ArrayList<Link>();
            // Source Controls
            links.addAll(createSourceControlsLinks(catalog, elem.getGlobalIdentifier()));

            
            control.setLinks(links);

            // For 800-172 security requirement, create OSCAL control
            
            return control;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private List<ControlPart> createGuidancePart(Catalog catalog, String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, DISCUSSION_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            ControlPart gdn_part = buildPartFromElementText(elem, "guidance");
            gdn_part.setId(getEscapedIdentifier(removeCprtPrefix(elem.element_identifier, DISCUSSION_PREFIX) + "_gdn"));
            return gdn_part;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private List<ControlPart> createAdversaryEffectParts(Catalog catalog, String parentId) {
        List<CprtElement> adversaryEffectElements = getRelatedElementsBySourceIdWithType(parentId, ADVERSARY_EFFECT_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            return elem;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        List<ControlPart> adversaryEffectParts = new ArrayList<ControlPart>();
        if (adversaryEffectElements.size() > 1) {
            CprtElement SP_800_160_adversary_element = adversaryEffectElements.get(0);
            List<CprtElement> SP_800_160_element_list = getAdversaryEffectElements(SP_800_160_adversary_element, REFERENCE_ITEM_ELEMENT_TYPE);

            CprtElement SP_800_160_element = SP_800_160_element_list.get(0);

            ControlPart topLevelAEPart = new ControlPart();
            topLevelAEPart.setName(ADVERSARY_EFFECT_ELEMENT_TYPE);
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
            
            for (int i = 1; i < adversaryEffectElements.size(); i++) {
                // effect
               
                CprtElement adversaryElement = adversaryEffectElements.get(i);
                List<CprtElement> effectElementList = getAdversaryEffectElements(adversaryElement, EFFECT_ELEMENT_TYPE);

                for (CprtElement effectElement : effectElementList) {
                    ControlPart effectPart = buildAdversaryEffectPart(effectElement, adversaryElement);

                    // impact
                    List<CprtElement> impactElementList = getAdversaryEffectElements(effectElement, IMPACT_ELEMENT_TYPE);
                    for (CprtElement impactElement : impactElementList) {
                        ControlPart impactPart = buildAdversaryEffectPart(impactElement, adversaryElement);
                        effectPart.addPart(impactPart);
                    }

                    //expected results (examples)

                    adversaryEffectParts.add(effectPart);
                }


                // tactic
                // List<CprtElement> tacticElementList = getAdversaryEffectElements(adversaryElement, TACTIC_ELEMENT_TYPE);
                // for (int j = 0; j < tacticElementList.size(); j++) {
                //     CprtElement tacticElement = tacticElementList.get(j);
                //     ControlPart tacticPart = buildAdversaryEffectPart(tacticElement, adversaryElement);
                //     tacticPart.setId(tacticPart.getId() + "-" + j);
                //     adversaryEffectParts.add(tacticPart);
                // }
            }
        }

        return adversaryEffectParts;
    }

    private ControlPart buildAdversaryEffectPart(CprtElement elem, CprtElement adversaryElement) {
        ControlPart part = buildPartFromElementText(elem, elem.element_identifier, SP_800_172_URI);
        part.setClazz(elem.element_type);
        part.setProse(createMarkupMultilineEscaped(elem.title + "\n\n" + elem.text));
        part.setId(adversaryElement.element_identifier + "-" + elem.element_type);
        return part;
    }

    private List<CprtElement> getAdversaryEffectElements(CprtElement adversaryElement, String elemType) {
        return getRelatedElementsBySourceIdWithType(adversaryElement.getGlobalIdentifier(), elemType).map(elem -> {
            return elem;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private List<Property> createProtectionStrategyProps(Catalog catalog, String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, PROTECTION_STRATEGY_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            Property ps_prop = buildProp(PROTECTION_STRATEGY_ELEMENT_TYPE, elem.title, SP_800_172_URI.toString());
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
                rlink.setHref(URI.create("https://csrc.nist.gov/projects/cprt/catalog#/cprt/framework/version/SP_800_53_5_1_1/home?element=" + source_control_identifier));
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
   
}
