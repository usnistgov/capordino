package gov.nist.capordino.cprt.conversion.sp_800_66;

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

public class SP80066OscalConverter extends AbstractOscalConverter {
    protected void assertFrameworkIdentifier() throws InvalidFrameworkIdentifier {
        if (!cprtMetadataVersion.frameworkIdentifier.equals("SP800_66")) {
            throw new InvalidFrameworkIdentifier("SP800_66", cprtMetadataVersion.frameworkIdentifier);
        }
    }

    public SP80066OscalConverter(CprtMetadataVersion cprtMetadataVersion, CprtRoot cprtRoot) throws InvalidFrameworkIdentifier {
        super(cprtMetadataVersion, cprtRoot);
        assertFrameworkIdentifier();
    }
    
    public SP80066OscalConverter(CprtMetadataVersion cprtMetadataVersion) throws IOException, InterruptedException, InvalidFrameworkIdentifier {
        super(cprtMetadataVersion);
        assertFrameworkIdentifier();
    }

    public SP80066OscalConverter(String frameworkVersionIdentifier) throws IOException, InterruptedException, InvalidFrameworkIdentifier {
        super(frameworkVersionIdentifier);
        assertFrameworkIdentifier();
    }

    private final String DESCRIPTION_ELEMENT_TYPE = "description";
    private final String FOOTNOTE_ELEMENT_TYPE = "footnote";
    private final String IMP_SPEC_ELEMENT_TYPE = "imp_spec";
    private final String KEY_ACTIVITY_ELEMENT_TYPE = "key_activity";
    private final String PUB_CROSSWALK_ELEMENT_TYPE = "pub_crosswalk";
    private final String SAMPLE_QUESTION_ELEMENT_TYPE = "sample_question";
    private final String SECURITY_RULE_ELEMENT_TYPE = "security_rule";
    private final String STANDARD_ELEMENT_TYPE = "standard";
    private final String TYPE_ELEMENT_TYPE = "type";

    private final String PROJECTION_RELATIONSHIP_TYPE = "projection";

    private Map<String, Link> createdPubCrosswalks = new HashMap<String, Link>();

    /**
     * The URI to use for 800-66-specific props.
     */
    private final URI SP_800_66_URI = URI.create("https://csrc.nist.gov/ns/SP-800-66");

    /*
     * security rule -> standard
     * 
     * standard -> key activity
     * standard -> pub crosswalk
     * 
     * key activity -> imp spec
     * key activity -> sample question
     * key activity -> description
     * 
     * description -> description (subparts of same description)
     * sample question -> sample question (subparts of same sample question)
     * 
     * imp spec -> type (of imp spec)
     * 
     * description -> footnote
     * sample question -> footnote
     * key activity -> footnote
     * standard -> footnote
     * 
     */

    @Override
    protected void hydrateCatalog(Catalog catalog) {
        catalog.setGroups(buildSecurityRuleGroups(catalog));
    }

    /**
     * Build the top level group of the catalog, represented in CPRT as security rules.
     */
    private List<CatalogGroup> buildSecurityRuleGroups(Catalog catalog) {
        // Recursively go down tree of elements, to build family groups
        return cprtRoot.getElements().stream()
            .filter(elem -> elem.element_type.equals(SECURITY_RULE_ELEMENT_TYPE))
            .map(elem -> {
                // For each 800-66 security rule, create an OSCAL group
                CatalogGroup group = new CatalogGroup();
                group.setId("SP_800_66-" + elem.element_identifier);
                group.setClazz(elem.element_type);
                group.setTitle(MarkupLine.fromMarkdown(elem.title));

                group.addPart(buildPartFromElementText(elem, "overview"));
                // For 800-66 standard, create an OSCAL control within this overall security rule group
                group.setGroups(buildStandardGroups(catalog, elem.getGlobalIdentifier()));
                


                group.addProp(buildLabelProp(elem.title + " (" + elem.element_identifier + ")"));

                return group;
            })
            .sorted(Comparator.comparing(CatalogGroup::getId))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }


    /**
     * Build the second level group of the catalog, represented in CPRT as standards.
     */
    // For 800-66 standard, create an OSCAL group
    private List<CatalogGroup> buildStandardGroups(Catalog catalog, String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, STANDARD_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            CatalogGroup standardGroup = new CatalogGroup();
            standardGroup.setId("SP_800_66-" + elem.element_identifier);
            standardGroup.setClazz(elem.element_type);
            standardGroup.setTitle(createMarkupLineEscaped(elem.title));

            standardGroup.addPart(buildPartFromElementText(elem, "instruction"));            

            // Key activity
            standardGroup.setControls(buildKeyActivityControls(catalog, elem.getGlobalIdentifier()));

            // Pub crosswalk
            List<Link> links = new ArrayList<Link>();
            links.addAll(createPubCrosswalkLinks(catalog, elem.getGlobalIdentifier()));
            standardGroup.setLinks(links);

            standardGroup.addProp(buildLabelProp(elem.title + " (" + elem.element_identifier + ")"));
            
            return standardGroup;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Build the third level control of the catalog, represented in CPRT as key activities.
     */
    private List<Control> buildKeyActivityControls(Catalog catalog, String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, KEY_ACTIVITY_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            Control control = new Control();
            control.setId(elem.element_identifier);
            control.setClazz(elem.element_type);
            control.setTitle(createMarkupLineEscaped(elem.title));

            
            List<ControlPart> parts = new ArrayList<ControlPart>();

            // Description within a key activity
            ControlPart statementPart = buildPartFromElementText(elem, "statement");
            statementPart.setParts(buildKeyActivityParts(catalog, elem.getGlobalIdentifier(), DESCRIPTION_ELEMENT_TYPE));
            parts.add(statementPart);

            // Sample questions within a key activity
            ControlPart guidancePart = buildPartFromElementText(elem, "guidance");
            guidancePart.setParts(buildKeyActivityParts(catalog, elem.getGlobalIdentifier(), SAMPLE_QUESTION_ELEMENT_TYPE));
            parts.add(guidancePart);

            parts.addAll(buildImpSpecParts(catalog, elem.getGlobalIdentifier()));

            control.setParts(parts);

            return control;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private List<ControlPart> buildKeyActivityParts(Catalog catalog, String parentId, String elemType) {
        try {
            return getRelatedElementsBySourceIdWithType(parentId, elemType, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
                ControlPart part = buildPartFromElementText(elem, elemType);
                part.setId(elem.element_identifier);
                // part.setClazz(elem.element_type);
                part.setNs(SP_800_66_URI);
                part.setParts(buildKeyActivityParts(catalog, elem.getGlobalIdentifier(), elemType));

                return part;
            }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        } catch (IllegalArgumentException e) {
            return new ArrayList<ControlPart>();
        }
    }

    private List<ControlPart> buildImpSpecParts(Catalog catalog, String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, IMP_SPEC_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            ControlPart part = buildPartFromElementText(elem, IMP_SPEC_ELEMENT_TYPE);
            part.setId("SP_800_66-" + elem.element_identifier);
            // part.setClazz(elem.element_type);
            part.setNs(SP_800_66_URI);
            part.setTitle(createMarkupLineEscaped(elem.title));

            List<Property> impSpecProps = buildImpSpecTypes(catalog, elem.getGlobalIdentifier());
            part.setProps(impSpecProps);

            return part;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private List<Property> buildImpSpecTypes(Catalog catalog, String parentId) {
         return getRelatedElementsBySourceIdWithType(parentId, TYPE_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            Property impSpecType = buildProp(TYPE_ELEMENT_TYPE, elem.title, SP_800_66_URI.toString());
            return impSpecType;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }


    // Build RLinks to references, represented in CPRT site as publication crosswalks (pub_crosswalk element type, projection relationship type)
    private List<Link> createPubCrosswalkLinks(Catalog catalog, String parentId) {
         return getRelatedElementsBySourceIdWithType(parentId, PUB_CROSSWALK_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE)
            .map(elem -> {
                if (! createdPubCrosswalks.containsKey(elem.title)) {
                    Resource pubCrosswalkResource = buildResource(elem);
                    pubCrosswalkResource.setTitle(MarkupLine.fromMarkdown(elem.title));
                    Link link = newLinkRel(catalog, pubCrosswalkResource, PUB_CROSSWALK_ELEMENT_TYPE);
                    createdPubCrosswalks.put(elem.title, link);
                    return link;
                }
                else {
                    return createdPubCrosswalks.get(elem.title);
                }
            // if exists, link to already existing reference, have a hashmap of identifier and resource object
            }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
}
