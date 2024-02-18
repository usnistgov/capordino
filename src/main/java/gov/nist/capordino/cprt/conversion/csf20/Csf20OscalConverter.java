package gov.nist.capordino.cprt.conversion.csf20;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import gov.nist.capordino.cprt.pojo.CprtElement;
import gov.nist.capordino.cprt.pojo.CprtRoot;
import gov.nist.secauto.metaschema.model.common.datatype.markup.MarkupLine;
import gov.nist.secauto.metaschema.model.common.datatype.markup.MarkupMultiline;
import gov.nist.secauto.oscal.lib.model.Catalog;
import gov.nist.secauto.oscal.lib.model.CatalogGroup;
import gov.nist.secauto.oscal.lib.model.Control;
import gov.nist.secauto.oscal.lib.model.ControlPart;
import gov.nist.secauto.oscal.lib.model.Metadata;
import gov.nist.secauto.oscal.lib.model.Property;

public class Csf20OscalConverter {
    private CprtRoot cprtRoot;

    private final String FUNCTION_ELEMENT_TYPE = "function";
    private final String SORT_ELEMENT_TYPE = "sort";
    private final String CATEGORY_ELEMENT_TYPE = "category";
    private final String SUBCATEGORY_ELEMENT_TYPE = "subcategory";
    private final String IMPLEMENTATION_EXAMPLE_ELEMENT_TYPE = "implementation_example";
    private final String PARTY_ELEMENT_TYPE = "party";

    /**
     * The URI to use for CSF-specific props.
     * 
     * TODO: This should be updated to a more official URI once it is available.
     */
    private final String CSF_URI = "https://doi.org/10.6028/NIST.CSWP.29.ipd";

    public Csf20OscalConverter(CprtRoot cprtRoot) {
        this.cprtRoot = cprtRoot;
    }

    public Catalog toOscal() {
        ZonedDateTime now = ZonedDateTime.now();
        
        Catalog catalog = new Catalog();
        catalog.setUuid(UUID.randomUUID());
        
        Metadata metadata = new Metadata();

        metadata.setOscalVersion("v1.1.2");
        metadata.setTitle(MarkupLine.fromMarkdown(cprtRoot.getDocuments().get(0).name));
        metadata.setLastModified(now);
        metadata.setPublished(now);
        metadata.setVersion(cprtRoot.getDocuments().get(0).version);

        catalog.setMetadata(metadata);

        catalog.setGroups(buildFunctionGroups(catalog));

        return catalog;
    }

    private Stream<CprtElement> getRelatedElementsBySourceIdWithType(String sourceId, String type) {
        return cprtRoot.getRelationships().stream()
            .filter(rel -> rel.getSourceGlobalIdentifier().equals(sourceId))
            .map(rel -> cprtRoot.getElementById(rel.getDestGlobalIdentifier()))
            .filter(elem -> elem.element_type.equals(type));
    }

    /**
     * Escape square brackets in the input string to avoid confusing OSCAL's param syntax.
     */
    private String escapeSquareBrackets(String input) {
        return input.replaceAll("\\[", "(").replaceAll("\\]", ")");
    }

    private ControlPart buildPartFromElementText(CprtElement element, String name) {
        ControlPart elementProse = new ControlPart();
        elementProse.setName(name);
        elementProse.setProse(MarkupMultiline.fromMarkdown(escapeSquareBrackets(element.text)));
        return elementProse;
    }

    /**
     * Build the top level group of the catalog, represented in CPRT as functions.
     */
    private List<CatalogGroup> buildFunctionGroups(Catalog catalog) {
        return cprtRoot.getElements().stream()
            .filter(elem -> elem.element_type.equals(FUNCTION_ELEMENT_TYPE))
            .map(elem -> {
                CatalogGroup group = new CatalogGroup();
                group.setId(elem.element_identifier);
                group.setClazz(elem.element_type);
                group.setTitle(MarkupLine.fromMarkdown(elem.title));

                group.addPart(buildPartFromElementText(elem, "overview"));
                group.setGroups(buildCategoryGroups(catalog, elem.getGlobalIdentifier()));

                Property sortProp = buildSortProp(elem.getGlobalIdentifier());
                if (sortProp != null) {
                    group.addProp(sortProp);
                }

                return group;
            }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Build the second level group of the catalog, represented in CPRT as categories.
     */
    private List<CatalogGroup> buildCategoryGroups(Catalog catalog, String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, CATEGORY_ELEMENT_TYPE).map(elem -> {
            CatalogGroup group = new CatalogGroup();
            group.setId(elem.element_identifier);
            group.setClazz(elem.element_type);
            group.setTitle(MarkupLine.fromMarkdown(elem.title));

            group.addPart(buildPartFromElementText(elem, "overview"));
            group.setControls(buildSubcategoryControls(catalog, elem.getGlobalIdentifier()));

            Property sortProp = buildSortProp(elem.getGlobalIdentifier());
            if (sortProp != null) {
                group.addProp(sortProp);
            }

            return group;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Build the third level control of the catalog, represented in CPRT as subcategories.
     */
    private List<Control> buildSubcategoryControls(Catalog catalog, String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, SUBCATEGORY_ELEMENT_TYPE).map(elem -> {
            Control control = new Control();
            control.setId(elem.element_identifier);
            control.setClazz(elem.element_type);
            control.setTitle(MarkupLine.fromMarkdown(elem.title));

            control.setParts(buildSubcategoryImplementationExamples(catalog, elem.getGlobalIdentifier()));
            control.addPart(buildPartFromElementText(elem, "statement"));

            control.setProps(buildSubcategoryRiskPartyProps(elem.getGlobalIdentifier()));

            Property sortProp = buildSortProp(elem.getGlobalIdentifier());
            if (sortProp != null) {
                control.addProp(sortProp);
            }

            return control;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private List<ControlPart> buildSubcategoryImplementationExamples(Catalog catalog, String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, IMPLEMENTATION_EXAMPLE_ELEMENT_TYPE).map(elem -> {
            ControlPart part = buildPartFromElementText(elem, "example");
            part.setId(elem.element_identifier);
            return part;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private List<Property> buildSubcategoryRiskPartyProps(String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, PARTY_ELEMENT_TYPE).map(elem -> {
            Property riskPartyProp = new Property();
            riskPartyProp.setName("risk-party");
            riskPartyProp.setNs(URI.create(CSF_URI));
            riskPartyProp.setValue(elem.title);
            riskPartyProp.setRemarks(MarkupMultiline.fromMarkdown(elem.text));
            return riskPartyProp;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private Property buildSortProp(String parentId) {
        List<CprtElement> sorts = getRelatedElementsBySourceIdWithType(parentId, SORT_ELEMENT_TYPE).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
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
