package org.thechiselgroup.biomixer.client.services.rootpath;

import org.thechiselgroup.biomixer.client.core.resources.ResourceList;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface TermRootPathServiceAsync {

    void findPathToRoot(String virtualOntologyId, String fullConceptId,
            AsyncCallback<ResourceList> callback);

}
