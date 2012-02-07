package org.thechiselgroup.biomixer.client.services.ontology_status;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.thechiselgroup.biomixer.server.core.util.IOUtils;
import org.thechiselgroup.biomixer.server.workbench.util.xml.StandardJavaXMLDocumentProcessor;

public class OntologyStatusParserTest {

    private OntologyStatusParser underTest;

    @Test
    public void checkVirtualOntologyIdAssociatedWithCorrectStatus()
            throws Exception {
        Map<String, List<String>> virtualOntologyIdsByStatus = parseResponse("20120206_statuses.response");
        assertTrue(virtualOntologyIdsByStatus.get("28").contains("945"));
        assertTrue(virtualOntologyIdsByStatus.get("28").contains("1064"));
        assertTrue(virtualOntologyIdsByStatus.get("99").contains("821"));
    }

    public Map<String, List<String>> parseResponse(String xmlFilename)
            throws Exception {
        String responseXML = IOUtils.readIntoString(OntologyStatusParser.class
                .getResourceAsStream(xmlFilename));
        return underTest.parseStatuses(responseXML);
    }

    @Before
    public void setUp() throws Exception {
        underTest = new OntologyStatusParser(
                new StandardJavaXMLDocumentProcessor());
    }

}
