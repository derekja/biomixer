/*******************************************************************************
 * Copyright 2012 David Rusk 
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 *
 *    http://www.apache.org/licenses/LICENSE-2.0 
 *     
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 *******************************************************************************/
package org.thechiselgroup.biomixer.client.services.rootpath;

import org.thechiselgroup.biomixer.client.Concept;
import org.thechiselgroup.biomixer.client.core.resources.Resource;
import org.thechiselgroup.biomixer.client.core.resources.UriList;
import org.thechiselgroup.biomixer.client.services.AbstractXMLResultParser;
import org.thechiselgroup.biomixer.shared.workbench.util.xml.DocumentProcessor;

import com.google.inject.Inject;

public class TermParentParser extends AbstractXMLResultParser {

    private static final String OWL_THING = "owl:Thing";

    @Inject
    public TermParentParser(DocumentProcessor documentProcessor) {
        super(documentProcessor);
    }

    public Resource parse(String virtualOntologyId, String xmlText)
            throws Exception {

        Object baseNode = parseDocument(xmlText);
        Object[] classBeans = getNodes(baseNode, "//success/data/classBean");
        assert classBeans.length == 1;
        Object queriedTerm = classBeans[0];

        String shortId = getText(queriedTerm, "id/text()");
        String fullId = getText(queriedTerm, "fullId/text()");
        String label = getText(queriedTerm, "label/text()");
        String type = getText(queriedTerm, "type/text()");

        int childCount = 0;
        Object[] childCountNodes = getNodes(queriedTerm,
                "relations/entry[string/text()=\"ChildCount\"]");
        if (childCountNodes.length > 0) {
            childCount = Integer.parseInt(getText(childCountNodes[0],
                    "int/text()"));
        }

        Object[] superClassBeans = getNodes(queriedTerm,
                "relations/entry[string/text()=\"SuperClass\"]/list/classBean");

        UriList parentUris = new UriList();
        for (Object superClassBean : superClassBeans) {
            if (getText(superClassBean, "id/text()").equals(OWL_THING)) {
                // don't show owl:Thing in the hierarchy
                continue;
            }
            String parentFullId = getText(superClassBean, "fullId/text()");
            parentUris.add(Concept
                    .toConceptURI(virtualOntologyId, parentFullId));
        }

        Resource resource = new Resource(Concept.toConceptURI(
                virtualOntologyId, fullId));
        resource.putValue(Concept.FULL_ID, fullId);
        resource.putValue(Concept.VIRTUAL_ONTOLOGY_ID, virtualOntologyId);
        resource.putValue(Concept.SHORT_ID, shortId);
        resource.putValue(Concept.LABEL, label);
        resource.putValue(Concept.TYPE, type);
        resource.putValue(Concept.CONCEPT_CHILD_COUNT,
                Integer.valueOf(childCount));
        resource.putValue(Concept.PARENT_CONCEPTS, parentUris);

        return resource;
    }

}
