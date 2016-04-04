package com.temenos.interaction.core.hypermedia;

/*
 * #%L
 * interaction-core
 * %%
 * Copyright (C) 2012 - 2014 Temenos Holdings N.V.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */


import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.odata4j.core.OCollection;
import org.odata4j.core.OCollections;
import org.odata4j.core.OComplexObject;
import org.odata4j.core.OComplexObjects;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmComplexType;

public class TestHypermediaTemplateHelper {

	@Test
	public void testGetBaseUri() {
		assertEquals("http://localhost:8080/responder/rest/", 
				HypermediaTemplateHelper.getTemplatedBaseUri("http://localhost:8080/responder/rest", "http://localhost:8080/responder/rest/"));
	}

	@Test
	public void testGetBaseUriSimple() {
		assertEquals("http://localhost:8080/responder/rest/", 
				HypermediaTemplateHelper.getTemplatedBaseUri("http://localhost:8080/responder/rest", "http://localhost:8080/responder/rest/test"));
	}

	@Test
	public void testGetTemplatedBaseUri() {
		assertEquals("http://localhost:8080/responder/rest/MockCompany001/", 
				HypermediaTemplateHelper.getTemplatedBaseUri("http://localhost:8080/responder/rest/{companyid}", "http://localhost:8080/responder/rest/MockCompany001/"));
	}

	@Test
	public void testGetTemplatedBaseUriSimple() {
		assertEquals("http://localhost:8080/responder/rest/MockCompany001/", 
				HypermediaTemplateHelper.getTemplatedBaseUri("http://localhost:8080/responder/rest/{companyid}", "http://localhost:8080/responder/rest/MockCompany001/test"));
	}

	@Test
	public void testGetTemplatedBaseUriNested() {
		assertEquals("http://localhost:8080/responder/rest/MockCompany001/", 
				HypermediaTemplateHelper.getTemplatedBaseUri("http://localhost:8080/responder/rest/{companyid}", "http://localhost:8080/responder/rest/MockCompany001/test/blah"));
	}

	@Test
	public void testGetTemplatedBaseUriQueryParameters() {
		assertEquals("http://localhost:8080/responder/rest/MockCompany001/", 
				HypermediaTemplateHelper.getTemplatedBaseUri("http://localhost:8080/responder/rest/{companyid}", "http://localhost:8080/responder/rest/MockCompany001/test?blah=123"));
	}

	@Test
	public void testGetTemplatedBaseUriLookBehind() {
		assertEquals("http://127.0.0.1:9081/hothouse-iris/Hothouse.svc/GB0010001/", 
				HypermediaTemplateHelper.getTemplatedBaseUri("http://127.0.0.1:9081/hothouse-iris/Hothouse.svc/{companyid}/", "http://127.0.0.1:9081/hothouse-iris/Hothouse.svc/GB0010001/root"));
	}
	
	@Test
	public void testGetTemplatedBaseUriLookBehind_FundsTransferNew() {
		assertEquals("http://portal3.xlb2.lo/tapt24-iris/tapt24.svc/LU0010001/", 
				HypermediaTemplateHelper.getTemplatedBaseUri("http://portal3.xlb2.lo/tapt24-iris/tapt24.svc/{companyid}/", "http://portal3.xlb2.lo/tapt24-iris/tapt24.svc/LU0010001/FundsTransfer_FtTaps()/new"));
	}
	
	@Test
	public void testGetTemplatedBaseUriLookBehind_FundsTransferValidate() {
		assertEquals("http://portal3.xlb2.lo/tapt24-iris/tapt24.svc/LU0010001/", 
				HypermediaTemplateHelper.getTemplatedBaseUri("http://portal3.xlb2.lo/tapt24-iris/tapt24.svc/{companyid}/", "http://portal3.xlb2.lo/tapt24-iris/tapt24.svc/LU0010001/FundsTransfer_FtTaps('FT1336500058')/validate"));
	}
	
	@Test
	public void testGetTemplatedBaseUriLookBehind_FundsTransferFtTaps() {
		assertEquals("http://portal3.xlb2.lo/tapt24-iris/tapt24.svc/LU0010001/", 
				HypermediaTemplateHelper.getTemplatedBaseUri("http://portal3.xlb2.lo/tapt24-iris/tapt24.svc/{companyid}/", "http://portal3.xlb2.lo/tapt24-iris/tapt24.svc/LU0010001/FundsTransfer_FtTaps()"));
	}
	
	@Test
	public void testGetTemplatedBaseUriLookBehind_FundsTransferFtTapsNewHol() {
		assertEquals("http://portal3.xlb2.lo/tapt24-iris/tapt24.svc/LU0010001/", 
				HypermediaTemplateHelper.getTemplatedBaseUri("http://portal3.xlb2.lo/tapt24-iris/tapt24.svc/{companyid}/", "http://portal3.xlb2.lo/tapt24-iris/tapt24.svc/LU0010001/FundsTransfer_FtTaps()/new/hold?id=FT1336500058"));
	}
	
	@Test
	public void testGetTemplatedBaseUriLastCharacter() {
		assertEquals("http://localhost:8080/example/interaction-odata-multicompany.svc/MockCompany001/", 
				HypermediaTemplateHelper.getTemplatedBaseUri("http://localhost:8080/example/interaction-odata-multicompany.svc/{companyid}/", "http://localhost:8080/example/interaction-odata-multicompany.svc/MockCompany001"));
	}
		
	@Test
	public void testTemplateReplace() {
		Map<String,Object> properties = new HashMap<String,Object>();
		properties.put("companyid", "GB0010001");
		assertEquals("http://127.0.0.1:9081/hothouse-iris/Hothouse.svc/GB0010001/",
				HypermediaTemplateHelper.templateReplace("http://127.0.0.1:9081/hothouse-iris/Hothouse.svc/{companyid}/", properties));
	}

	@Test
	public void testSpecialCharacterTemplateReplace() {
		Map<String,Object> properties = new HashMap<String,Object>();
		properties.put("companyid", "GB0010001");
		assertEquals("http://127.0.0.1:9081/hothouse-iris/Hothouse.svc/GB0010001/$metadata",
				HypermediaTemplateHelper.templateReplace("http://127.0.0.1:9081/hothouse-iris/Hothouse.svc/{companyid}/$metadata", properties));
	}

	@Test
	public void testPartialTemplateReplace() {
		Map<String,Object> properties = new HashMap<String,Object>();
		properties.put("companyid", "GB0010001");
		assertEquals("http://127.0.0.1:9081/hothouse-iris/Hothouse.svc/GB0010001/flights/{id}",
				HypermediaTemplateHelper.templateReplace("http://127.0.0.1:9081/hothouse-iris/Hothouse.svc/{companyid}/flights/{id}", properties));
	}
	
	@Test
	public void testOneLevelCollectionTemplateReplace()
	{
	    OCollection<?> contactColl = OCollections.newBuilder(null).add(createComplexObject("Email","johnEmailAddr","Tel","12345")).add(createComplexObject("Email","smithEmailAddr","Tel","66778")).build();                
        Map<String,Object> properties = new LinkedHashMap<String,Object>();
        properties.put("source_Contact", contactColl);
        
        assertEquals("http://127.0.0.1:9081/hothouse-iris/Hothouse.svc/contact/johnEmailAddr",
                HypermediaTemplateHelper.templateReplace("http://127.0.0.1:9081/hothouse-iris/Hothouse.svc/contact/{Contact(0).Email}", properties));
        
        assertEquals("http://127.0.0.1:9081/hothouse-iris/Hothouse.svc/contact/12345",
                HypermediaTemplateHelper.templateReplace("http://127.0.0.1:9081/hothouse-iris/Hothouse.svc/contact/{Contact(0).Tel}", properties));
        
        assertEquals("http://127.0.0.1:9081/hothouse-iris/Hothouse.svc/contact/smithEmailAddr",
                HypermediaTemplateHelper.templateReplace("http://127.0.0.1:9081/hothouse-iris/Hothouse.svc/contact/{Contact(1).Email}", properties));
        
        assertEquals("http://127.0.0.1:9081/hothouse-iris/Hothouse.svc/contact/66778",
                HypermediaTemplateHelper.templateReplace("http://127.0.0.1:9081/hothouse-iris/Hothouse.svc/contact/{Contact(1).Tel}", properties));  
	}
	
	@Test
	public void testTwoLevelCollectionTemplateReplace()
	{
	    OCollection<?> pcColl = OCollections.newBuilder(null).add(createComplexObject("PostCode", "ABCD")).add(createComplexObject("PostCode", "EFGH")).build();  
        
        OProperty<?> contactCollectionProp =  OProperties.collection("Address", null, pcColl);
        List<OProperty<?>> contactPropList = new ArrayList<OProperty<?>>();
        contactPropList.add(contactCollectionProp);
        OComplexObject contactDetails = OComplexObjects.create(EdmComplexType.newBuilder().build(), contactPropList);
        OCollection<?> contactColl = OCollections.newBuilder(null).add(contactDetails).build();  
        
        Map<String,Object> properties = new LinkedHashMap<String,Object>();
        properties.put("source_Contact", contactColl);        
        
        assertEquals("http://127.0.0.1:9081/hothouse-iris/Hothouse.svc/contact/ABCD",
                HypermediaTemplateHelper.templateReplace("http://127.0.0.1:9081/hothouse-iris/Hothouse.svc/contact/{Contact(0).Address(0).PostCode}", properties));
        
        assertEquals("http://127.0.0.1:9081/hothouse-iris/Hothouse.svc/contact/EFGH",
                HypermediaTemplateHelper.templateReplace("http://127.0.0.1:9081/hothouse-iris/Hothouse.svc/contact/{Contact(0).Address(1).PostCode}", properties));
        
	}
	
	private OComplexObject createComplexObject(String... values)
    {
        List<OProperty<?>> propertyList = new ArrayList<OProperty<?>>();        
        for(int i=0; i<values.length; i+=2)
        {
            OProperty<String> property = OProperties.string(values[i], values[i+1]);
            propertyList.add(property);
        }        
        OComplexObject complexObj = OComplexObjects.create(EdmComplexType.newBuilder().build(),propertyList);        
        return complexObj;
    }
}
