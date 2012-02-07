package org.thechiselgroup.biomixer.client.services.ontology_status;

import java.util.List;
import java.util.Map;

import org.thechiselgroup.biomixer.client.core.util.transform.Transformer;
import org.thechiselgroup.biomixer.client.core.util.url.UrlBuilder;
import org.thechiselgroup.biomixer.client.core.util.url.UrlBuilderFactory;
import org.thechiselgroup.biomixer.client.core.util.url.UrlFetchService;
import org.thechiselgroup.biomixer.client.services.AbstractXMLWebResourceService;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;

public class OntologyStatusServiceAsyncClientImplementation extends
        AbstractXMLWebResourceService implements OntologyStatusServiceAsync {

    private OntologyStatusParser parser;

    @Inject
    public OntologyStatusServiceAsyncClientImplementation(
            UrlFetchService urlFetchService,
            UrlBuilderFactory urlBuilderFactory, OntologyStatusParser parser) {
        super(urlFetchService, urlBuilderFactory);
        this.parser = parser;
    }

    private String buildUrl() {
        UrlBuilder urlBuilder = urlBuilderFactory.createUrlBuilder();
        String path = "/obs/ontologies";
        urlBuilder.setPath(path);
        return urlBuilder.buildString();
    }

    @Override
    public void getOntologyStatuses(
            AsyncCallback<Map<String, List<String>>> callback) {

        String url = buildUrl();
        fetchUrl(callback, url,
                new Transformer<String, Map<String, List<String>>>() {

                    @Override
                    public Map<String, List<String>> transform(String xmlText)
                            throws Exception {
                        return parser.parseStatuses(xmlText);
                    }

                });

    }

}
