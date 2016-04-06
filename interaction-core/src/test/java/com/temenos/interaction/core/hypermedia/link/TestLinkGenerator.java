package com.temenos.interaction.core.hypermedia.link;

import com.temenos.interaction.core.MultivaluedMapImpl;
import com.temenos.interaction.core.command.InteractionContext;
import com.temenos.interaction.core.hypermedia.*;
import com.temenos.interaction.core.web.RequestContext;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.mockito.Matchers.anyObject;


/**
 * Created by ikarady on 06/04/2016.
 */
public class TestLinkGenerator {

    //@Test
    public void testCreateLinkFromDynamicResource() {
        //ResourceStateMachine engine = new ResourceStateMachine(mock(ResourceState.class), new BeanTransformer());
        RequestContext.setRequestContext(new RequestContext("test", null, null));
        ResourceStateMachine engineMock = Mockito.mock(ResourceStateMachine.class);
        ResourceStateAndParameters resourceStateAndParameters = new ResourceStateAndParameters();
        resourceStateAndParameters.setState(mockTarget("/testDynamic"));
        resourceStateAndParameters.setParams(new ParameterAndValue[] {new ParameterAndValue(null, null)});
        Mockito.when(engineMock.resolveDynamicState(
                Mockito.mock(DynamicResourceState.class),
                Mockito.mock(Map.class),
                Mockito.mock(InteractionContext.class)))
                .thenReturn(resourceStateAndParameters);
        Map<String,String> uriParameters = new HashMap<String,String>();
        uriParameters.put("filter", "{nodeId}");
        Transition t = new Transition.Builder()
                .source(mock(ResourceState.class))
                .target(mockTarget("/test"))
                .uriParameters(uriParameters)
                .build();
        MultivaluedMap<String,String> queryParameters = new MultivaluedMapImpl<String>();
        queryParameters.add("nodeId", "123");
        LinkGenerator linkGenerator = new LinkGenerator(engineMock, t, null);
        linkGenerator.setAllQueryParameters(true);
        Collection<Link> links = linkGenerator.createLink(new MultivaluedMapImpl<String>(), queryParameters, null);
        Link result = (!links.isEmpty()) ? links.iterator().next() : null;
        assertEquals("/baseuri/test?$filter=123", result.getHref());
    }

    private ResourceState mockTarget(String path) {
        ResourceState target = mock(ResourceState.class);
        when(target.getPath()).thenReturn(path);
        return target;
    }

}
