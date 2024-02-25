package gov.nist.capordino.cprt.conversion;

import java.io.IOException;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.Nonnull;

import gov.nist.capordino.cprt.api.CprtApiClient;
import gov.nist.capordino.cprt.pojo.CprtElement;
import gov.nist.capordino.cprt.pojo.CprtExportResponse;
import gov.nist.capordino.cprt.pojo.CprtMetadataResponse;
import gov.nist.capordino.cprt.pojo.CprtMetadataVersion;
import gov.nist.capordino.cprt.pojo.CprtRoot;
import gov.nist.secauto.metaschema.model.common.datatype.markup.MarkupLine;
import gov.nist.secauto.metaschema.model.common.datatype.markup.MarkupMultiline;
import gov.nist.secauto.oscal.lib.model.Address;
import gov.nist.secauto.oscal.lib.model.BackMatter;
import gov.nist.secauto.oscal.lib.model.Catalog;
import gov.nist.secauto.oscal.lib.model.Link;
import gov.nist.secauto.oscal.lib.model.Metadata;
import gov.nist.secauto.oscal.lib.model.Property;
import gov.nist.secauto.oscal.lib.model.ResponsibleParty;
import gov.nist.secauto.oscal.lib.model.BackMatter.Resource;
import gov.nist.secauto.oscal.lib.model.BackMatter.Resource.Rlink;
import gov.nist.secauto.oscal.lib.model.Metadata.Party;
import gov.nist.secauto.oscal.lib.model.Metadata.Role;

public abstract class AbstractOscalConverter {
    protected final CprtMetadataVersion cprtMetadataVersion;
    protected final CprtRoot cprtRoot;

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

    private Party buildNistParty() {
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

        if (cprtMetadataVersion.pocEmailAddress != null && cprtMetadataVersion.pocEmailAddress != "") {
            party.addEmailAddress(cprtMetadataVersion.pocEmailAddress);
        }
        return party;
    }

    private Metadata buildMetadata(Catalog catalog) {
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
        Resource frameworkVersionLinkResource = new Resource();
        frameworkVersionLinkResource.setTitle(MarkupLine.fromMarkdown(cprtMetadataVersion.frameworkVersionName));
        Rlink frameworkVersionWebsiteRlink = new Rlink();
        frameworkVersionWebsiteRlink.setHref(URI.create(cprtMetadataVersion.frameworkWebSite));
        frameworkVersionWebsiteRlink.setMediaType("application/html");
        frameworkVersionLinkResource.addRlink(frameworkVersionWebsiteRlink);
        metadata.addLink(newLinkRel(catalog, frameworkVersionLinkResource, "alternate"));

        // Add party information
        Party nistParty = buildNistParty();
        metadata.addParty(nistParty);

        Role publisherRole = new Role();
        publisherRole.setId("publisher");
        metadata.addRole(publisherRole);

        ResponsibleParty publisherResponsibleParty = new ResponsibleParty();
        publisherResponsibleParty.setRoleId(publisherRole.getId());
        publisherResponsibleParty.addPartyUuid(nistParty.getUuid());
        metadata.addResponsibleParty(publisherResponsibleParty);

        Role contactRole = new Role();
        contactRole.setId("contact");
        metadata.addRole(contactRole);

        ResponsibleParty contactResponsibleParty = new ResponsibleParty();
        contactResponsibleParty.setRoleId(contactRole.getId());
        contactResponsibleParty.addPartyUuid(nistParty.getUuid());
        metadata.addResponsibleParty(contactResponsibleParty);
        
        return metadata;
    }

    /*
     * Given a catalog containing minimal metadata, hydrate the catalog with the CPRT data
     */
    protected abstract void hydrateCatalog(Catalog catalog);

    // Helpers

    protected Stream<CprtElement> getRelatedElementsBySourceIdWithType(String sourceId, String type) {
        return cprtRoot.getRelationships().stream()
            .filter(rel -> rel.getSourceGlobalIdentifier().equals(sourceId))
            .map(rel -> cprtRoot.getElementById(rel.getDestGlobalIdentifier()))
            .filter(elem -> elem.element_type.equals(type));
    }

    /**
     * Escape square brackets in the input string to avoid confusing OSCAL's param syntax.
     */
    protected String escapeSquareBrackets(String input) {
        return input.replaceAll("\\[", "(").replaceAll("\\]", ")");
    }
}
