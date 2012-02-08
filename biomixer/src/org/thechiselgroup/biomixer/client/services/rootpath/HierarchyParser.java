package org.thechiselgroup.biomixer.client.services.rootpath;

import java.util.ArrayList;
import java.util.List;

import org.thechiselgroup.biomixer.client.Concept;
import org.thechiselgroup.biomixer.client.core.resources.Resource;
import org.thechiselgroup.biomixer.client.services.AbstractXMLResultParser;
import org.thechiselgroup.biomixer.shared.workbench.util.xml.DocumentProcessor;

import com.google.inject.Inject;

public class HierarchyParser extends AbstractXMLResultParser {

    private static final String DELIMITER = "\\.";

    @Inject
    public HierarchyParser(DocumentProcessor documentProcessor) {
        super(documentProcessor);
    }

    public Resource getResourceFromAlreadyFound(String conceptShortId,
            List<Resource> alreadyFound) {
        for (Resource resource : alreadyFound) {
            if (((String) resource.getValue(Concept.SHORT_ID))
                    .equals(conceptShortId)) {
                return resource;
            }
        }
        return null;
    }

    public List<Resource> parse(String targetShortConceptId, String xmlText,
            String virtualOntologyId) throws Exception {
        List<Resource> resourcesOnPaths = new ArrayList<Resource>();

        Resource targetResource = new Resource(Concept.toConceptURI(
                virtualOntologyId, targetShortConceptId));
        targetResource.putValue(Concept.SHORT_ID, targetShortConceptId);
        targetResource.putValue(Concept.VIRTUAL_ONTOLOGY_ID, virtualOntologyId);
        resourcesOnPaths.add(targetResource);

        Object rootNode = parseDocument(xmlText);
        Object[] paths = getNodes(rootNode, "//success/data/list/classBean");

        for (Object path : paths) {
            Object[] entries = getNodes(path, "relations/entry/string[last()]");
            assert entries.length == 1;
            String pathIds = getText(entries[0], "text()");
            String[] shortIds = pathIds.split(DELIMITER);

            if (shortIds.length == 0) {
                continue;
            }

            Resource previousResource = null;
            for (String shortId : shortIds) {
                Resource alreadyFound = getResourceFromAlreadyFound(shortId,
                        resourcesOnPaths);

                Resource concept;
                if (alreadyFound == null) {
                    concept = new Resource(Concept.toConceptURI(
                            virtualOntologyId, shortId));
                    concept.putValue(Concept.SHORT_ID, shortId);
                    concept.putValue(Concept.VIRTUAL_ONTOLOGY_ID,
                            virtualOntologyId);
                    resourcesOnPaths.add(concept);
                } else {
                    concept = alreadyFound;
                }

                if (previousResource != null) {
                    concept.addParent(previousResource.getUri());
                    previousResource.addChild(concept.getUri());
                }

                previousResource = concept;
            }

            // link last resource with the target
            previousResource.addChild(targetResource.getUri());
            targetResource.addParent(previousResource.getUri());
        }

        return resourcesOnPaths;
    }
}
