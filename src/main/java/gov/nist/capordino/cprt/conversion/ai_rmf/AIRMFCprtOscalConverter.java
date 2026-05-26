package gov.nist.capordino.cprt.conversion.ai_rmf;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
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
import gov.nist.secauto.oscal.lib.model.Link;
import gov.nist.secauto.oscal.lib.model.Property;

public class AIRMFCprtOscalConverter extends AbstractOscalConverter {
    protected void assertFrameworkIdentifier() throws InvalidFrameworkIdentifier {
        if (!cprtMetadataVersion.frameworkIdentifier.equals("AI_100")) {
            throw new InvalidFrameworkIdentifier("AI_100", cprtMetadataVersion.frameworkIdentifier);
        }
    }

    public AIRMFCprtOscalConverter(CprtMetadataVersion cprtMetadataVersion, CprtRoot cprtRoot) throws InvalidFrameworkIdentifier {
        super(cprtMetadataVersion, cprtRoot);
        assertFrameworkIdentifier();
    }
    
    public AIRMFCprtOscalConverter(CprtMetadataVersion cprtMetadataVersion) throws IOException, InterruptedException, InvalidFrameworkIdentifier {
        super(cprtMetadataVersion);
        assertFrameworkIdentifier();
    }

    public AIRMFCprtOscalConverter(String frameworkVersionIdentifier) throws IOException, InterruptedException, InvalidFrameworkIdentifier {
        super(frameworkVersionIdentifier);
        assertFrameworkIdentifier();
    }

    private final String ABOUT_ELEMENT_TYPE = "about";
    private final String CATEGORY_ELEMENT_TYPE = "category";
    private final String DOCUMENTATION_ELEMENT_TYPE = "documentation";
    private final String FUNCTION_ELEMENT_TYPE = "function";
    private final String REFERENCE_ELEMENT_TYPE = "reference";
    private final String RESOURCE_ELEMENT_TYPE = "resource";
    private final String SORT_ELEMENT_TYPE = "sort";
    private final String SUBCATEGORY_ELEMENT_TYPE = "subcategory";
    private final String SUGGESTED_ACTION_ELEMENT_TYPE = "suggested_action";

    private final String PROJECTION_RELATIONSHIP_TYPE = "projection";


    @Override
    protected void hydrateCatalog(Catalog catalog) {
        catalog.setGroups(buildFunctionGroups(catalog));
    }

    protected ControlPart buildPartFromElementText(CprtElement element, String name) {
        ControlPart elementProse = new ControlPart();
        elementProse.setId(element.element_identifier.replaceAll(" ", "_") + "_" + name);
        elementProse.setName(name);
        elementProse.setProse(MarkupMultiline.fromMarkdown(escapeSquareBracketsWithParentheses(element.text)));
        return elementProse;
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
     * Build the top level group of the catalog, represented in AI RMF as functions.
     */
    private List<CatalogGroup> buildFunctionGroups(Catalog catalog) {
        return cprtRoot.getElements().stream()
            .filter(elem -> elem.element_type.equals(FUNCTION_ELEMENT_TYPE))
            .map(elem -> {
                // For each AI RMF function, create an OSCAL group
                CatalogGroup group = new CatalogGroup();
                group.setId(elem.element_identifier);
                group.setClazz(elem.element_type);
                group.setTitle(MarkupLine.fromMarkdown(elem.element_identifier));

                group.addPart(buildPartFromElementText(elem, "overview"));
                // For AI RMF category, create an OSCAL group
                group.setGroups(buildCategoryGroups(catalog, elem.getGlobalIdentifier()));

                Property sortProp = buildSortProp(elem.getGlobalIdentifier());
                if (sortProp != null) {
                    group.addProp(sortProp);
                }

                return group;
            })
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

    /**
     * Build the second level group of the catalog, represented in AI RMF as categories.
     */
    private List<CatalogGroup> buildCategoryGroups(Catalog catalog, String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, CATEGORY_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            // For each AI RMF category, create an OSCAL group
            CatalogGroup group = new CatalogGroup();
            group.setId(elem.element_identifier.replaceAll(" ", "_"));
            group.setClazz(elem.element_type);
            group.setTitle(MarkupLine.fromMarkdown(elem.element_identifier));

            group.addPart(buildPartFromElementText(elem, "overview"));

            // For AI RMF subcategory, create an OSCAL control
            group.setControls(buildSubcategoryControls(catalog, elem.getGlobalIdentifier()));
           
            Property sortProp = buildSortProp(elem.getGlobalIdentifier());
            if (sortProp != null) {
                group.addProp(sortProp);
            }

            return group;
        })
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

    /**
     * Build the third level control of the catalog, represented in AI RMF as subcategories.
     */
    private List<Control> buildSubcategoryControls(Catalog catalog, String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, SUBCATEGORY_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            Control control = new Control();
            control.setId(elem.element_identifier.replaceAll(" ", "_"));
            control.setClazz(elem.element_type);

            control.setTitle(MarkupLine.fromMarkdown(elem.element_identifier));

            List<ControlPart> parts = new ArrayList<ControlPart>();

            ControlPart statementPart = buildPartFromElementText(elem, "statement");
            parts.add(statementPart);
            // Subcategory parts - about, documentation, resource, reference, suggested action


            
            control.setParts(parts);

            Property sortProp = buildSortProp(elem.getGlobalIdentifier());
            if (sortProp != null) {
                control.addProp(sortProp);
            }

            return control;
        })
        .sorted(Comparator.comparing(control -> 
            control.getProps().stream()
                // Get the sort-id prop
                .filter(prop -> prop.getName().equals("sort-id"))
                // Compare based on value of sort-id like "00001"
                .map(Property::getValue)
                .findFirst().orElse("")
        ))
        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
}
