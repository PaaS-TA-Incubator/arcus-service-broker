package org.openpaas.servicebroker.arcus.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.openpaas.servicebroker.model.Catalog;
import org.openpaas.servicebroker.model.ServiceDefinition;
import org.openpaas.servicebroker.service.CatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * 
 * @author minkikim89@jam2in.com
 *
 */
@Service
public class ArcusCatalogService implements CatalogService {

	private Catalog catalog;
	private Map<String,ServiceDefinition> serviceDefs = new HashMap<String,ServiceDefinition>();

	@Autowired
	public ArcusCatalogService(Catalog catalog) {
		this.catalog = catalog;
		initializeMap();
	}
	
	private void initializeMap() {
		for (ServiceDefinition def: catalog.getServiceDefinitions()) {
			serviceDefs.put(def.getId(), def);
		}
	}
	
	@Override
	public Catalog getCatalog() {
		return catalog;
	}

	@Override
	public ServiceDefinition getServiceDefinition(String serviceId) {
		return serviceDefs.get(serviceId);
	}

}
