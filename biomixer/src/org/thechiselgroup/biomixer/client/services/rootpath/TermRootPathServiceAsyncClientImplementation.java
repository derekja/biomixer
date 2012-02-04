package org.thechiselgroup.biomixer.client.services.rootpath;

import org.thechiselgroup.biomixer.client.Concept;
import org.thechiselgroup.biomixer.client.core.error_handling.ErrorHandler;
import org.thechiselgroup.biomixer.client.core.error_handling.ErrorHandlingAsyncCallback;
import org.thechiselgroup.biomixer.client.core.resources.Resource;
import org.thechiselgroup.biomixer.client.core.resources.ResourceList;
import org.thechiselgroup.biomixer.client.core.util.url.UrlBuilderFactory;
import org.thechiselgroup.biomixer.client.core.util.url.UrlFetchService;
import org.thechiselgroup.biomixer.client.services.AbstractXMLWebResourceService;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;

public class TermRootPathServiceAsyncClientImplementation extends
        AbstractXMLWebResourceService implements TermRootPathServiceAsync {

    @Inject
    private ErrorHandler errorHandler;

    @Inject
    private TermParentServiceAsync termParentService;

    @Inject
    public TermRootPathServiceAsyncClientImplementation(
            UrlFetchService urlFetchService, UrlBuilderFactory urlBuilderFactory) {
        super(urlFetchService, urlBuilderFactory);
    }

    @Override
    public void findPathToRoot(String virtualOntologyId, String fullConceptId,
            AsyncCallback<ResourceList> callback) {

        ResourceList resourcesFound = new ResourceList();
        loadFirstTerm(virtualOntologyId, fullConceptId, resourcesFound);
        callback.onSuccess(resourcesFound);
        // XXX the callback returns before any of the terms are loaded...
    }

    private void loadFirstTerm(String virtualOntologyId, String fullConceptId,
            ResourceList resourcesFound) {
        loadTerm(virtualOntologyId, fullConceptId, null, resourcesFound);
    }

    private void loadTerm(final String virtualOntologyId,
            final String fullConceptId, final Resource previous,
            final ResourceList resourcesFound) {

        termParentService.getNextResource(virtualOntologyId, fullConceptId,
                new ErrorHandlingAsyncCallback<Resource>(errorHandler) {

                    @Override
                    public void onFailure(Throwable caught) {
                        errorHandler.handleError(new Exception(
                                "Could not retrieve term information for "
                                        + fullConceptId, caught));
                    }

                    @Override
                    public void runOnSuccess(Resource resource)
                            throws Exception {

                        if (previous != null) {
                            resource.updateChildren(previous.getUri());
                        }

                        // if resource has already been found, merge them
                        if (resourcesFound.containsSameURI(resource)) {
                            Resource resourceWithSameURI = resourcesFound
                                    .getResourceWithSameURI(resource);
                            resourceWithSameURI.updateChildren(resource
                                    .getUriListValue(Concept.CHILD_CONCEPTS));
                            resourceWithSameURI.updateParents(resource
                                    .getUriListValue(Concept.PARENT_CONCEPTS));
                        } else {
                            resourcesFound.add(resource);
                        }

                        for (String parentUri : resource
                                .getUriListValue(Concept.PARENT_CONCEPTS)) {
                            String parentFullConceptId = Concept
                                    .extractFullConceptId(parentUri);
                            loadTerm(virtualOntologyId, parentFullConceptId,
                                    resource, resourcesFound);
                        }

                    }
                });

    }

}
