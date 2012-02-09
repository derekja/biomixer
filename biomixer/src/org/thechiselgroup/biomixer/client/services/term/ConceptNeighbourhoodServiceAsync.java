/*******************************************************************************
 * Copyright 2009, 2010, 2012 Lars Grammel, David Rusk 
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
package org.thechiselgroup.biomixer.client.services.term;

import org.thechiselgroup.biomixer.client.core.resources.Resource;
import org.thechiselgroup.biomixer.client.visualization_component.graph.ResourceNeighbourhood;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ConceptNeighbourhoodServiceAsync {

    /**
     * 
     * @param conceptId
     *            can be either a full concept id or a short concept id
     */
    void getNeighbourhood(String ontologyId, String conceptFullId,
            AsyncCallback<ResourceNeighbourhood> callback);

    /**
     * 
     * @param conceptId
     *            can be either a full concept id or a short concept id
     */
    void getResourceWithRelations(String ontologyId, String conceptId,
            AsyncCallback<Resource> callback);

}