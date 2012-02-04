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
package org.thechiselgroup.biomixer.client.core.resources;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ResourceList implements Iterable<Resource> {

    private List<Resource> resources;

    public ResourceList() {
        this.resources = new ArrayList<Resource>();
    }

    public void add(Resource resource) {
        resources.add(resource);
    }

    /**
     * 
     * @param otherResource
     *            The resource to check for
     * @return True if the ResourceList contains a resource with the same URI as
     *         the specified resource, not taking into account any differences
     *         in other attributes
     */
    public boolean containsSameURI(Resource otherResource) {
        String uri = otherResource.getUri();
        for (Resource resource : resources) {
            if (resource.getUri().equals(uri)) {
                return true;
            }
        }
        return false;
    }

    public Resource getResourceWithSameURI(Resource otherResource) {
        String uri = otherResource.getUri();
        for (Resource resource : resources) {
            if (resource.getUri().equals(uri)) {
                return resource;
            }
        }
        return null;
    }

    @Override
    public Iterator<Resource> iterator() {
        return resources.iterator();
    }

}
