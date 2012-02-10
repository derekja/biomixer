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
import java.util.Set;

import org.thechiselgroup.biomixer.client.core.error_handling.ErrorHandler;
import org.thechiselgroup.biomixer.client.core.error_handling.ErrorHandlingAsyncCallback;
import org.thechiselgroup.biomixer.client.core.resources.Resource;
import org.thechiselgroup.biomixer.client.core.util.UriUtils;
import org.thechiselgroup.biomixer.client.core.visualization.DefaultView;
import org.thechiselgroup.biomixer.client.core.visualization.View;
import org.thechiselgroup.biomixer.client.dnd.windows.ViewWindowContent;
import org.thechiselgroup.biomixer.client.dnd.windows.WindowContentProducer;
import org.thechiselgroup.biomixer.client.services.hierarchy.HierarchyPathServiceAsync;
import org.thechiselgroup.biomixer.client.services.ontology.OntologyStatusServiceAsync;
import org.thechiselgroup.biomixer.client.services.term.ConceptNeighbourhoodServiceAsync;
import org.thechiselgroup.biomixer.client.services.term.TermServiceAsync;
import org.thechiselgroup.biomixer.client.visualization_component.graph.Graph;
import org.thechiselgroup.biomixer.client.visualization_component.graph.GraphLayoutSupport;
import org.thechiselgroup.biomixer.client.visualization_component.graph.layouts.VerticalTreeLayout;
import org.thechiselgroup.biomixer.client.workbench.embed.EmbeddedViewLoader;
import org.thechiselgroup.biomixer.client.workbench.init.WindowLocation;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;

public class RootPathsLoader implements EmbeddedViewLoader {

    public static final String EMBED_MODE = "paths_to_root";

    @Inject
    private HierarchyPathServiceAsync hierarchyPathService;

    @Inject
    private TermServiceAsync termService;

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
                new ErrorHandlingAsyncCallback<Set<String>>(errorHandler) {

                    @Override
                    public void onFailure(Throwable caught) {
                        errorHandler.handleError(new Exception(
                                "Could not retrieve hierarchy to root for "
                                        + conceptId, caught));
                    }

                    @Override
                    protected void runOnSuccess(Set<String> shortIdsInHierarchy)
                            throws Exception {

                        for (String shortId : shortIdsInHierarchy) {
                            conceptNeighbourhoodService
                                    .getResourceWithRelations(
                                            virtualOntologyId,
                                            shortId,
                                            new ErrorHandlingAsyncCallback<Resource>(
                                                    errorHandler) {
                                                @Override
                                                public void onFailure(
                                                        Throwable caught) {
                                                    errorHandler
                                                            .handleError(new Exception(
                                                                    "Could not retrieve full term information for "
                                                                            + conceptId,
                                                                    caught));
                                                }

                                                @Override
                                                protected void runOnSuccess(
                                                        Resource resource) {
                                                    view.getResourceModel()
                                                            .getAutomaticResourceSet()
                                                            .add(resource);
                                                    layout(view);
                                                }
                                            });
                        }
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
            final String fullConceptId, final DefaultView view) {

        final String conceptUri = Concept.toConceptURI(virtualOntologyId,
                fullConceptId);
        if (view.getResourceModel().getResources().getByUri(conceptUri) != null) {
            return;
        }

        conceptNeighbourhoodService.getResourceWithRelations(virtualOntologyId,
                fullConceptId, new ErrorHandlingAsyncCallback<Resource>(
                        errorHandler) {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorHandler.handleError(new Exception(
                                "Could not retrieve full term information for "
                                        + fullConceptId, caught));
                    }

                    @Override
                    public void runOnSuccess(Resource resource) {
                        if (view.getResourceModel().getResources()
                                .getByUri(conceptUri) != null) {
                            return;
                        }

                        view.getResourceModel().getAutomaticResourceSet()
                                .add(resource);
                        layout(view);

                        for (String parentUri : resource
                                .getUriListValue(Concept.PARENT_CONCEPTS)) {

                            String parentFullConceptId = Concept
                                    .getConceptId(parentUri);
                            loadTerm(virtualOntologyId, parentFullConceptId,
                                    view);
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

                        String shortId = (String) result
                                .getValue(Concept.SHORT_ID);
                        loadHierarchyData(graphView, virtualOntologyId, shortId);
                    }

                });
    }

    private void loadUsingRecursiveTermService(final DefaultView view,
            final String virtualOntologyId, final String fullConceptId) {

        if (view.isReady()) {
            loadTerm(virtualOntologyId, fullConceptId, view);
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
