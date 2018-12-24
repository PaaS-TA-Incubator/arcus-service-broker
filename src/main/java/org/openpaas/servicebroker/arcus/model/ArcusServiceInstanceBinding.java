package org.openpaas.servicebroker.arcus.model;

import java.util.HashMap;
import java.util.Map;

import org.openpaas.servicebroker.model.ServiceInstanceBinding;

/**
 * A binding to a service instance
 * 
 * @author minkikim89@jam2in.com
 *
 */
public class ArcusServiceInstanceBinding extends ServiceInstanceBinding {
	
	private String clusterUserName;

	public ArcusServiceInstanceBinding(String id,
			String serviceInstanceId, 
			Map<String,Object> credentials,
			String syslogDrainUrl, String appGuid) {
		super(id, serviceInstanceId, credentials, syslogDrainUrl, appGuid);
	}

	public ArcusServiceInstanceBinding() {
		super("", "", new HashMap<String, Object>() , "", "");
	}
	
	public String getClusterUserName() {
		return clusterUserName;
	}

	public void setClusterUserName(String clusterUserName) {
		this.clusterUserName = clusterUserName;
	}
}
