package org.thechiselgroup.biomixer.client.services.rootpath;

import org.thechiselgroup.biomixer.client.core.util.UriUtils;
import org.thechiselgroup.biomixer.client.core.util.transform.Transformer;
import org.thechiselgroup.biomixer.client.core.util.url.UrlBuilder;
import org.thechiselgroup.biomixer.client.core.util.url.UrlBuilderFactory;
import org.thechiselgroup.biomixer.client.core.util.url.UrlFetchService;
import org.thechiselgroup.biomixer.client.services.AbstractXMLWebResourceService;
import org.thechiselgroup.biomixer.client.visualization_component.graph.ResourcePath;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;

public class RootPathServiceAsyncClientImplementation extends
        AbstractXMLWebResourceService implements RootPathServiceAsync {

    private final RootPathParser resultParser;

    @Inject
    public RootPathServiceAsyncClientImplementation(
            UrlFetchService urlFetchService,
            UrlBuilderFactory urlBuilderFactory, RootPathParser resultParser) {

        super(urlFetchService, urlBuilderFactory);

        this.resultParser = resultParser;
    }

    private String buildUrl(final String conceptId,
            final String ontologyVersionId) {
        UrlBuilder urlBuilder = urlBuilderFactory.createUrlBuilder();
        String path = "/bioportal/path/" + ontologyVersionId + "/";
        urlBuilder.setPath(path);
        urlBuilder.setParameter("source",
                UriUtils.encodeURIComponent(conceptId));
        urlBuilder.setParameter("target", "root");
        return urlBuilder.buildString();
    }

    @Override
    public void findPathToRoot(final String virtualOntologyId,
            final String ontologyVersionId, final String fullConceptId,
            final AsyncCallback<ResourcePath> callback) {

        String url = buildUrl(fullConceptId, ontologyVersionId);
        fetchUrl(callback, url, new Transformer<String, ResourcePath>() {
            @Override
            public ResourcePath transform(String xmlText) throws Exception {
                return resultParser.parse(ontologyVersionId, virtualOntologyId,
                        fullConceptId, xmlText);
            }

        });

    }

}
