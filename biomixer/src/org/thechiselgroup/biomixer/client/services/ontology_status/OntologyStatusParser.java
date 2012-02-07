package org.thechiselgroup.biomixer.client.services.ontology_status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.thechiselgroup.biomixer.client.services.AbstractXMLResultParser;
import org.thechiselgroup.biomixer.shared.workbench.util.xml.DocumentProcessor;

import com.google.inject.Inject;

public class OntologyStatusParser extends AbstractXMLResultParser {

    @Inject
    public OntologyStatusParser(DocumentProcessor documentProcessor) {
        super(documentProcessor);
    }

    public Map<String, List<String>> parseStatuses(String xmlText)
            throws Exception {
        Map<String, List<String>> virtualOntologyIdsByStatus = new HashMap<String, List<String>>();

        Object root = parseDocument(xmlText);

        for (Object ontologyBean : getNodes(root,
                "//success/data/list/ontologyBean")) {

            String virtualOntologyId = getText(ontologyBean, "id/text()");
            String status = getText(ontologyBean, "status/text()");
            if (!virtualOntologyIdsByStatus.containsKey(status)) {
                virtualOntologyIdsByStatus.put(status, new ArrayList<String>());
            }
            virtualOntologyIdsByStatus.get(status).add(virtualOntologyId);
        }

        return virtualOntologyIdsByStatus;
    }
}
