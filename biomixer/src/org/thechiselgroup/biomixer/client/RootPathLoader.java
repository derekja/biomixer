package org.thechiselgroup.biomixer.client;

import org.thechiselgroup.biomixer.client.core.error_handling.ErrorHandler;
import org.thechiselgroup.biomixer.client.core.error_handling.ErrorHandlingAsyncCallback;
import org.thechiselgroup.biomixer.client.core.resources.DefaultResourceSet;
import org.thechiselgroup.biomixer.client.core.resources.ResourceSet;
import org.thechiselgroup.biomixer.client.core.util.UriUtils;
import org.thechiselgroup.biomixer.client.core.visualization.DefaultView;
import org.thechiselgroup.biomixer.client.core.visualization.View;
import org.thechiselgroup.biomixer.client.dnd.windows.ViewWindowContent;
import org.thechiselgroup.biomixer.client.dnd.windows.WindowContentProducer;
import org.thechiselgroup.biomixer.client.services.ontology_version.OntologyVersionServiceAsync;
import org.thechiselgroup.biomixer.client.services.rootpath.RootPathServiceAsync;
import org.thechiselgroup.biomixer.client.visualization_component.graph.Graph;
import org.thechiselgroup.biomixer.client.visualization_component.graph.GraphLayoutSupport;
import org.thechiselgroup.biomixer.client.visualization_component.graph.ResourcePath;
import org.thechiselgroup.biomixer.client.visualization_component.graph.layouts.VerticalTreeLayout;
import org.thechiselgroup.biomixer.client.workbench.embed.EmbeddedViewLoader;
import org.thechiselgroup.biomixer.client.workbench.init.WindowLocation;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;

public class RootPathLoader implements EmbeddedViewLoader {

    public static final String EMBED_MODE = "path_to_root";

    @Inject
    private RootPathServiceAsync rootPathService;

    @Inject
    private OntologyVersionServiceAsync ontologyVersionService;

    @Inject
    private WindowContentProducer windowContentProducer;

    @Inject
    private ErrorHandler errorHandler;

    private void doLoadData(final DefaultView view,
            final String virtualOntologyId, final String ontologyVersionId,
            final String fullConceptId) {

        rootPathService.findPathToRoot(virtualOntologyId, ontologyVersionId,
                fullConceptId, new ErrorHandlingAsyncCallback<ResourcePath>(
                        errorHandler) {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorHandler.handleError(new Exception(
                                "Could not retrieve path to root for "
                                        + fullConceptId, caught));
                    }

                    @Override
                    protected void runOnSuccess(ResourcePath resourcePath)
                            throws Exception {
                        ResourceSet resourceSet = new DefaultResourceSet();
                        resourceSet.addAll(resourcePath
                                .getPathToRootResources());
                        view.getResourceModel().addResourceSet(resourceSet);
                        layout(view);
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

    private void loadData(final DefaultView view,
            final String virtualOntologyId, final String ontologyVersionId,
            final String fullConceptId) {

        if (view.isReady()) {
            doLoadData(view, virtualOntologyId, ontologyVersionId,
                    fullConceptId);
        } else {
            new Timer() {
                @Override
                public void run() {
                    loadData(view, virtualOntologyId, ontologyVersionId,
                            fullConceptId);
                }
            }.schedule(200);
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

        ontologyVersionService.getOntologyVersionId(virtualOntologyId,
                new ErrorHandlingAsyncCallback<String>(errorHandler) {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorHandler.handleError(new Exception(
                                "Could not retrieve ontology id for virtual ontology id "
                                        + virtualOntologyId, caught));

                    }

                    @Override
                    protected void runOnSuccess(String result) throws Exception {
                        loadData((DefaultView) graphView, virtualOntologyId,
                                result, fullConceptId);
                    }

                });
    }
}
