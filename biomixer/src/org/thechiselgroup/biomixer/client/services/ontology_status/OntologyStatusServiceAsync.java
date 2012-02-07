package org.thechiselgroup.biomixer.client.services.ontology_status;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface OntologyStatusServiceAsync {

    void getOntologyStatuses(AsyncCallback<Map<String, List<String>>> callback);

}
