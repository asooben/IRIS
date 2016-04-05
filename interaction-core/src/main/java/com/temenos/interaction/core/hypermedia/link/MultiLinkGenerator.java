package com.temenos.interaction.core.hypermedia.link;

import com.temenos.interaction.core.command.InteractionContext;
import com.temenos.interaction.core.hypermedia.HypermediaTemplateHelper;
import com.temenos.interaction.core.hypermedia.Link;
import com.temenos.interaction.core.hypermedia.ResourceStateMachine;
import com.temenos.interaction.core.hypermedia.Transition;

import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;


/**
 * Created by ikarady on 05/04/2016.
 */
public class MultiLinkGenerator extends LinkGenerator {

    private String collectionName;

    public MultiLinkGenerator(ResourceStateMachine resourceStateMachine,
            Transition transition, Object entity, String collectionName) {
        super(resourceStateMachine, transition, entity);
        this.collectionName = collectionName;
    }

    @Override
    public Collection<Link> createLink(Map<String, Object> transitionProperties,
            MultivaluedMap<String, String> queryParameters, InteractionContext ctx) {
        Collection<Link> eLinks = new ArrayList<Link>();
        Map<String, Object> normalizedProperties = HypermediaTemplateHelper.normalizeProperties(transitionProperties);

        Iterator<Map.Entry<String,Object>> entryItr = normalizedProperties.entrySet().iterator();
        while(entryItr.hasNext())
        {
            Map.Entry<String,Object> entry = entryItr.next();
            String entryKey = entry.getKey();
            if(collectionName.equals(entryKey.replaceAll("[()0-9]", "")))
            {
                Link link = createLink(transitionProperties, queryParameters, ctx, entryKey); //No query parameter

                if (link != null) {
                    eLinks.add(link);
                }
            }
        }

        return eLinks;
    }


}
