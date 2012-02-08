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

import java.util.ArrayList;
import java.util.List;

import org.thechiselgroup.biomixer.client.services.AbstractXMLResultParser;
import org.thechiselgroup.biomixer.shared.workbench.util.xml.DocumentProcessor;

import com.google.inject.Inject;

public class HierarchyParser extends AbstractXMLResultParser {

    private static final String DELIMITER = "\\.";

    @Inject
    public HierarchyParser(DocumentProcessor documentProcessor) {
        super(documentProcessor);
    }

    public List<String> parse(String targetShortConceptId, String xmlText,
            String virtualOntologyId) throws Exception {

        List<String> shortIdsOnPaths = new ArrayList<String>();
        shortIdsOnPaths.add(targetShortConceptId);

        Object rootNode = parseDocument(xmlText);
        Object[] paths = getNodes(rootNode, "//success/data/list/classBean");

        for (Object path : paths) {
            Object[] entries = getNodes(path, "relations/entry/string[last()]");
            assert entries.length == 1;
            String pathIds = getText(entries[0], "text()");
            String[] shortIds = pathIds.split(DELIMITER);

            // TODO remove - unnecessary, loop is skipped anyways when length=0
            if (shortIds.length == 0) {
                continue;
            }

            for (String shortId : shortIds) {
                // TODO if order does not matter, use set instead of contains
                // check
                if (!shortIdsOnPaths.contains(shortId)) {
                    shortIdsOnPaths.add(shortId);
                }
            }

        }

        return shortIdsOnPaths;
    }
}
