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
package org.thechiselgroup.biomixer.client.services.rootpath;

import java.util.List;

import org.thechiselgroup.biomixer.client.core.resources.Resource;
import org.thechiselgroup.biomixer.client.core.util.transform.Transformer;
import org.thechiselgroup.biomixer.client.core.util.url.UrlBuilder;
import org.thechiselgroup.biomixer.client.core.util.url.UrlBuilderFactory;
import org.thechiselgroup.biomixer.client.core.util.url.UrlFetchService;
import org.thechiselgroup.biomixer.client.services.AbstractXMLWebResourceService;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;

public class HierarchyPathServiceAsyncClientImplementation extends
        AbstractXMLWebResourceService implements HierarchyPathServiceAsync {

    private final HierarchyParser parser;

    @Inject
    public HierarchyPathServiceAsyncClientImplementation(
            UrlFetchService urlFetchService,
            UrlBuilderFactory urlBuilderFactory, HierarchyParser parser) {
        super(urlFetchService, urlBuilderFactory);

        this.parser = parser;
    }

    private String buildUrl(String conceptId, String virtualOntologyId) {
        UrlBuilder urlBuilder = urlBuilderFactory.createUrlBuilder().path(
                "/bioportal/virtual/rootpath/" + virtualOntologyId + "/"
                        + conceptId);
        return urlBuilder.toString();
    }

    @Override
    public void findHierarchyToRoot(final String virtualOntologyId,
            final String conceptId, AsyncCallback<List<Resource>> callback) {

        String url = buildUrl(conceptId, virtualOntologyId);
        fetchUrl(callback, url, new Transformer<String, List<Resource>>() {
            @Override
            public List<Resource> transform(String xmlText) throws Exception {
                return parser.parse(conceptId, xmlText, virtualOntologyId);
            }

        });

    }

}
