package org.openpaas.servicebroker.arcus.service.impl;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.openpaas.servicebroker.arcus.exception.ArcusServiceException;
import org.openpaas.servicebroker.exception.ServiceBrokerException;
import org.openpaas.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.openpaas.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.openpaas.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.openpaas.servicebroker.service.ServiceInstanceBindingService;
import org.openpaas.servicebroker.arcus.model.ArcusServiceInstance;
import org.openpaas.servicebroker.arcus.model.ArcusServiceInstanceBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author minkikim89@jam2in.com
 */
@Service
public class ArcusServiceInstanceBindingService implements ServiceInstanceBindingService {

	private static final Logger logger = LoggerFactory.getLogger(ArcusServiceInstanceBindingService.class);

	@Autowired
	private ArcusAdminService arcusAdminService;
	
	
	@Autowired
	public ArcusServiceInstanceBindingService(ArcusAdminService arcusAdminService) {
		this.arcusAdminService = arcusAdminService;
	}
	
	@Override
	public ArcusServiceInstanceBinding createServiceInstanceBinding(
			CreateServiceInstanceBindingRequest request)
			throws ServiceInstanceBindingExistsException, ServiceBrokerException {
		logger.debug("ArcusServiceInstanceBindingService CLASS createServiceInstanceBinding");

		ArcusServiceInstanceBinding binding = arcusAdminService.findBindById(request.getBindingId());
		if (binding != null) {
			throw new ServiceInstanceBindingExistsException(binding);
		}

		ArcusServiceInstance instance = arcusAdminService.findSvcById(request.getServiceInstanceId());

		String uri = instance.getZookeeperHost();

		String serviceCode = instance.getServiceCode();

		// TODO authentication.. we don't use it now. just save.
		String username = getUsername();
		String password = "";
		
		Map<String,Object> credentials = new HashMap<String,Object>();
		credentials.put("uri", uri);
		credentials.put("username", username);
		credentials.put("password", password);
		credentials.put("name", serviceCode);

		binding = new ArcusServiceInstanceBinding(request.getBindingId(), instance.getServiceInstanceId(), credentials, null, request.getAppGuid());
		binding.setClusterUserName(username);

		arcusAdminService.saveBind(binding);
		
		return binding;
	}

	protected ArcusServiceInstanceBinding getServiceInstanceBinding(String id) throws ArcusServiceException {
		return arcusAdminService.findBindById(id);
	}

	@Override
	public ArcusServiceInstanceBinding deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request)
			throws ServiceBrokerException {
		String bindingId = request.getBindingId();
		ArcusServiceInstanceBinding binding = getServiceInstanceBinding(bindingId);

		logger.debug("binding : {}", (binding == null ? "Not exist": "Exist") );

		if (binding!= null) {
			arcusAdminService.deleteBind(bindingId);
		}
		return binding;
	}

	private String getUsername() {
		String uuid16 = null;
		MessageDigest digest = null;
		try {
			do {
				digest = MessageDigest.getInstance("MD5");
				digest.update(UUID.randomUUID().toString().getBytes());
				uuid16 = new BigInteger(1, digest.digest()).toString(16).replaceAll("/[^a-zA-Z]+/", "").substring(0, 16);
			} while(!uuid16.matches("^[a-zA-Z][a-zA-Z0-9]+"));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return uuid16;
	}

	private String getPassword() {
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("MD5");
			digest.update(UUID.randomUUID().toString().getBytes());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return new BigInteger(1, digest.digest()).toString(16).replaceAll("/[^a-zA-Z]+/", "").substring(0, 16);
	}

}