package org.thechiselgroup.biomixer.client.services.rootpath;

import java.util.List;

import org.thechiselgroup.biomixer.client.core.resources.Resource;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface HierarchyPathServiceAsync {

    void findHierarchyToRoot(String virtualOntologyId, String conceptId,
            AsyncCallback<List<Resource>> callback);

}
