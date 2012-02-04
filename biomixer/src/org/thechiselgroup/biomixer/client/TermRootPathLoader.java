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

import org.thechiselgroup.biomixer.client.core.error_handling.ErrorHandler;
import org.thechiselgroup.biomixer.client.core.error_handling.ErrorHandlingAsyncCallback;
import org.thechiselgroup.biomixer.client.core.resources.DefaultResourceSet;
import org.thechiselgroup.biomixer.client.core.resources.ResourceList;
import org.thechiselgroup.biomixer.client.core.resources.ResourceSet;
import org.thechiselgroup.biomixer.client.core.util.UriUtils;
import org.thechiselgroup.biomixer.client.core.visualization.DefaultView;
import org.thechiselgroup.biomixer.client.core.visualization.View;
import org.thechiselgroup.biomixer.client.dnd.windows.ViewWindowContent;
import org.thechiselgroup.biomixer.client.dnd.windows.WindowContentProducer;
import org.thechiselgroup.biomixer.client.services.rootpath.TermRootPathServiceAsync;
import org.thechiselgroup.biomixer.client.visualization_component.graph.Graph;
import org.thechiselgroup.biomixer.client.visualization_component.graph.GraphLayoutSupport;
import org.thechiselgroup.biomixer.client.visualization_component.graph.layouts.VerticalTreeLayout;
import org.thechiselgroup.biomixer.client.workbench.embed.EmbeddedViewLoader;
import org.thechiselgroup.biomixer.client.workbench.init.WindowLocation;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;

public class TermRootPathLoader implements EmbeddedViewLoader {

    // XXX change to just 'path_to_root' after integration with other services
    public static final String EMBED_MODE = "path_to_root_term";

    @Inject
    private ErrorHandler errorHandler;

    @Inject
    private WindowContentProducer windowContentProducer;

    @Inject
    private TermRootPathServiceAsync rootPathService;

    private void doLoadPathsToRoot(final DefaultView view,
            String virtualOntologyId, final String fullConceptId) {

        rootPathService.findPathToRoot(virtualOntologyId, fullConceptId,
                new ErrorHandlingAsyncCallback<ResourceList>(errorHandler) {

                    @Override
                    public void onFailure(Throwable caught) {
                        errorHandler.handleError(new Exception(
                                "Could not retrieve path to root for concept "
                                        + fullConceptId, caught));
                    }

                    @Override
                    public void runOnSuccess(ResourceList result) {
                        ResourceSet resourceSet = new DefaultResourceSet();
                        resourceSet.addAll(result);
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

    private void loadPathsToRoot(final DefaultView view,
            final String virtualOntologyId, final String fullConceptId) {
        if (view.isReady()) {
            doLoadPathsToRoot(view, virtualOntologyId, fullConceptId);
        } else {
            new Timer() {

                @Override
                public void run() {
                    loadPathsToRoot(view, virtualOntologyId, fullConceptId);

                }

            }.schedule(50);
        }
    }

    @Override
    public void loadView(WindowLocation windowLocation,
            AsyncCallback<View> callback) {

        View graphView = ((ViewWindowContent) windowContentProducer
                .createWindowContent(Graph.ID)).getView();
        graphView.init();
        callback.onSuccess(graphView);

        String fullConceptId = UriUtils.decodeURIComponent(windowLocation
                .getParameter("full_concept_id"));
        String virtualOntologyId = windowLocation
                .getParameter("virtual_ontology_id");

        loadPathsToRoot((DefaultView) graphView, virtualOntologyId,
                fullConceptId);
    }

}
