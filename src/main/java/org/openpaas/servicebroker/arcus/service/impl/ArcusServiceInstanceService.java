package org.openpaas.servicebroker.arcus.service.impl;


import org.openpaas.servicebroker.exception.ServiceBrokerException;
import org.openpaas.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.openpaas.servicebroker.exception.ServiceInstanceExistsException;
import org.openpaas.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.openpaas.servicebroker.model.CreateServiceInstanceRequest;
import org.openpaas.servicebroker.model.DeleteServiceInstanceRequest;
import org.openpaas.servicebroker.model.ServiceInstance;
import org.openpaas.servicebroker.model.UpdateServiceInstanceRequest;
import org.openpaas.servicebroker.arcus.exception.ArcusServiceException;
import org.openpaas.servicebroker.service.ServiceInstanceService;
import org.openpaas.servicebroker.arcus.model.ArcusServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Arcus impl to manage service instances.
 *  
 * @author minkikim89@jam2in.com
 *
 */
@Service
public class ArcusServiceInstanceService implements ServiceInstanceService {

	private static final Logger logger = LoggerFactory.getLogger(ArcusServiceInstanceService.class);

	@Autowired
	private ArcusAdminService arcusAdminService;

	@Autowired
	public ArcusServiceInstanceService(ArcusAdminService arcusAdminService) {
		this.arcusAdminService = arcusAdminService;
	}
	
	@Override
	public ArcusServiceInstance createServiceInstance(CreateServiceInstanceRequest request)
			throws ServiceInstanceExistsException, ServiceBrokerException {
		logger.debug("ArcusServiceInstanceService CLASS createServiceInstance");

		ArcusServiceInstance instance = arcusAdminService.findSvcById(request.getServiceInstanceId());
		
		if (instance != null) {
			String as_is_id = instance.getServiceInstanceId();
			String as_is_plan = instance.getPlanId();
			String to_be_id = request.getServiceInstanceId();
			String to_be_plan = request.getPlanId();

			logger.debug("as-is : Instance ID = {}, Plan = {}", as_is_id, as_is_plan);
			logger.debug("to-be : Instance ID = {}, Plan = {}", to_be_id, to_be_plan);

			// if instance_id is equal, just return that instance
			if (as_is_id.equals(to_be_id) && as_is_plan.equals(to_be_plan)) {
				instance.setHttpStatusOK();
				return instance;
			}

			throw new ServiceInstanceExistsException(instance);
		}

		instance = new ArcusServiceInstance();
		instance.setPlanId(request.getPlanId());
		instance.setServiceInstanceId(request.getServiceInstanceId());
		// service code is randomly generated
		do {
			instance.setServiceCode(getServiceCodeName());
		} while (arcusAdminService.isExistsService(instance));

		arcusAdminService.createService(instance);

		arcusAdminService.save(instance);
		return instance;
	}

	@Override
	public ServiceInstance getServiceInstance(String id) {
		ServiceInstance instance = null;
		try {
			instance = arcusAdminService.findSvcById(id);
		} catch (ArcusServiceException e) {
			e.printStackTrace();
		}
		return instance;
	}

	@Override
	public ArcusServiceInstance deleteServiceInstance(DeleteServiceInstanceRequest request) throws ArcusServiceException {
		ArcusServiceInstance instance = arcusAdminService.findSvcById(request.getServiceInstanceId());
		arcusAdminService.deleteService(instance);
		arcusAdminService.delete(instance.getServiceInstanceId());
		return instance;		
	}

	@Override
	public ArcusServiceInstance updateServiceInstance(UpdateServiceInstanceRequest request)
			throws ServiceInstanceUpdateNotSupportedException, ServiceBrokerException, ServiceInstanceDoesNotExistException {
		ArcusServiceInstance instance = arcusAdminService.findSvcById(request.getServiceInstanceId());
//		arcusAdminService.delete(instance.getServiceInstanceId());
//		ArcusServiceInstance updatedInstance = new ArcusServiceInstance(request);
//		arcusAdminService.save(updatedInstance);
		return instance;
	}

	// TODO service code validation check
	private String getServiceCodeName() {
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("MD5");
			digest.update(UUID.randomUUID().toString().getBytes());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		//[^a-zA-Z0-9]
		return new BigInteger(1, digest.digest()).toString(16).replaceAll("/[^a-zA-Z0-9]+/", "").substring(0, 16);

	}

}