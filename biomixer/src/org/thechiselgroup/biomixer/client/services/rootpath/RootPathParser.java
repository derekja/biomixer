package org.thechiselgroup.biomixer.client.services.rootpath;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.thechiselgroup.biomixer.client.Concept;
import org.thechiselgroup.biomixer.client.core.resources.Resource;
import org.thechiselgroup.biomixer.client.core.resources.UriList;
import org.thechiselgroup.biomixer.client.core.util.collections.CollectionFactory;
import org.thechiselgroup.biomixer.client.services.AbstractXMLResultParser;
import org.thechiselgroup.biomixer.client.visualization_component.graph.ResourcePath;
import org.thechiselgroup.biomixer.shared.workbench.util.xml.DocumentProcessor;
import org.thechiselgroup.biomixer.shared.workbench.util.xml.XPathEvaluationException;

import com.google.inject.Inject;

public class RootPathParser extends AbstractXMLResultParser {

    // TODO put this is one standard place for both this parser and
    // FullTermResponseParser
    private static final String REVERSE_PREFIX = "[R]";

    private Resource target;

    private String ontologyVersionId;

    private String virtualOntologyId;

    private String fullConceptId;

    private List<Resource> resources;

    private List<Object> processLater;

    private List<String> subclassOrSuperclassConceptIds;

    @Inject
    public RootPathParser(DocumentProcessor documentProcessor) {
        super(documentProcessor);
    }

    // XXX duplicated from FullTermResponseParser
    private String getConceptId(Object r) throws XPathEvaluationException {
        return getText(r, "fullId/text()");
    }

    private void initializeState(String ontologyVersionId,
            String virtualOntologyId, String fullConceptId) {
        // XXX don't know if these should really be fields. It avoids passing
        // them all as parameters to traverseLayer though.
        // TODO: put in constructor. Not sure how injection works yet so leaving
        // them here for the moment
        this.ontologyVersionId = ontologyVersionId;
        this.virtualOntologyId = virtualOntologyId;
        this.fullConceptId = fullConceptId;
        target = null;
        resources = new ArrayList<Resource>();
        processLater = new ArrayList<Object>();
        subclassOrSuperclassConceptIds = new ArrayList<String>();
    }

    public ResourcePath parse(String ontologyVersionId,
            String virtualOntologyId, String fullConceptId, String xmlText)
            throws Exception {

        initializeState(ontologyVersionId, virtualOntologyId, fullConceptId);

        Object rootNode = parseDocument(xmlText);

        // This is the top level node, i.e. "Thing"
        Object[] nodes = getNodes(rootNode, "//success/data/classBean");
        assert nodes.length == 1;

        traverseLayer(nodes[0], null);
        assert target != null;

        // XXX don't really understand why this is needed yet
        for (Object n : processLater) {
            if (subclassOrSuperclassConceptIds.contains(getConceptId(n))) {
                continue;
            }

            process(n, new UriList(), new UriList());
        }

        return new ResourcePath(target, resources);
    }

    private Resource process(Object node, UriList parentConcepts,
            UriList childConcepts) throws XPathEvaluationException {

        String conceptId = getConceptId(node);
        String conceptShortId = getText(node, "id/text()");
        String label = getText(node, "label/text()");

        int childCount = 0;
        Object[] nodes = getNodes(node,
                "relations/entry[string/text()=\"ChildCount\"]");

        if (nodes.length > 0) {
            childCount = Integer.parseInt(getText(nodes[0], "int/text()"));
        }

        Resource concept = new Resource(Concept.toConceptURI(virtualOntologyId,
                conceptId));

        concept.putValue(Concept.FULL_ID, conceptId);
        concept.putValue(Concept.SHORT_ID, conceptShortId);
        concept.putValue(Concept.LABEL, label);
        concept.putValue(Concept.VIRTUAL_ONTOLOGY_ID, virtualOntologyId);
        concept.putValue(Concept.CONCEPT_CHILD_COUNT,
                Integer.valueOf(childCount));

        // TODO: parent concepts?
        // childConcepts.add(concept.getUri());

        return concept;

    }

    private void traverseLayer(Object entryNode, Resource previous)
            throws XPathEvaluationException {

        Object[] nodes = getNodes(entryNode, "relations/entry");

        if (nodes.length == 0) {
            /* End of branch */
            return;
        }

        for (int i = 0; i < nodes.length; i++) {
            Object node = nodes[i];

            Object[] relationships = getNodes(node, "list/classBean");

            if (relationships.length == 0) {
                continue;
            }

            // Move parents and child concepts uri list here and don't pass it
            // along to next layer
            UriList parentConcepts = new UriList();
            UriList childConcepts = new UriList();
            Map<String, Serializable> partialPropertiesForPrevious = CollectionFactory
                    .createStringMap();
            Map<String, Serializable> partialPropertiesForChildren = CollectionFactory
                    .createStringMap();

            if (previous != null) {
                parentConcepts.add(previous.getUri());
                partialPropertiesForChildren.put(Concept.PARENT_CONCEPTS,
                        parentConcepts);
                // apply this to each child found
            }

            String name = getText(node, "string/text()");
            // TODO deal with reversed names

            for (int j = 0; j < relationships.length; j++) {
                Object relationship = relationships[j];

                if (!("SubClass".equals(name) || "SuperClass".equals(name))) {
                    processLater.add(relationship);
                    continue;
                }

                Resource resource = process(relationship, parentConcepts,
                        childConcepts);

                resource.applyPartialProperties(partialPropertiesForChildren);
                childConcepts.add(resource.getUri());

                // TODO: find out what 'OMV:' prefix means, find better way of
                // handling
                if (!resource.getValue(Concept.FULL_ID).equals(fullConceptId)
                        && !resource.getValue(Concept.SHORT_ID).equals(
                                "OMV:" + fullConceptId)) {
                    resources.add(resource);
                    subclassOrSuperclassConceptIds.add(Concept
                            .getFullId(resource));
                } else {
                    /* Found the node with specified concept id */
                    target = resource;
                }

                /* Check for greater depths to traverse */
                traverseLayer(relationship, resource);
            }

            if (previous != null) {
                partialPropertiesForPrevious.put(Concept.CHILD_CONCEPTS,
                        childConcepts);
                previous.applyPartialProperties(partialPropertiesForPrevious);
            }

        }
    }
}
