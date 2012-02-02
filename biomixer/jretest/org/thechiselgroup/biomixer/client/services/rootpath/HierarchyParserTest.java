package org.thechiselgroup.biomixer.client.services.rootpath;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.thechiselgroup.biomixer.shared.core.test.matchers.collections.CollectionMatchers.containsExactly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thechiselgroup.biomixer.client.Concept;
import org.thechiselgroup.biomixer.client.core.resources.Resource;
import org.thechiselgroup.biomixer.client.core.resources.UriList;
import org.thechiselgroup.biomixer.server.core.util.IOUtils;
import org.thechiselgroup.biomixer.server.workbench.util.xml.StandardJavaXMLDocumentProcessor;

public class HierarchyParserTest {

    private HierarchyParser underTest;

    public void assertResourceHasChildren(String virtualOntologyId,
            Resource resource, String... childrenShortIds) {
        assertUrisContainIds(virtualOntologyId,
                resource.getUriListValue(Concept.CHILD_CONCEPTS),
                childrenShortIds);
    }

    public void assertResourceHasParents(String virtualOntologyId,
            Resource resource, String... parentShortIds) {
        assertUrisContainIds(virtualOntologyId,
                resource.getUriListValue(Concept.PARENT_CONCEPTS),
                parentShortIds);
    }

    private void assertUrisContainIds(String virtualOntologyId,
            UriList uriList, String... shortIds) {
        for (String shortId : shortIds) {
            assertTrue(uriList.contains(Concept.toConceptURI(virtualOntologyId,
                    shortId)));
        }
    }

    private List<String> getAllShortIds(List<Resource> pathResources) {
        List<String> shortIds = new ArrayList<String>();
        for (Resource resource : pathResources) {
            shortIds.add((String) resource.getValue(Concept.SHORT_ID));
        }
        return shortIds;
    }

    private Resource getResource(String string, List<Resource> pathResources) {
        for (Resource resource : pathResources) {
            if (((String) resource.getValue(Concept.SHORT_ID)).equals(string)) {
                return resource;
            }
        }
        Assert.fail();
        return null;
    }

    private List<Resource> getResourcePath(String conceptShortId,
            String xmlFilename, String virtualOntologyId) throws Exception {
        String responseXml = IOUtils.readIntoString(HierarchyParserTest.class
                .getResourceAsStream(xmlFilename));

        return underTest.parse(conceptShortId, responseXml, virtualOntologyId);
    }

    @Test
    public void parseSingleHierarchyLengthFour() throws Exception {
        String virtualOntologyId = "1487";
        List<Resource> pathResources = getResourcePath(
                "SympatheticNervousSystem",
                "single_hierarchy_length_four.response", virtualOntologyId);

        assertThat(pathResources.size(), equalTo(4));
        assertThat(getAllShortIds(pathResources),
                containsExactly(Arrays
                        .asList("SympatheticNervousSystem", "BodySystem",
                                "NervousSystem", "AutonomicNervousSystem")));
        assertResourceHasChildren(virtualOntologyId,
                getResource("BodySystem", pathResources), "NervousSystem");
        assertResourceHasChildren(virtualOntologyId,
                getResource("NervousSystem", pathResources),
                "AutonomicNervousSystem");
        assertResourceHasChildren(virtualOntologyId,
                getResource("AutonomicNervousSystem", pathResources),
                "SympatheticNervousSystem");
    }

    @Test
    public void parseTwoHierarchies() throws Exception {
        String virtualOntologyId = "1070";
        List<Resource> pathResources = getResourcePath("GO:0007569",
                "two_hierarchies.response", virtualOntologyId);

        assertThat(pathResources.size(), equalTo(5));
        assertThat(getAllShortIds(pathResources),
                containsExactly(Arrays.asList("GO:0008150", "GO:0009987",
                        "GO:0032502", "GO:0007568", "GO:0007569")));
        assertResourceHasChildren(virtualOntologyId,
                getResource("GO:0008150", pathResources), "GO:0032502",
                "GO:0009987");
        assertResourceHasChildren(virtualOntologyId,
                getResource("GO:0032502", pathResources), "GO:0007568");
        assertResourceHasChildren(virtualOntologyId,
                getResource("GO:0007568", pathResources), "GO:0007569");
        assertResourceHasChildren(virtualOntologyId,
                getResource("GO:0009987", pathResources), "GO:0007569");

        assertResourceHasParents(virtualOntologyId,
                getResource("GO:0007569", pathResources), "GO:0007568",
                "GO:0009987");
    }

    @Before
    public void setUp() throws Exception {
        underTest = new HierarchyParser(new StandardJavaXMLDocumentProcessor());
    }

}
