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

import static org.junit.Assert.assertThat;
import static org.thechiselgroup.biomixer.client.services.rootpath.ResourceMatcher.equalsResource;

import org.junit.Before;
import org.junit.Test;
import org.thechiselgroup.biomixer.client.Concept;
import org.thechiselgroup.biomixer.client.core.resources.Resource;
import org.thechiselgroup.biomixer.client.core.resources.UriList;
import org.thechiselgroup.biomixer.server.core.util.IOUtils;
import org.thechiselgroup.biomixer.server.workbench.util.xml.StandardJavaXMLDocumentProcessor;

public class TermParentParserTest {

    private TermParentParser underTest;

    @Test
    public void parseAtRootOBO() throws Exception {
        String virtualOntologyId = "1070";
        String fullId = "http://purl.org/obo/owl/GO#GO_0008150";
        Resource expected = new Resource(Concept.toConceptURI(
                virtualOntologyId, fullId));
        expected.putValue(Concept.SHORT_ID, "GO:0008150");
        expected.putValue(Concept.FULL_ID, fullId);
        expected.putValue(Concept.CONCEPT_CHILD_COUNT, Integer.valueOf(28));
        expected.putValue(Concept.VIRTUAL_ONTOLOGY_ID, virtualOntologyId);
        expected.putValue(Concept.LABEL, "biological_process");
        expected.putValue(Concept.TYPE, "class");
        expected.putValue(Concept.PARENT_CONCEPTS, new UriList());

        Resource resourceReturned = parseResource(virtualOntologyId,
                "full_term_obo_at_root.response");
        assertThat(resourceReturned, equalsResource(expected));
    }

    @Test
    public void parseAtRootOWL() throws Exception {
        String virtualOntologyId = "1487";
        String fullId = "http://who.int/bodysystem.owl#BodySystem";
        Resource expected = new Resource(Concept.toConceptURI(
                virtualOntologyId, fullId));
        expected.putValue(Concept.SHORT_ID, "BodySystem");
        expected.putValue(Concept.FULL_ID, fullId);
        expected.putValue(Concept.CONCEPT_CHILD_COUNT, Integer.valueOf(15));
        expected.putValue(Concept.VIRTUAL_ONTOLOGY_ID, virtualOntologyId);
        expected.putValue(Concept.LABEL, "Body System");
        expected.putValue(Concept.TYPE, "class");
        expected.putValue(Concept.PARENT_CONCEPTS, new UriList());

        Resource resourceReturned = parseResource(virtualOntologyId,
                "full_term_owl_at_root.response");
        assertThat(resourceReturned, equalsResource(expected));
    }

    @Test
    public void parseOneParentOWL() throws Exception {
        String virtualOntologyId = "1487";
        String fullId = "http://who.int/bodysystem.owl#SympatheticNervousSystem";
        Resource expected = new Resource(Concept.toConceptURI(
                virtualOntologyId, fullId));
        expected.putValue(Concept.SHORT_ID, "SympatheticNervousSystem");
        expected.putValue(Concept.FULL_ID, fullId);
        expected.putValue(Concept.CONCEPT_CHILD_COUNT, Integer.valueOf(0));
        expected.putValue(Concept.VIRTUAL_ONTOLOGY_ID, virtualOntologyId);
        expected.putValue(Concept.LABEL, "Sympathetic Nervous System");
        expected.putValue(Concept.TYPE, "class");
        UriList parents = new UriList(Concept.toConceptURI(virtualOntologyId,
                "http://who.int/bodysystem.owl#AutonomicNervousSystem"));
        expected.putValue(Concept.PARENT_CONCEPTS, parents);

        Resource resourceReturned = parseResource(virtualOntologyId,
                "full_term_owl_one_parent.response");
        assertThat(resourceReturned, equalsResource(expected));
    }

    private Resource parseResource(String virtualOntologyId, String xmlFilename)
            throws Exception {
        String responseXML = IOUtils.readIntoString(RootPathParserTest.class
                .getResourceAsStream(xmlFilename));
        return underTest.parse(virtualOntologyId, responseXML);
    }

    @Test
    public void parseTwoParentsOBO() throws Exception {
        String virtualOntologyId = "1070";
        String fullId = "http://purl.org/obo/owl/GO#GO_0007569";
        Resource expected = new Resource(Concept.toConceptURI(
                virtualOntologyId, fullId));
        expected.putValue(Concept.SHORT_ID, "GO:0007569");
        expected.putValue(Concept.FULL_ID, fullId);
        expected.putValue(Concept.CONCEPT_CHILD_COUNT, Integer.valueOf(5));
        expected.putValue(Concept.VIRTUAL_ONTOLOGY_ID, virtualOntologyId);
        expected.putValue(Concept.LABEL, "cell aging");
        expected.putValue(Concept.TYPE, "class");
        UriList parents = new UriList();
        parents.add(Concept.toConceptURI(virtualOntologyId,
                "http://purl.org/obo/owl/GO#GO_0007568"));
        parents.add(Concept.toConceptURI(virtualOntologyId,
                "http://purl.org/obo/owl/GO#GO_0009987"));
        expected.putValue(Concept.PARENT_CONCEPTS, parents);

        Resource resourceReturned = parseResource(virtualOntologyId,
                "full_term_obo_two_parents.response");
        assertThat(resourceReturned, equalsResource(expected));
    }

    @Before
    public void setUp() throws Exception {
        underTest = new TermParentParser(new StandardJavaXMLDocumentProcessor());
    }

}
