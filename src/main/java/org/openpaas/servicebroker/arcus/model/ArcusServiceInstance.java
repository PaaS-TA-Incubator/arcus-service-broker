package org.openpaas.servicebroker.arcus.model;

import org.openpaas.servicebroker.model.CreateServiceInstanceRequest;
import org.openpaas.servicebroker.model.DeleteServiceInstanceRequest;
import org.openpaas.servicebroker.model.ServiceInstance;
import org.openpaas.servicebroker.model.UpdateServiceInstanceRequest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * An instance of a ServiceDefinition.
 * 
 * @author minkikim89@jam2in.com
 *
 */
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class ArcusServiceInstance extends ServiceInstance {

	private String zookeeperHost;
	private String serviceCode;

	public ArcusServiceInstance(CreateServiceInstanceRequest request) {
		super(request);
	}

	public ArcusServiceInstance(DeleteServiceInstanceRequest request) {
		super(request);
	}

	public ArcusServiceInstance(UpdateServiceInstanceRequest request) {
		super(request);
	}
	
	public ArcusServiceInstance() {
		super(new CreateServiceInstanceRequest());
	}

	public String getZookeeperHost() { return zookeeperHost; }

	public void setZookeeperHost(String zookeeperHost) { this.zookeeperHost = zookeeperHost; }

	public String getServiceCode() { return serviceCode; }

	public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
}
