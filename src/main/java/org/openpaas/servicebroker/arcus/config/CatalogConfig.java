package org.openpaas.servicebroker.arcus.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openpaas.servicebroker.model.Catalog;
import org.openpaas.servicebroker.model.Plan;
import org.openpaas.servicebroker.model.ServiceDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogConfig {
	
	@Bean
	public Catalog catalog() {
		return new Catalog( Arrays.asList(
				new ServiceDefinition(
					"arcus",
					"ARCUS",
					"ARCUS is a memcached-based cache cloud with list, set, map, b+tree collections.",
					true, 
					false,
					Arrays.asList(
							new Plan("arcus-100mb-plan",
									"arcus-100mb-plan",
									"This is an ARCUS free plan. 100Mb memory, 100 connections",
									getPlanMetadata(), true)),
					Arrays.asList("arcus", "document"),
					getServiceDefinitionMetadata(),
					null,
					null)));
	}

	private Map<String,Object> getServiceDefinitionMetadata() {
		Map<String,Object> sdMetadata = new HashMap<String,Object>();
		sdMetadata.put("displayName", "ARCUS");
		sdMetadata.put("imageUrl","http://www.jam2in.com/img/services-arcus.png");
		sdMetadata.put("longDescription","ARCUS Service");
		sdMetadata.put("providerDisplayName","JaM2in");
		sdMetadata.put("documentationUrl","https://github.com/naver/arcus");
		sdMetadata.put("supportUrl","http://www.jam2in.com");
		return sdMetadata;
	}
	
	private Map<String,Object> getPlanMetadata() {		
		Map<String,Object> planMetadata = new HashMap<String,Object>();
		planMetadata.put("cost", getCosts());
		planMetadata.put("bullets", getBullets());
		return planMetadata;
	}
	
	private List<Map<String,Object>> getCosts() {
		Map<String,Object> costsMap = new HashMap<String,Object>();
		
		Map<String,Object> amount = new HashMap<String,Object>();
		amount.put("usd", new Double(0.0));
	
		costsMap.put("amount", amount);
		costsMap.put("unit", "MONTHLY");
		
		return Arrays.asList(costsMap);
	}
	
	private List<String> getBullets() {
		return Arrays.asList("ARCUS cluster",
				"100 MB memory",
				"100 connections");
	}
	
}