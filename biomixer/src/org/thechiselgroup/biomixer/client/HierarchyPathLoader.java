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
import org.thechiselgroup.biomixer.client.services.rootpath.HierarchyPathServiceAsync;
import org.thechiselgroup.biomixer.client.services.term.TermServiceAsync;
import org.thechiselgroup.biomixer.client.visualization_component.graph.Graph;
import org.thechiselgroup.biomixer.client.visualization_component.graph.GraphLayoutSupport;
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
    private WindowContentProducer windowContentProducer;

    @Inject
    private ErrorHandler errorHandler;

    private void doLoadData(final DefaultView view,
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
                            view.getResourceModel().addResourceSet(resourceSet);
                            layout(view);
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

                        ResourceSet resourceSet = new DefaultResourceSet();
                        for (Resource resource : hierarchyResources) {
                            loadAdditonalTermInformation(resource,
                                    virtualOntologyId, resourceSet);
                        }

                        addResourcesToView(view, resourceSet,
                                hierarchyResources.size());
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

    private void loadAdditonalTermInformation(final Resource resource,
            final String virtualOntologyId, final ResourceSet resourceSet) {

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

                        // TODO must be a better way than having to reset the
                        // uri
                        resource.resetUri(Concept.toConceptURI(
                                virtualOntologyId, fullId));
                        resourceSet.add(resource);
                    }
                });
    }

    private void loadData(final DefaultView view,
            final String virtualOntologyId, final String conceptId) {

        if (view.isReady()) {
            doLoadData(view, virtualOntologyId, conceptId);
        } else {
            new Timer() {
                @Override
                public void run() {
                    loadData(view, virtualOntologyId, conceptId);
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

        // XXX full concept id or short??
        // need to convert to short??
        final String fullConceptId = UriUtils.decodeURIComponent(windowLocation
                .getParameter("full_concept_id"));
        final String virtualOntologyId = windowLocation
                .getParameter("virtual_ontology_id");

        loadData((DefaultView) graphView, virtualOntologyId, fullConceptId);

    }
}
