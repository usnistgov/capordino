package gov.nist.capordino.cprt.conversion.csf20;

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

public class Csf20CprtOscalConverter extends AbstractOscalConverter {
    protected void assertFrameworkIdentifier() throws InvalidFrameworkIdentifier {
        if (!cprtMetadataVersion.frameworkIdentifier.equals("CSF")) {
            throw new InvalidFrameworkIdentifier("CSF", cprtMetadataVersion.frameworkIdentifier);
        }
    }

    public Csf20CprtOscalConverter(CprtMetadataVersion cprtMetadataVersion, CprtRoot cprtRoot) throws InvalidFrameworkIdentifier {
        super(cprtMetadataVersion, cprtRoot);
        assertFrameworkIdentifier();
    }
    
    public Csf20CprtOscalConverter(CprtMetadataVersion cprtMetadataVersion) throws IOException, InterruptedException, InvalidFrameworkIdentifier {
        super(cprtMetadataVersion);
        assertFrameworkIdentifier();
    }

    public Csf20CprtOscalConverter(String frameworkVersionIdentifier) throws IOException, InterruptedException, InvalidFrameworkIdentifier {
        super(frameworkVersionIdentifier);
        assertFrameworkIdentifier();
    }

    private final String FUNCTION_ELEMENT_TYPE = "function";
    private final String SORT_ELEMENT_TYPE = "sort";
    private final String CATEGORY_ELEMENT_TYPE = "category";
    private final String SUBCATEGORY_ELEMENT_TYPE = "subcategory";
    private final String IMPLEMENTATION_EXAMPLE_ELEMENT_TYPE = "implementation_example";
    private final String PARTY_ELEMENT_TYPE = "party";

    private final String PROJECTION_RELATIONSHIP_TYPE = "projection";

    /**
     * The URI to use for CSF-specific props.
     */
    private final URI CSF_URI = URI.create("https://csrc.nist.gov/ns/csf");

    @Override
    protected void hydrateCatalog(Catalog catalog) {
        catalog.setGroups(buildFunctionGroups(catalog));
    }

    protected ControlPart buildPartFromElementText(CprtElement element, String name) {
        ControlPart elementProse = new ControlPart();
        elementProse.setId(element.element_identifier + "_" + name);
        elementProse.setName(name);
        elementProse.setProse(MarkupMultiline.fromMarkdown(escapeSquareBrackets(element.text)));
        return elementProse;
    }

    protected ControlPart buildPartFromElementText(CprtElement element, String name, URI namespace) {
        ControlPart part = buildPartFromElementText(element, name);
        part.setNs(namespace);
        return part;
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

                group.addPart(buildPartFromElementText(elem, "instruction"));
                group.setControls(buildCategoryControls(catalog, elem.getGlobalIdentifier()));

                Property sortProp = buildSortProp(elem.getGlobalIdentifier());
                if (sortProp != null) {
                    group.addProp(sortProp);
                }

                group.addProp(buildLabelProp(elem.title + " (" + elem.element_identifier + ")"));

                return group;
            }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Build the second level group of the catalog, represented in CPRT as categories.
     */
    private List<Control> buildCategoryControls(Catalog catalog, String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, CATEGORY_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            Control control = new Control();
            control.setId(elem.element_identifier);
            control.setClazz(elem.element_type);
            control.setTitle(MarkupLine.fromMarkdown(elem.title));

            control.addPart(buildPartFromElementText(elem, "statement"));
            control.setControls(buildSubcategoryControls(catalog, elem.getGlobalIdentifier()));

            Property sortProp = buildSortProp(elem.getGlobalIdentifier());
            if (sortProp != null) {
                control.addProp(sortProp);
            }

            control.addProp(buildLabelProp(elem.title + " (" + elem.element_identifier + ")"));

            return control;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Build the third level control of the catalog, represented in CPRT as subcategories.
     */
    private List<Control> buildSubcategoryControls(Catalog catalog, String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, SUBCATEGORY_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            Control control = new Control();
            control.setId(elem.element_identifier);
            control.setClazz(elem.element_type);

            // CSF subcategories do not have titles, so use the identifier as the title
            String title = elem.title;
            if (title == null || title.isEmpty()) {
                title = elem.element_identifier;
            }
            control.setTitle(MarkupLine.fromMarkdown(title));

            // Examples should follow statement
            ArrayList<ControlPart> parts = new ArrayList<ControlPart>();
            parts.add(buildPartFromElementText(elem, "statement"));
            parts.addAll(buildSubcategoryImplementationExamples(catalog, elem.getGlobalIdentifier()));
            control.setParts(parts);

            control.setProps(buildSubcategoryRiskPartyProps(elem.getGlobalIdentifier()));

            Property sortProp = buildSortProp(elem.getGlobalIdentifier());
            if (sortProp != null) {
                control.addProp(sortProp);
            }

            control.addProp(buildLabelProp(elem.element_identifier));

            return control;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private List<ControlPart> buildSubcategoryImplementationExamples(Catalog catalog, String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, IMPLEMENTATION_EXAMPLE_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            ControlPart part = buildPartFromElementText(elem, "example", CSF_URI);
            part.setId(elem.element_identifier);
            return part;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private List<Property> buildSubcategoryRiskPartyProps(String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, PARTY_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            Property riskPartyProp = new Property();
            riskPartyProp.setName("risk-party");
            riskPartyProp.setNs(CSF_URI);
            riskPartyProp.setValue(elem.title);
            riskPartyProp.setRemarks(MarkupMultiline.fromMarkdown(elem.text));
            return riskPartyProp;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
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
