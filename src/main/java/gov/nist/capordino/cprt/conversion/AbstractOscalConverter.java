package gov.nist.capordino.cprt.conversion;

import java.io.IOException;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import gov.nist.capordino.cprt.api.CprtApiClient;
import gov.nist.capordino.cprt.pojo.CprtElement;
import gov.nist.capordino.cprt.pojo.CprtExportResponse;
import gov.nist.capordino.cprt.pojo.CprtMetadataResponse;
import gov.nist.capordino.cprt.pojo.CprtMetadataVersion;
import gov.nist.capordino.cprt.pojo.CprtRelationship;
import gov.nist.capordino.cprt.pojo.CprtRoot;
import gov.nist.secauto.metaschema.model.common.datatype.markup.MarkupLine;
import gov.nist.secauto.metaschema.model.common.datatype.markup.MarkupMultiline;
import gov.nist.secauto.oscal.lib.model.Address;
import gov.nist.secauto.oscal.lib.model.BackMatter;
import gov.nist.secauto.oscal.lib.model.BackMatter.Resource;
import gov.nist.secauto.oscal.lib.model.BackMatter.Resource.Citation;
import gov.nist.secauto.oscal.lib.model.BackMatter.Resource.Rlink;
import gov.nist.secauto.oscal.lib.model.Catalog;
import gov.nist.secauto.oscal.lib.model.ControlPart;
import gov.nist.secauto.oscal.lib.model.Link;
import gov.nist.secauto.oscal.lib.model.Metadata;
import gov.nist.secauto.oscal.lib.model.Metadata.Party;
import gov.nist.secauto.oscal.lib.model.Metadata.Role;
import gov.nist.secauto.oscal.lib.model.Parameter;
import gov.nist.secauto.oscal.lib.model.ParameterGuideline;
import gov.nist.secauto.oscal.lib.model.Property;
import gov.nist.secauto.oscal.lib.model.ResponsibleParty;

public abstract class AbstractOscalConverter {
    protected final CprtMetadataVersion cprtMetadataVersion;
    protected final CprtRoot cprtRoot;
    protected final String CAPORDINO_CONTACT_EMAIL = "capordino@nist.gov";

    /**
     * The URI to use for CPRT-specific props.
     */
    private final URI CPRT_URI = URI.create("https://csrc.nist.gov/ns/cprt");

    public AbstractOscalConverter(CprtMetadataVersion cprtMetadataVersion, CprtRoot cprtRoot) {
        this.cprtMetadataVersion = cprtMetadataVersion;
        this.cprtRoot = cprtRoot;
    }

    /**
     * Given the cprtMetadataVersion, fetch the CPRT data from the API
     * @param cprtMetadataVersion
     * @throws IOException
     * @throws InterruptedException
     */
    public AbstractOscalConverter(CprtMetadataVersion cprtMetadataVersion) throws IOException, InterruptedException {
        CprtApiClient cprtApiClient = new CprtApiClient();
        CprtExportResponse response = cprtApiClient.exportCprt(cprtMetadataVersion.frameworkVersionIdentifier);
        
        this.cprtMetadataVersion = cprtMetadataVersion;
        this.cprtRoot = response.elements;
    }

    /**
     * Given the frameworkVersionIdentifier, fetch the CPRT data and metadata from the API
     * @param frameworkVersionIdentifier
     * @throws IOException
     * @throws InterruptedException
     */
    public AbstractOscalConverter(String frameworkVersionIdentifier) throws IOException, InterruptedException {
        CprtApiClient cprtApiClient = new CprtApiClient();
        CprtMetadataResponse metadata = cprtApiClient.getMetadata();
        this.cprtMetadataVersion = metadata.versions.stream()
            .filter(v -> v.frameworkVersionIdentifier.equals(frameworkVersionIdentifier))
            .findFirst()
            .orElseThrow();
        CprtExportResponse response = cprtApiClient.exportCprt(cprtMetadataVersion.frameworkVersionIdentifier);
        this.cprtRoot = response.elements;
    }

    public Catalog buildCatalog() {
        Catalog catalog = new Catalog();
        catalog.setUuid(UUID.randomUUID());
        catalog.setMetadata(buildMetadata(catalog));

        hydrateCatalog(catalog);

        return catalog;
    }

    protected Property newCprtProp(@Nonnull String name, @Nonnull String value, @Nullable MarkupMultiline remarks) {
        Property prop = new Property();
        prop.setName(name);
        prop.setValue(value);
        prop.setNs(CPRT_URI);

        if (remarks != null) {
            prop.setRemarks(remarks);
        }

        return prop;
    }

    protected Property newCprtProp(@Nonnull String name, @Nonnull String value) {
        return newCprtProp(name, value, null);
    }

    protected ZonedDateTime dateToZonedDateTime(java.util.Date date) {
        return ZonedDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault());
    }

    /**
     * Create and return a new link with the given relType to the given resource
     * 
     * Side effects:
     * - Adds resource to the catalog's backMatter
     * - If the resource does not have a UUID, sets the UUID to a new random UUID
     */
    protected Link newLinkRel(@Nonnull Catalog catalog, @Nonnull Resource resource, @Nonnull String relType) {
        if (resource.getUuid() == null) {
            resource.setUuid(UUID.randomUUID());
        }

        if (catalog.getBackMatter() == null) {
            catalog.setBackMatter(new BackMatter());
        }
        
        catalog.getBackMatter().addResource(resource);
                
        Link link = new Link();
        link.setHref(URI.create("#" + resource.getUuid().toString()));
        link.setRel(relType);
        
        return link;
    }

    private Party buildPublisherParty() {
        Party party = new Party();
        party.setUuid(UUID.randomUUID());
        party.setName("National Institute of Standards and Technology");
        party.setShortName("NIST");
        party.setType("organization");

        Address address = new Address();
        address.addAddrLine("National Institute of Standards and Technology");
        address.addAddrLine("Attn: Applied Cybersecurity Division");
        address.addAddrLine("Information Technology Laboratory");
        address.addAddrLine("100 Bureau Drive (Mail Stop 2000)");
        address.setCity("Gaithersburg");
        address.setState("MD");
        address.setPostalCode("20899-2000");

        party.addAddress(address);
        party.addEmailAddress(CAPORDINO_CONTACT_EMAIL);
        return party;
    }

    private Party buildAuthorParty() {
        Party party = new Party();
        party.setUuid(UUID.randomUUID());
        party.setType("organization");

        party.addEmailAddress(cprtMetadataVersion.pocEmailAddress);
        return party;
    }

    private Metadata buildMetadata(@Nonnull Catalog catalog) {
        Metadata metadata = new Metadata();
        metadata.setOscalVersion("v1.1.2");
        metadata.setLastModified(ZonedDateTime.now());
        
        metadata.setTitle(MarkupLine.fromMarkdown(cprtMetadataVersion.frameworkVersionName));
        metadata.setVersion(cprtMetadataVersion.version);
        
        metadata.addProp(newCprtProp("framework-identifier", cprtMetadataVersion.frameworkIdentifier));
        metadata.addProp(newCprtProp("framework-version-identifier", cprtMetadataVersion.frameworkVersionIdentifier));

        metadata.addProp(newCprtProp("generated-by", "Cybersecurity And Privacy Open Reference Datasets In OSCAL (CAPORDINO)"));

        if (cprtMetadataVersion.publicationStatus != null && !cprtMetadataVersion.publicationStatus.equals("")) {
            metadata.addProp(newCprtProp("publication-status", cprtMetadataVersion.publicationStatus));
        }

        metadata.setPublished(dateToZonedDateTime(cprtMetadataVersion.publicationReleaseDate));

        // Add website link
        Resource frameworkLinkResource = new Resource();
        frameworkLinkResource.setTitle(MarkupLine.fromMarkdown(cprtMetadataVersion.frameworkVersionName));
        Rlink frameworkWebsiteRlink = new Rlink();
        frameworkWebsiteRlink.setHref(URI.create(cprtMetadataVersion.frameworkWebSite));
        frameworkWebsiteRlink.setMediaType("application/html");
        frameworkLinkResource.addRlink(frameworkWebsiteRlink);
        metadata.addLink(newLinkRel(catalog, frameworkLinkResource, "alternate"));

        if (cprtMetadataVersion.frameworkVersionWebSite != null && !cprtMetadataVersion.frameworkVersionWebSite.isEmpty()) {
            Resource frameworkVersionLinkResource = new Resource();
            frameworkVersionLinkResource.setTitle(MarkupLine.fromMarkdown(cprtMetadataVersion.frameworkVersionName));
            Rlink frameworkVersionWebsiteRlink = new Rlink();
            frameworkVersionWebsiteRlink.setHref(URI.create(cprtMetadataVersion.frameworkVersionWebSite));
            frameworkVersionWebsiteRlink.setMediaType("application/html");
            frameworkVersionLinkResource.addRlink(frameworkVersionWebsiteRlink);
            metadata.addLink(newLinkRel(catalog, frameworkVersionLinkResource, "canonical"));
        }

        // Add party information
        Party publisherParty = buildPublisherParty();
        metadata.addParty(publisherParty);

        Role publisherRole = new Role();
        publisherRole.setId("publisher");
        publisherRole.setTitle(MarkupLine.fromMarkdown("Publisher"));
        metadata.addRole(publisherRole);

        ResponsibleParty publisherResponsibleParty = new ResponsibleParty();
        publisherResponsibleParty.setRoleId(publisherRole.getId());
        publisherResponsibleParty.addPartyUuid(publisherParty.getUuid());
        metadata.addResponsibleParty(publisherResponsibleParty);

        Role contactRole = new Role();
        contactRole.setId("contact");
        contactRole.setTitle(MarkupLine.fromMarkdown("Contact"));
        metadata.addRole(contactRole);

        ResponsibleParty contactResponsibleParty = new ResponsibleParty();
        contactResponsibleParty.setRoleId(contactRole.getId());
        contactResponsibleParty.addPartyUuid(publisherParty.getUuid());
        metadata.addResponsibleParty(contactResponsibleParty);

        if (cprtMetadataVersion.pocEmailAddress != null && !cprtMetadataVersion.pocEmailAddress.isEmpty()) {
            Party authorParty = buildAuthorParty();
            metadata.addParty(authorParty);
    
            Role authorRole = new Role();
            authorRole.setId("author");
            authorRole.setTitle(MarkupLine.fromMarkdown("Author"));
            metadata.addRole(authorRole);
    
            ResponsibleParty authorResponsibleParty = new ResponsibleParty();
            authorResponsibleParty.setRoleId(authorRole.getId());
            authorResponsibleParty.addPartyUuid(authorParty.getUuid());
            metadata.addResponsibleParty(authorResponsibleParty);
        }

        return metadata;
    }

    /*
     * Given a catalog containing minimal metadata, hydrate the catalog with the CPRT data
     */
    protected abstract void hydrateCatalog(Catalog catalog);

    // Helpers

    // Get all elements of type elemType and element identifier contains parentId
    // Used for an easier way to retrieve the relevant assessment objectives of a control
    protected Stream<CprtElement> getRelatedElementsByType(String elemType, String parentId) {
        return cprtRoot.getElements().stream()
            .filter(elem -> elem.element_type.equals(elemType))
            .filter(elem -> elem.element_identifier.contains(parentId));
            
    }

    protected Stream<CprtElement> getRelatedElementsBySourceIdWithType(String sourceId, String elemType) {
        return cprtRoot.getRelationships().stream()
            .filter(rel -> rel.getSourceGlobalIdentifier().equals(sourceId))
            .map(rel -> {
                CprtElement element = cprtRoot.getElementById(rel.getDestGlobalIdentifier());
                if (element == null) {
                    throw new IllegalArgumentException("Error getting elements related to sourceId " + sourceId + ". Destination identifier " + rel.getDestGlobalIdentifier() + " not found");
                }
                return element;
            })
            .filter(elem -> elem.element_type.equals(elemType));
    }

    protected Stream<CprtElement> getRelatedElementsBySourceIdWithType(String sourceId, String elemType, String relationType) {
        return cprtRoot.getRelationships().stream()
            .filter(rel -> rel.getSourceGlobalIdentifier().equals(sourceId) && rel.relationship_identifier.equals(relationType))
            .map(rel -> {
                CprtElement element = cprtRoot.getElementById(rel.getDestGlobalIdentifier());
                if (element == null) {
                    throw new IllegalArgumentException("Error getting elements related to sourceId " + sourceId + ". Destination identifier " + rel.getDestGlobalIdentifier() + " not found");
                }
                return element;
            })
            .filter(elem -> elem.element_type.equals(elemType));
    }

    // Given a source element identifier and a relationship type, find all of its destination element identifiers
    // Useful to get the control a withdrawn control points to
    protected Stream<String> getDestinationIdWithType(String sourceId, String relationType) {
        return cprtRoot.getRelationships().stream()
            .filter(rel -> rel.getSourceGlobalIdentifier().equals(sourceId) && rel.relationship_identifier.equals(relationType))
            .map(rel -> {
                return rel.dest_element_identifier;
            });
    }

    /**
     * Escape square brackets in the input string to avoid confusing OSCAL's param syntax.
     */
    protected String escapeSquareBracketsWithParentheses(String input) {
        return input.replaceAll("\\[", "(").replaceAll("\\]", ")");
    }

    // Escape square brackets in input string, keep square brackets
    protected String escapeSquareBrackets(String input) {
        return input.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]");
    }

    protected Property buildProp(String name, String value) {
        Property prop = new Property();
        prop.setName(name);
        prop.setValue(value);
        return prop;
    }

    protected Property buildLabelProp(String label) {
        return buildProp("label", label);
    }

    protected Property buildWithdrawnProp() {
        return buildProp("status", "withdrawn");
    }

    

    protected ControlPart buildPartFromElementText(CprtElement element, String name) {
        ControlPart elementProse = new ControlPart();
        elementProse.setId(element.element_identifier + "_" + name);
        elementProse.setName(name);

        // Parse ODPs before setting text
        elementProse.setProse(MarkupMultiline.fromMarkdown(escapeSquareBracketsWithParentheses(parseODPInElementText(element))));
        
        return elementProse;
    }

    protected abstract String parseODPInElementText(CprtElement element);

    protected ControlPart buildPartFromElementText(CprtElement element, String name, URI namespace) {
        ControlPart part = buildPartFromElementText(element, name);
        part.setNs(namespace);
        return part;
    }

    // Builds a Part for a Assessment Objective
    protected ControlPart buildAssessmentObjectivePart(CprtElement element) {
        ControlPart part = buildPartFromElementText(element, "assessment-objective");

        // Regex to match how ODPs are written in assessment objectives
        String objective_text = element.text;
        // Need non-greedy regex, otherwise it matches multiple ODPs as one.
        Pattern odp_pattern = Pattern.compile("<(.+?): .+?>");
        Matcher odp_matcher = odp_pattern.matcher(objective_text);

        // Get ODP(s) in this assessment objective
        List<String> odp_identifiers = new ArrayList<String>();
        while(odp_matcher.find()) {
            odp_identifiers.add(odp_matcher.group(1));
        }
        
        
        // Replace ODP with insert param
        for (String odp_identifier : odp_identifiers) {
            String insert = String.format("<insert type=\"param\" id-ref=\"%s\" />", odp_identifier) ;
            String escaped_odp_identifier = escapeSquareBrackets(odp_identifier);

            // Only replace the ODP that matches this identifier
            String specific_odp_pattern = "<" + escaped_odp_identifier + ": .+?>"; 
            objective_text = objective_text.replaceAll(specific_odp_pattern, insert);
        }
        
        part.setProse(MarkupMultiline.fromMarkdown(escapeSquareBracketsWithParentheses(objective_text)));

        return part;
    }

    // Builds a list of Params that the given assessment objective states
    // Uses ODPs listed in assessment objective instead of in control, because ODP id is explicitly stated in assessment objective
    // Also doesn't require traversing to all subcontrol items, to find all ODPs connected to a control
    // Assumes assessment objective is correctly connected to the control it assesses
    protected List<Parameter> buildParams(CprtElement element) {
        // Regex to match how ODPs are written in assessment objectives
        String objective_text = element.text;
        // Need non-greedy regex. Otherwise it matches multiple ODPs as one.
        Pattern odp_pattern = Pattern.compile("<(.+?): .+?>");
        Matcher odp_matcher = odp_pattern.matcher(objective_text);

        // Get ODP(s) in this assessment objective
        List<String> odp_identifiers = new ArrayList<String>();
        while(odp_matcher.find()) {
            odp_identifiers.add(odp_matcher.group(1));
        }
        
        // Build param Parts
        List<Parameter> params = new ArrayList<Parameter>();
        for (String odp_identifier : odp_identifiers) {
            params.add(buildParam(odp_identifier, element.doc_identifier));
        }

        return params;
    }

    // Builds a OSCAL Param for a given ODP id
    protected Parameter buildParam(String odp_identifier, String doc_identifier) {
        // Convert to global identifier, because of how elements map stores elements
        String odp_global_identifier = doc_identifier + ":" + odp_identifier;

        // Get the ODP element associated with the ODP id
        CprtElement odp_element = cprtRoot.getElementById(odp_global_identifier);

        // Create a Parameter object
        Parameter odp_param = new Parameter();
        odp_param.addProp(buildLabelProp(odp_identifier));
        odp_param.setLabel(MarkupLine.fromMarkdown(escapeSquareBracketsWithParentheses(odp_element.title)));
        ParameterGuideline odp_param_guideline = new ParameterGuideline();
        odp_param_guideline.setProse(MarkupMultiline.fromMarkdown(escapeSquareBracketsWithParentheses(odp_element.text)));
        odp_param.addGuideline(odp_param_guideline);

        // Param id must be escaped to be consistent with how params are inserted in controls and assessment objectives, which require escaped square brackets
        String escaped_odp_identifier = escapeSquareBracketsWithParentheses(odp_identifier);
        odp_param.setId(escaped_odp_identifier);

        return odp_param;
    }

    // Builds a Part for Assessment Methods
    protected ControlPart buildAssessmentMethodPart(CprtElement element, String separator, String prefix, String suffix) {
        ControlPart part = new ControlPart();
        part.setName("assessment-method");
        part.setId(element.element_identifier + "_" + part.getName() + "_" + element.element_type);
        part.addProp(buildProp("method", element.element_type.toUpperCase()));

        // Assessment Methods contain Assessment Objects
        ControlPart objects = new ControlPart();
        objects.setName("assessment-objects");

        // Assessment Objects in CPRT are written as "[SELECT FROM: object1; object2; ... objectz]" in element.text
        // In OSCAL, each object should be its own <p>
        
        // Remove the prefix, "[SELECT FROM: " and the suffix, "]"
        int suffix_index = element.text.indexOf(suffix);
        String object_list = element.text.substring(prefix.length(), suffix_index);

        // Each object is separated by ";" 
        // Convert the separator to two newlines, because fromMarkdown() converts two newlines to <p>
        String final_object_list = object_list.replaceAll(separator, "\n\n");

        objects.setProse(MarkupMultiline.fromMarkdown(escapeSquareBracketsWithParentheses(final_object_list)));

        // Nest assessment objects within assessment method
        part.addPart(objects);

        return part;
    }

    protected Resource buildResource(CprtElement element) {
        // Create external resource 
        Resource resource = new Resource();
        resource.setTitle(MarkupLine.fromMarkdown(element.element_identifier));

        // Publication information
        Citation citation = new Citation();
        citation.setText(MarkupLine.fromMarkdown(element.title));
        resource.setCitation(citation);

        // Link to publication
        Rlink rlink = new Rlink();
        rlink.setHref(URI.create(element.text));
        resource.addRlink(rlink);
            
        return resource;
    }

    // Create a relative Link
    protected Link createLink(String identifier, String relationType) {
        Link link = new Link();
        link.setHref(URI.create(identifier));
        link.setRel(relationType);

        return link;
    }

    // Create a list of relative Links
    protected List<Link> createLinks(List<String> identifiers, String relationType) {
        List<Link> links = new ArrayList<Link>();

        for (String identifier : identifiers) {
            Link link = createLink(identifier, relationType);
            links.add(link);
        }

        return links;
    }

}
