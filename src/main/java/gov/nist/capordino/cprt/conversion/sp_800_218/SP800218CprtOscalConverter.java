package gov.nist.capordino.cprt.conversion.sp_800_218;

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

public class SP800218CprtOscalConverter extends AbstractOscalConverter {
    protected void assertFrameworkIdentifier() throws InvalidFrameworkIdentifier {
        if (!cprtMetadataVersion.frameworkIdentifier.equals("SP_800_218")) {
            throw new InvalidFrameworkIdentifier("SP_800_218", cprtMetadataVersion.frameworkIdentifier);
        }
    }

    public SP800218CprtOscalConverter(CprtMetadataVersion cprtMetadataVersion, CprtRoot cprtRoot) throws InvalidFrameworkIdentifier {
        super(cprtMetadataVersion, cprtRoot);
        assertFrameworkIdentifier();
    }
    
    public SP800218CprtOscalConverter(CprtMetadataVersion cprtMetadataVersion) throws IOException, InterruptedException, InvalidFrameworkIdentifier {
        super(cprtMetadataVersion);
        assertFrameworkIdentifier();
    }

    public SP800218CprtOscalConverter(String frameworkVersionIdentifier) throws IOException, InterruptedException, InvalidFrameworkIdentifier {
        super(frameworkVersionIdentifier);
        assertFrameworkIdentifier();
    }

    private final String GROUP_ELEMENT_TYPE = "group";
    private final String PRACTICE_ELEMENT_TYPE = "practice";
    private final String TASK_ELEMENT_TYPE = "task";
    private final String IMPLEMENTATION_EXAMPLE_ELEMENT_TYPE = "example";
    private final String REF_ITEM_ELEMENT_TYPE = "ref_item";
    private final String REF_DOC_ELEMENT_TYPE = "ref_doc";

    private final String PROJECTION_RELATIONSHIP_TYPE = "projection";
    private final String REFERENCE_RELATIONSHIP_TYPE = "reference";
    private final String EXTERNAL_REFERENCE_RELATIONSHIP_TYPE = "external_reference";

    /**
     * The URI to use for 800-218-specific props.
     */
    private final URI SP_800_218_URI = URI.create("https://csrc.nist.gov/ns/csf");

    @Override
    protected void hydrateCatalog(Catalog catalog) {
        catalog.setGroups(buildGroups(catalog));
    }

    

    /**
     * Build the top level group of the catalog, represented in CPRT as groups.
     */
    private List<CatalogGroup> buildGroups(Catalog catalog) {
        // Recursively go down tree of elements, to build family groups
        return cprtRoot.getElements().stream()
            .filter(elem -> elem.element_type.equals(GROUP_ELEMENT_TYPE))
            .map(elem -> {
                // For each 800-218 group, create an OSCAL group
                CatalogGroup group = new CatalogGroup();
                group.setId(elem.element_identifier);
                group.setClazz(elem.element_type);
                group.setTitle(MarkupLine.fromMarkdown(elem.title));
                group.addProp(buildProp("sort-id", elem.element_identifier));

                group.addPart(buildPartFromElementText(elem, "overview"));
                // For 800-218 practice, create an OSCAL control
                group.setControls(buildPracticeControls(catalog, elem.getGlobalIdentifier()));

                group.addProp(buildLabelProp(elem.title + " (" + elem.element_identifier + ")"));

                return group;
            }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Build the second level group of the catalog, represented in CPRT as practices.
     */
    // For 800-218 practice, create an OSCAL control
    private List<Control> buildPracticeControls(Catalog catalog, String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, PRACTICE_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            Control control = new Control();
            control.setId(elem.element_identifier);
            control.setClazz(elem.element_type);

            control.addProp(buildProp("sort-id", elem.element_identifier));
            control.setTitle(MarkupLine.fromMarkdown(elem.title));

            control.addPart(buildPartFromElementText(elem, "statement"));
            // For 800-218 task, create OSCAL subcontrol
            control.setControls(buildTaskControls(catalog, elem.getGlobalIdentifier()));

            control.addProp(buildLabelProp(elem.title + " (" + elem.element_identifier + ")"));

            return control;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Build the third level control of the catalog, represented in CPRT as tasks.
     */
    private List<Control> buildTaskControls(Catalog catalog, String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, TASK_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
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
            parts.addAll(buildNotionalImplementationExamples(catalog, elem.getGlobalIdentifier()));
            control.setParts(parts);

            control.setLinks(createRefItemLinks(catalog, elem.getGlobalIdentifier()));


            control.addProp(buildLabelProp(elem.element_identifier));

            return control;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    // Build RLinks to references, represented in CPRT site as ref items (ref_items element type, external_reference relationship type)
    private List<Link> createRefItemLinks(Catalog catalog, String parentId) {
        List<CprtElement> ref_item_elements = getRelatedElementsBySourceIdWithType(parentId, REF_ITEM_ELEMENT_TYPE, EXTERNAL_REFERENCE_RELATIONSHIP_TYPE).map(elem -> {
            return elem;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        List<Link> ref_item_links = new ArrayList<Link>();
        for (CprtElement ref_item : ref_item_elements) {
            List<CprtElement> ref_doc_elements = getRelatedElementsBySourceIdWithType(ref_item.getGlobalIdentifier(), REF_DOC_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
                return elem;
            }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            
            for (CprtElement ref_doc : ref_doc_elements) {
                Resource refItemResource = buildResource(ref_doc);
                Link link = newLinkRel(catalog, refItemResource, EXTERNAL_REFERENCE_RELATIONSHIP_TYPE);
                link.setText(MarkupLine.fromMarkdown(ref_item.text));
                ref_item_links.add(link);
            }
        }

        return ref_item_links;
    }

    private List<ControlPart> buildNotionalImplementationExamples(Catalog catalog, String parentId) {
        return getRelatedElementsBySourceIdWithType(parentId, IMPLEMENTATION_EXAMPLE_ELEMENT_TYPE, PROJECTION_RELATIONSHIP_TYPE).map(elem -> {
            ControlPart part = buildPartFromElementText(elem, "example");//  CSF_URI);
            part.setId(elem.element_identifier);
            return part;
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
   
}
