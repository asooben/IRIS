package com.temenos.interaction.core.hypermedia.link;

import com.temenos.interaction.core.MultivaluedMapImpl;
import com.temenos.interaction.core.hypermedia.*;
import com.temenos.interaction.core.web.RequestContext;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Created by ikarady on 06/04/2016.
 */
public class TestLinkGenerator {

    @Test
    public void testCreateLinkFromDynamicResource() {
        RequestContext.setRequestContext(new RequestContext("/test", null, null));
        ResourceStateMachine engineMock = Mockito.mock(ResourceStateMachine.class);
        ResourceStateAndParameters resourceStateAndParameters = new ResourceStateAndParameters();
        resourceStateAndParameters.setState(mockTarget("/testDynamic"));
        resourceStateAndParameters.setParams(new ParameterAndValue[] {new ParameterAndValue("filter2", "564")});
        DynamicResourceState dynamicResourceStateMock = mockDynamicTarget(null);
        Map<String,String> uriParameters = new HashMap<String,String>();
        uriParameters.put("filter", "{nodeId}");
        Transition t = new Transition.Builder()
                .source(mock(ResourceState.class))
                .target(dynamicResourceStateMock)
                .uriParameters(uriParameters)
                .build();
        MultivaluedMap<String,String> queryParameters = new MultivaluedMapImpl<String>();
        queryParameters.add("nodeId", "123");
        Map<String, Object> transitionProperties = new HashMap<String, Object>();
        transitionProperties.put("filter", "{nodeId}");
        transitionProperties.put("nodeId", "123");
        Mockito.when(engineMock.resolveDynamicState(
                dynamicResourceStateMock,
                transitionProperties,
                null))
                .thenReturn(resourceStateAndParameters);
        Mockito.when(engineMock.getTransitionProperties(t, null, new MultivaluedMapImpl<String>(), queryParameters)).thenReturn(transitionProperties);

        LinkGenerator linkGenerator = new LinkGenerator(engineMock, t, null);
        linkGenerator.setAllQueryParameters(true);
        Collection<Link> links = linkGenerator.createLink(new MultivaluedMapImpl<String>(), queryParameters, null);
        Link result = (!links.isEmpty()) ? links.iterator().next() : null;
        assertEquals("/test/testDynamic?filter=123&filter2=564", result.getHref());
    }

    private ResourceState mockTarget(String path) {
        ResourceState target = mock(ResourceState.class);
        when(target.getPath()).thenReturn(path);
        when(target.getRel()).thenReturn("collection");
        return target;
    }

    private DynamicResourceState mockDynamicTarget(String path) {
        DynamicResourceState target = mock(DynamicResourceState.class);
        when(target.getPath()).thenReturn(path);
        return target;
    }

}
