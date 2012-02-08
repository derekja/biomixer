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
package org.thechiselgroup.biomixer.client;

import java.util.List;

import org.thechiselgroup.biomixer.client.core.error_handling.ErrorHandler;
import org.thechiselgroup.biomixer.client.core.error_handling.ErrorHandlingAsyncCallback;
import org.thechiselgroup.biomixer.client.core.resources.DefaultResourceSet;
import org.thechiselgroup.biomixer.client.core.resources.Resource;
import org.thechiselgroup.biomixer.client.core.resources.ResourceSet;
import org.thechiselgroup.biomixer.client.core.util.UriUtils;
import org.thechiselgroup.biomixer.client.core.visualization.DefaultView;
import org.thechiselgroup.biomixer.client.core.visualization.View;
import org.thechiselgroup.biomixer.client.dnd.windows.ViewWindowContent;
import org.thechiselgroup.biomixer.client.dnd.windows.WindowContentProducer;
import org.thechiselgroup.biomixer.client.services.ontology_status.OntologyStatusServiceAsync;
import org.thechiselgroup.biomixer.client.services.rootpath.HierarchyPathServiceAsync;
import org.thechiselgroup.biomixer.client.services.rootpath.TermParentServiceAsync;
import org.thechiselgroup.biomixer.client.services.term.ConceptNeighbourhoodServiceAsync;
import org.thechiselgroup.biomixer.client.services.term.TermServiceAsync;
import org.thechiselgroup.biomixer.client.visualization_component.graph.Graph;
import org.thechiselgroup.biomixer.client.visualization_component.graph.GraphLayoutSupport;
import org.thechiselgroup.biomixer.client.visualization_component.graph.ResourceNeighbourhood;
import org.thechiselgroup.biomixer.client.visualization_component.graph.layouts.VerticalTreeLayout;
import org.thechiselgroup.biomixer.client.workbench.embed.EmbeddedViewLoader;
import org.thechiselgroup.biomixer.client.workbench.init.WindowLocation;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;

public class HierarchyPathLoader implements EmbeddedViewLoader {

    public static final String EMBED_MODE = "hierarchy_to_root";

    @Inject
    private HierarchyPathServiceAsync hierarchyPathService;

    @Inject
    private TermServiceAsync termService;

    @Inject
    private TermParentServiceAsync termParentService;

    @Inject
    private ConceptNeighbourhoodServiceAsync conceptNeighbourhoodService;

    @Inject
    private WindowContentProducer windowContentProducer;

    @Inject
    private ErrorHandler errorHandler;

    @Inject
    private OntologyStatusServiceAsync ontologyStatusService;

    private void checkOntologyStatus(final View graphView,
            final String fullConceptId, final String virtualOntologyId) {

        ontologyStatusService
                .getAvailableOntologies(new ErrorHandlingAsyncCallback<List<String>>(
                        errorHandler) {

                    @Override
                    public void onFailure(Throwable caught) {
                        errorHandler
                                .handleError(new Exception(
                                        "Could not retrieve status information for ontologies",
                                        caught));
                    }

                    @Override
                    protected void runOnSuccess(
                            List<String> availableVirtualOntologyIds)
                            throws Exception {

                        if (availableVirtualOntologyIds
                                .contains(virtualOntologyId)) {
                            loadUsingHierarchyService((DefaultView) graphView,
                                    virtualOntologyId, fullConceptId);
                        } else {
                            loadUsingRecursiveTermService(
                                    (DefaultView) graphView, virtualOntologyId,
                                    fullConceptId);
                        }

                    }

                });
    }

    private void doLoadHierarchyData(final DefaultView view,
            final String virtualOntologyId, final String conceptId) {

        hierarchyPathService.findHierarchyToRoot(virtualOntologyId, conceptId,
                new ErrorHandlingAsyncCallback<List<Resource>>(errorHandler) {

                    private void addResourcesToView(final DefaultView view,
                            final ResourceSet resourceSet,
                            final int numberOfResourcesRetrieved) {

                        if (resourceSet.size() < numberOfResourcesRetrieved) {
                            /*
                             * Waiting for additional term information to be
                             * retrieved for resources on path hierarchy
                             */
                            new Timer() {
                                @Override
                                public void run() {
                                    addResourcesToView(view, resourceSet,
                                            numberOfResourcesRetrieved);
                                }
                            }.schedule(50);
                        } else {
                            for (final Resource resource : resourceSet) {
                                final String currentConceptFullId = (String) resource
                                        .getValue(Concept.FULL_ID);
                                // TODO why do we need to get the concept
                                // neighbourhood?
                                conceptNeighbourhoodService
                                        .getNeighbourhood(
                                                virtualOntologyId,
                                                currentConceptFullId,
                                                new ErrorHandlingAsyncCallback<ResourceNeighbourhood>(
                                                        errorHandler) {

                                                    @Override
                                                    public void onFailure(
                                                            Throwable caught) {
                                                        errorHandler
                                                                .handleError(new Exception(
                                                                        "Could not expand neighbourhood for "
                                                                                + currentConceptFullId,
                                                                        caught));
                                                    }

                                                    @Override
                                                    protected void runOnSuccess(
                                                            ResourceNeighbourhood result)
                                                            throws Exception {
                                                        resource.applyPartialProperties(result
                                                                .getPartialProperties());
                                                        view.getResourceModel()
                                                                .addResourceSet(
                                                                        resourceSet);
                                                        layout(view);
                                                    }

                                                });

                            }

                        }
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        errorHandler.handleError(new Exception(
                                "Could not retrieve hierarchy to root for "
                                        + conceptId, caught));
                    }

                    @Override
                    protected void runOnSuccess(
                            List<Resource> hierarchyResources) throws Exception {

                        // TODO use inject resource set factory
                        ResourceSet resourceSet = new DefaultResourceSet();
                        for (Resource resource : hierarchyResources) {
                            getBasicInformationForResourceAndAddToResourceSet(
                                    resource, virtualOntologyId, resourceSet);
                        }
                        // TODO a better alternative might be to pass
                        // the hierarchyResources.size into
                        // getBasicInformation...
                        // and check at the end of the method if all information
                        // has been retrieved (and to do the next thing if so).
                        // This would eliminate the
                        // wait with the timer. Also, the neighbourhood
                        // for a resource can immediately be retrieved after
                        // the basic info has been loaded (and is loading
                        // the basic info necessary? the info might be part of
                        // the response
                        // for the neighbourhood).
                        addResourcesToView(view, resourceSet,
                                hierarchyResources.size());
                    }
                });
    }

    private void getBasicInformationForResourceAndAddToResourceSet(
            final Resource resource, final String virtualOntologyId,
            final ResourceSet resourceSet) {

        final String conceptId = (String) resource.getValue(Concept.SHORT_ID);
        termService.getBasicInformation(virtualOntologyId, conceptId,
                new ErrorHandlingAsyncCallback<Resource>(errorHandler) {

                    @Override
                    public void onFailure(Throwable caught) {
                        errorHandler.handleError(new Exception(
                                "Could not retrieve basic information for "
                                        + conceptId, caught));
                    }

                    @Override
                    protected void runOnSuccess(Resource result)
                            throws Exception {

                        String fullId = (String) result
                                .getValue(Concept.FULL_ID);
                        String label = (String) result.getValue(Concept.LABEL);
                        String type = (String) result.getValue(Concept.TYPE);

                        resource.putValue(Concept.FULL_ID, fullId);
                        resource.putValue(Concept.LABEL, label);
                        resource.putValue(Concept.TYPE, type);

                        resource.regenerateUri();
                        resourceSet.add(resource);
                    }
                });
    }

    @Override
    public String getEmbedMode() {
        return EMBED_MODE;
    }

    private void layout(final DefaultView view) {
        new Timer() {
            @Override
            public void run() {
                view.adaptTo(GraphLayoutSupport.class).runLayout(
                        new VerticalTreeLayout());
            }
        }.schedule(50);
    }

    private void loadFirstTerm(String virtualOntologyId, String fullConceptId,
            DefaultView view) {
        loadTerm(virtualOntologyId, fullConceptId, null, view);
    }

    private void loadHierarchyData(final DefaultView view,
            final String virtualOntologyId, final String conceptId) {

        if (view.isReady()) {
            doLoadHierarchyData(view, virtualOntologyId, conceptId);
        } else {
            new Timer() {
                @Override
                public void run() {
                    loadHierarchyData(view, virtualOntologyId, conceptId);
                }
            }.schedule(50);
        }
    }

    private void loadTerm(final String virtualOntologyId,
            final String fullConceptId, final Resource previous,
            final DefaultView view) {

        System.out.println("Starting recursive term service");
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
                            resource.addChild(previous.getUri());
                        }

                        // TODO inject resource model & use directly
                        // if resource has already been found, merge them
                        Resource resourceWithSameUri = view.getResourceModel()
                                .getResources().getByUri(resource.getUri());
                        if (resourceWithSameUri != null) {
                            resourceWithSameUri.addChildren(resource
                                    .getUriListValue(Concept.CHILD_CONCEPTS));
                        } else {
                            // TODO use automatic resource set
                            ResourceSet resourceSet = new DefaultResourceSet();
                            resourceSet.add(resource);
                            view.getResourceModel().addResourceSet(resourceSet);
                        }

                        layout(view);

                        // XXX is there a problem here with resources that have
                        // been merged, following their parents multiple times?
                        for (String parentUri : resource
                                .getUriListValue(Concept.PARENT_CONCEPTS)) {
                            String parentFullConceptId = Concept
                                    .getConceptId(parentUri);
                            loadTerm(virtualOntologyId, parentFullConceptId,
                                    resource, view);
                        }

                    }
                });

    }

    private void loadUsingHierarchyService(final DefaultView graphView,
            final String virtualOntologyId, final String fullConceptId) {

        // need to look up short id since that is what the hierarchy service
        // requires as a parameter
        termService.getBasicInformation(virtualOntologyId, fullConceptId,
                new ErrorHandlingAsyncCallback<Resource>(errorHandler) {

                    @Override
                    public void onFailure(Throwable caught) {
                        errorHandler.handleError(new Exception(
                                "Could not retrieve basic information for "
                                        + fullConceptId, caught));
                    }

                    @Override
                    protected void runOnSuccess(Resource result)
                            throws Exception {

                        System.out.println("Got short id");
                        String shortId = (String) result
                                .getValue(Concept.SHORT_ID);
                        loadHierarchyData(graphView, virtualOntologyId, shortId);
                    }

                });
    }

    private void loadUsingRecursiveTermService(final DefaultView view,
            final String virtualOntologyId, final String fullConceptId) {

        if (view.isReady()) {
            loadFirstTerm(virtualOntologyId, fullConceptId, view);
        } else {
            new Timer() {

                @Override
                public void run() {
                    loadUsingRecursiveTermService(view, virtualOntologyId,
                            fullConceptId);

                }

            }.schedule(50);
        }
    }

    @Override
    public void loadView(WindowLocation windowLocation,
            AsyncCallback<View> callback) {
        final View graphView = ((ViewWindowContent) windowContentProducer
                .createWindowContent(Graph.ID)).getView();
        graphView.init();
        callback.onSuccess(graphView);

        final String fullConceptId = UriUtils.decodeURIComponent(windowLocation
                .getParameter("full_concept_id"));
        final String virtualOntologyId = windowLocation
                .getParameter("virtual_ontology_id");

        checkOntologyStatus(graphView, fullConceptId, virtualOntologyId);
    }

}
