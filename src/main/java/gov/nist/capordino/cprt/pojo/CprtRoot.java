package gov.nist.capordino.cprt.pojo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CprtRoot {
    @JsonProperty
    private ArrayList<CprtDocument> documents;

    public List<CprtDocument> getDocuments() {
        return Collections.unmodifiableList(documents);
    }

    @JsonProperty
    private ArrayList<CprtRelationshipType> relationship_types;

    public List<CprtRelationshipType> getRelationshipTypes() {
        return Collections.unmodifiableList(relationship_types);
    }

    @JsonProperty
    private ArrayList<CprtElement> elements;

    public List<CprtElement> getElements() {
        return Collections.unmodifiableList(elements);
    }

    @JsonProperty
    private ArrayList<CprtRelationship> relationships;

    public List<CprtRelationship> getRelationships() {
        return Collections.unmodifiableList(relationships);
    }

    /**
     * Map of elements by their global identifier
     */
    @JsonIgnore
    private HashMap<String, CprtElement> elementMap;
    
    /**
     * Map of relationships by their source element identifier
     */
    @JsonIgnore
    private HashMap<String, ArrayList<CprtRelationship>> elementSourceRelationshipsMap;

    /**
     * Map of relationships by their destination element identifier
     */
    @JsonIgnore
    private HashMap<String, ArrayList<CprtRelationship>> elementDestinationRelationshipsMap;

    protected void calculateMaps() {
        elementMap = new HashMap<String, CprtElement>();
        for (CprtElement element : elements) {
            elementMap.put(element.getGlobalIdentifier(), element);
        }

        elementSourceRelationshipsMap = new HashMap<String, ArrayList<CprtRelationship>>();
        elementDestinationRelationshipsMap = new HashMap<String, ArrayList<CprtRelationship>>();
        for (CprtRelationship relationship : relationships) {
            CprtElement sourceElement = elementMap.get(relationship.getSourceGlobalIdentifier());
            CprtElement destElement = elementMap.get(relationship.getDestGlobalIdentifier());
            if (sourceElement != null) {
                ArrayList<CprtRelationship> sourceRelationships = elementSourceRelationshipsMap.get(sourceElement.getGlobalIdentifier());
                if (sourceRelationships == null) {
                    sourceRelationships = new ArrayList<CprtRelationship>();
                    elementSourceRelationshipsMap.put(sourceElement.getGlobalIdentifier(), sourceRelationships);
                }
                sourceRelationships.add(relationship);
            }
            if (destElement != null) {
                ArrayList<CprtRelationship> destRelationships = elementDestinationRelationshipsMap.get(destElement.getGlobalIdentifier());
                if (destRelationships == null) {
                    destRelationships = new ArrayList<CprtRelationship>();
                    elementDestinationRelationshipsMap.put(destElement.getGlobalIdentifier(), destRelationships);
                }
                destRelationships.add(relationship);
            }
        }
    }

    public CprtElement getElementById(String id) {
        if (elementMap == null) {
            calculateMaps();
        }
        return elementMap.get(id);
    }

    public List<CprtRelationship> getRelationshipsBySourceElementId(String id) {
        if (elementSourceRelationshipsMap == null) {
            calculateMaps();
        }
        ArrayList<CprtRelationship> relationships = elementSourceRelationshipsMap.get(id);
        if (relationships == null) {
            return Collections.emptyList();
        }
        return relationships;
    }

    public List<CprtRelationship> getRelationshipsByDestinationElementId(String id) {
        if (elementDestinationRelationshipsMap == null) {
            calculateMaps();
        }
        ArrayList<CprtRelationship> relationships = elementDestinationRelationshipsMap.get(id);
        if (relationships == null) {
            return Collections.emptyList();
        }
        return relationships;
    }
}
