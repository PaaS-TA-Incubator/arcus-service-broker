package org.openpaas.servicebroker.arcus.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.ObjectReader;
import net.spy.memcached.AdminConnectTimeoutException;
import org.apache.zookeeper.Transaction;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.CreateMode;
import org.openpaas.servicebroker.arcus.ZkConnection;
import org.openpaas.servicebroker.arcus.exception.ArcusServiceException;
import org.openpaas.servicebroker.arcus.model.ArcusServiceInstance;
import org.openpaas.servicebroker.arcus.model.ArcusServiceInstanceBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.openpaas.servicebroker.util.JSchUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;

@Service
public class ArcusAdminService {

	private Logger logger = LoggerFactory.getLogger(ArcusAdminService.class);

	private static final String ARCUS_BASE_CACHE_LIST_PATH = "/arcus/cache_list/";

	private static final String ARCUS_BASE_CACHE_SERVER_MAPPING_PATH = "/arcus/cache_server_mapping/";

	private static final String ARCUS_BASE_CLIENT_INFO_ZPATH = "/arcus/client_list/";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private String zkPort;

	/* ssh connections */
	@Autowired
	private ArrayList<JSchUtil> jschList;

	private ArrayList<ZkConnection> zkConnList;

	private Map<String, Object> plans;

	private int availableUserSize;

	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	public ArcusAdminService(ArrayList<JSchUtil> jschList, JdbcTemplate jdbcTemplate, String zkPort) {
		this.plans = new HashMap<String, Object>();
		Map<String, String> plan = new HashMap<String, String>();

		//Simple Plan
		plan.put("memory", "100");
		plan.put("connection", "100");
		this.plans.put("arcus-100mb-plan", plan);

		// now, max user : 30
		availableUserSize = 30;

		this.jschList = jschList;
		this.jdbcTemplate = jdbcTemplate;
		this.zkPort = zkPort;

		this.zkConnList = new ArrayList<ZkConnection>();

		List<String> hosts = new ArrayList<String>();

		for (JSchUtil jschUtil : jschList) {
			String host = jschUtil.getHostname();
			hosts.add(host);
		}

		try {
			// now topology - ZK 1 : Memcached 1
			for (String host : hosts) {
				saveVmPortInfo(host);
				String hostPort = host + ":" + zkPort;

				ZkConnection conn = new ZkConnection(hostPort);

				zkConnList.add(conn);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean isExistsService(ArcusServiceInstance instance) throws ArcusServiceException {
		try {
			List<Map<String,Object>> databases = jdbcTemplate.queryForList("SELECT * FROM service_instances WHERE service_code = '"+instance.getServiceCode()+"'");

			return databases.size() > 0 ? true : false;

		} catch (InvalidResultSetAccessException e) {
			throw handleException(e);
		} catch (DataAccessException e) {
			throw handleException(e);
		}
	}
	
	public ArcusServiceInstance findSvcById(String id) throws ArcusServiceException {
		try {
			ArcusServiceInstance serviceInstance = null;

			List<Map<String,Object>> findByIdList = jdbcTemplate.queryForList("SELECT * FROM service_instances WHERE guid = '"+id+"'");

			if (findByIdList.size() > 0) {

				serviceInstance = new ArcusServiceInstance();
				Map<String,Object> findById = findByIdList.get(0);

				String serviceInstanceId = (String)findById.get("guid");
				String planId = (String)findById.get("plan_id");
				String zookeeperHost = (String)findById.get("zk_host");
				String serviceCode = (String)findById.get("service_code");

				if ( !"".equals(serviceInstanceId) && serviceInstanceId !=null) serviceInstance.setServiceInstanceId(serviceInstanceId);
				if ( !"".equals(planId) && planId !=null) serviceInstance.setPlanId(planId);
				if ( !"".equals(zookeeperHost) && zookeeperHost !=null) serviceInstance.setZookeeperHost(zookeeperHost);
				if ( !"".equals(serviceCode) && serviceCode !=null) serviceInstance.setServiceCode(serviceCode);
			}

			return serviceInstance;
		} catch (InvalidResultSetAccessException e) {
			throw handleException(e);
		} catch (DataAccessException e) {
			throw handleException(e);
		}
	}
	
	public ArcusServiceInstanceBinding findBindById(String id) throws ArcusServiceException {
		try {
			ArcusServiceInstanceBinding serviceInstanceBinding = null;

			List<Map<String,Object>> findByIdList = jdbcTemplate.queryForList("SELECT * FROM service_instance_bindings WHERE guid = '"+id+"'");

			if ( findByIdList.size() > 0) {
				serviceInstanceBinding = new ArcusServiceInstanceBinding();
				Map<String,Object> findById = findByIdList.get(0);

				String serviceInstanceBindingId = (String)findById.get("guid");
				String serviceInstanceId = (String)findById.get("service_instance_id");
				String clusterUserName = (String)findById.get("cluster_user_name");

				if ( !"".equals(serviceInstanceBindingId) && serviceInstanceBindingId !=null) serviceInstanceBinding.setId(serviceInstanceBindingId);
				if ( !"".equals(serviceInstanceId) && serviceInstanceId !=null) serviceInstanceBinding.setServiceInstanceId(serviceInstanceId);
				if ( !"".equals(clusterUserName) && clusterUserName !=null) serviceInstanceBinding.setClusterUserName(clusterUserName);

			}

			return serviceInstanceBinding;

		} catch (InvalidResultSetAccessException e) {
			throw handleException(e);
		} catch (DataAccessException e) {
			throw handleException(e);
		}
	}
	
	public void delete(String id) throws ArcusServiceException {
		try{
			try {
				jdbcTemplate.update("DELETE FROM service_instances WHERE guid = ?", id);
				jdbcTemplate.update("DELETE FROM service_instance_bindings WHERE service_instance_id = ?", id);
			} catch (InvalidResultSetAccessException e) {
				throw handleException(e);
			} catch (DataAccessException e) {
				throw handleException(e);
			}
		} catch (Exception e) {
			throw handleException(e);
		}
	}
	
	public void deleteBind(String id) throws ArcusServiceException {
		try {
			jdbcTemplate.update("DELETE FROM service_instance_bindings WHERE guid = ?", id);
		} catch (InvalidResultSetAccessException e) {
			throw handleException(e);
		} catch (DataAccessException e) {
			throw handleException(e);
		}
	}
	
	public void save(ArcusServiceInstance serviceInstance) throws ArcusServiceException {
		try{
			try {
				jdbcTemplate.update("INSERT INTO service_instances (guid, plan_id, zk_host, service_code) values (?, ?, ?, ?)",
						serviceInstance.getServiceInstanceId(), serviceInstance.getPlanId(), serviceInstance.getZookeeperHost(), serviceInstance.getServiceCode());
			} catch (InvalidResultSetAccessException e) {
				throw handleException(e);
			} catch (DataAccessException e) {
				throw handleException(e);
			}
		} catch (Exception e) {
			throw handleException(e);
		}
	}
	
	public void saveBind(ArcusServiceInstanceBinding serviceInstanceBinding) throws ArcusServiceException {
		try {
			jdbcTemplate.update("INSERT INTO service_instance_bindings (guid, service_instance_id, cluster_user_name) values (?, ?, ?)",
					serviceInstanceBinding.getId(), serviceInstanceBinding.getServiceInstanceId(), serviceInstanceBinding.getClusterUserName());
		} catch (InvalidResultSetAccessException e) {
			throw handleException(e);
		} catch (DataAccessException e) {
			throw handleException(e);
		}
	}

	public void saveVmPortInfo(String address) throws ArcusServiceException {
		try {
			int startPort = 11211;
			int endPort = startPort + availableUserSize;
			for (int port = startPort; port <= endPort; port++) {
				jdbcTemplate.update("INSERT INTO usable_port_info (port_num, vm_ip, is_used) VALUES (?, ?, ?)", port, address, false);
			}

		} catch (InvalidResultSetAccessException e) {
//			throw handleException(e);
		} catch (DataAccessException e) {
//			throw handleException(e);
		}

	}

	public int getAvailablePort(String address) throws ArcusServiceException {
		Connection conn = DataSourceUtils.getConnection(jdbcTemplate.getDataSource());
		try {
			int portNum = 0;
			conn.setAutoCommit(false);

			List<Map<String,Object>> findByIdList = jdbcTemplate.queryForList("SELECT * FROM usable_port_info WHERE vm_ip=? and is_used=?",
					address, false);

			if (findByIdList.size() > 0) {
				Map<String,Object> available_info = findByIdList.get(0);

				portNum = (Integer)available_info.get("port_num");

				jdbcTemplate.update("UPDATE usable_port_info SET is_used = true WHERE port_num = ?", portNum);
			}
			conn.commit();
			conn.setAutoCommit(true);

			if (portNum == 0) {
				throw new ArcusServiceException("Can't create more instance. It already over the limit");
			}
			return portNum;

		} catch (InvalidResultSetAccessException e) {
			throw handleException(e);
		} catch (DataAccessException e) {
			throw handleException(e);
		} catch (SQLException e) {
			try {
				conn.rollback();
				throw handleException(e);
			} catch (SQLException e1) {
				throw handleException(e1);
			}
		}
	}

	public void updateAvailablePort(String port, boolean is_used) throws ArcusServiceException {
		try {
			jdbcTemplate.update("UPDATE usable_port_info SET is_used=? WHERE port_num=?", is_used, port);
		} catch (InvalidResultSetAccessException e) {
			throw handleException(e);
		} catch (DataAccessException e) {
			throw handleException(e);
		}
	}
	
	public boolean createService(ArcusServiceInstance serviceInstance) throws ArcusServiceException {
		// memcached node select
		Random random = new Random();
		int nodeId = random.nextInt(jschList.size());

		String planId = serviceInstance.getPlanId();
		String arcusPath = "$ARCUS_PATH";

		String serviceCode = serviceInstance.getServiceCode();

		Map<String, String> plan = (Map<String, String>) plans.get(planId);

		if (plan == null) {
			throw new ArcusServiceException("no plan");
		}

		String memlimit = plan.get("memory");
		String connection = plan.get("connection");

		// ZooKeeper client code
		ZooKeeper zk = zkConnList.get(nodeId).getZk();
		JSchUtil jschUtil = jschList.get(nodeId);
		String nodeIp = jschUtil.getHostname();

		/* zkhost setting */
		serviceInstance.setZookeeperHost(nodeIp + ":" +zkPort);

		List<String> commands = new ArrayList<>();

//		// Free memory command
//		String totalMemCmd = "free  | grep ^Mem | tr -s ' ' | cut -d ' ' -f 2'";
//		String usedMemCmd = "free  | grep ^Mem | tr -s ' ' | cut -d ' ' -f 3";
//		String freeMemCmd ="free  | grep ^Mem | tr -s ' ' | cut -d ' ' -f 4";
//		commands.add(totalMemCmd);
//		commands.add(usedMemCmd);
//		commands.add(freeMemCmd);
//		Map<String, List<String>> memrs = jschUtil.shell(commands);
//
//		String resTotal = memrs.get(totalMemCmd).get(0);
//		String resUsed = memrs.get(usedMemCmd).get(0);
//		String resFree = memrs.get(freeMemCmd).get(0);
//
//		if (resTotal != null && resUsed != null && resFree != null) {
//			System.out.println(resTotal);
//			System.out.println(resUsed);
//			System.out.println(resFree);
//			long totalMem = Integer.valueOf(resTotal);
//			long used = Integer.valueOf(resUsed);
//			long free = Integer.valueOf(resFree);
//
//			/* The percentage of free memory is always upper than 10% of full memory or plan memory*2 */
//			if (used / totalMem * 100 > 90 || free < Integer.valueOf(plan.get("memory")) * 1000 * 2) {
//				throw new ArcusServiceException("The memory capacity is insufficient.");
//			}
//		}



		try {
			int arcusPort = -1;
			String zookeeperHost = null;
			int tryCnt = 0;

			while (tryCnt < availableUserSize) {
				arcusPort = getAvailablePort(nodeIp);
				zookeeperHost = nodeIp + ":" + zkPort;

				if (zk.exists(ARCUS_BASE_CACHE_LIST_PATH + serviceCode, false) == null) {
					break;
				}

				updateAvailablePort(String.valueOf(arcusPort), true);
				tryCnt++;
			}

			// 0. Create a transaction
			Transaction tx = zk.transaction();

			// 1. Cache list
			List<String> deleteList = getMappingForService(zk, serviceCode);


			if (zk.exists(ARCUS_BASE_CACHE_LIST_PATH + serviceCode, false) == null) {
				// Json
				// ObjectReader reader = mapper.reader();

				Map hashData = new HashMap();

				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Calendar cal = dateFormat.getCalendar();

				hashData.put("created", cal.getTime());
				// HashData have to keep server's config

				// use Jackson library
				byte[] data = mapper.writeValueAsBytes(hashData);


				zk.create(ARCUS_BASE_CACHE_LIST_PATH + serviceCode, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			} else {
				// Exception
			}

			// 2. Client list
			if (zk.exists(ARCUS_BASE_CLIENT_INFO_ZPATH + serviceCode, false) == null) {
				tx.create(ARCUS_BASE_CLIENT_INFO_ZPATH + serviceCode, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			} else {
				// pass
			}

			// 3. Mapping
			for (String each : deleteList) {
				tx.delete(each + "/" + serviceCode, 0);
				tx.delete(each, 0);
			}

			String mapIp = ARCUS_BASE_CACHE_SERVER_MAPPING_PATH + nodeIp + ":" +arcusPort;
			String mapCode = mapIp + "/" + serviceCode;

			byte[] config = "".getBytes();

			if (zk.exists(mapIp, false) == null) {
				tx.create(mapIp, config, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}

			if (zk.exists(mapCode, false) == null) {
				tx.create(mapCode, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}

			// 4. Commit
			tx.commit();


			// memcached start
			commands.add(arcusPath + "/bin/memcached"
					+ " -E " + arcusPath + "/lib/default_engine.so"
					+ " -X " + arcusPath + "/lib/syslog_logger.so"
					+ " -X " + arcusPath + "/lib/ascii_scrub.so"
					+ " -d -v -r -R5 -U 0 -D: -b 8192 -m" + memlimit
					+ " -c " + connection + " -t 6"
					+ " -p " + arcusPort
					+ " -z " + zookeeperHost);

			Map<String, List<String>> rs = jschUtil.shell(commands);

			if (!"0".equals(rs.get("exitStatus").get(0))) {
				throw new ArcusServiceException("Memcached Node connection error. "+ arcusPort + " port is already used..");
			}
		} catch (InterruptedException e) {
			throw handleException(e);
		} catch (KeeperException e) {
			throw handleException(e);
		} catch (JsonProcessingException e) {
			throw handleException(e);
		}
		return true;
	}

	public boolean deleteService(ArcusServiceInstance serviceInstance) throws ArcusServiceException {
		try{
			List<String> commands = new ArrayList<>();
			String zookeeperHost = serviceInstance.getZookeeperHost();
			String serviceCode = serviceInstance.getServiceCode();

			String nodeIp = zookeeperHost.split(":")[0];
			int nodeIdx = -1;

			for (int i = 0; i < jschList.size(); i++) {
				if(nodeIp.equals(jschList.get(i).getHostname())) {
					nodeIdx = i;
					break;
				}
			}

			if (nodeIdx == -1) {
				throw new ArcusServiceException("Service does not exist");
			}

			ZooKeeper zk = zkConnList.get(nodeIdx).getZk();

			List<String> deleteMappingList = getMappingForService(zk,  serviceCode);
			List<String> deleteCacheList = getCacheListForService(zk, serviceCode);
			List<String> deleteClientList = getClientListForService(zk, serviceCode);

			List<String> deletePortList = new ArrayList<String>();

			for (String each : deleteMappingList) {
				String[] zpathSp = each.split("/");
				String ipport = zpathSp[zpathSp.length-1];

				String[] ipportSp = ipport.split(":");
				String port = ipportSp[ipportSp.length-1];

				// for port
				commands.add("kill $(ps -ef | grep -e memcached " +
						"| grep -e '-p "+ port + "' | grep -v 'ssh' | awk '{print $2}')");
				deletePortList.add(port);
			}

			//return ==> map key: command, val: result
			Map<String, List<String>> rs = jschList.get(nodeIdx).shell(commands);

			for (String port : deletePortList) {
				updateAvailablePort(port, false);
			}

			if ("0".equals(rs.get("exitStatus").get(0)) == false)
				throw new ArcusServiceException("Can't shutdown arcus-memcached");

			Thread.sleep(1000);

			// 0. Create transaction
			Transaction tx = zk.transaction();

			// 1. Delete children (mapping is both of parent, child
			for (String each : deleteMappingList) {
				if (zk.exists(each + "/" + serviceCode, false) != null) {
					tx.delete(each + "/" + serviceCode, 0);
					tx.delete(each, 0);
				}
			}

			for (String each : deleteCacheList) {
				if (zk.exists(each, false) != null)
					tx.delete(each, 0);
			}

			for (String each : deleteClientList) {
				if (zk.exists(each, false) != null)
					tx.delete(each, 0);
			}

			// 2. Delete parent
			tx.delete(ARCUS_BASE_CACHE_LIST_PATH + serviceCode, 0);
			tx.delete(ARCUS_BASE_CLIENT_INFO_ZPATH + serviceCode, 0);

			// 4. Commit
			tx.commit();

			return true;
		} catch (Exception e) {

			throw handleException(e);
		}
	}

	public List<String> getMappingForService(ZooKeeper zk, String serviceCode) throws ArcusServiceException{
		List<String> list = new ArrayList<String>();
		String mapping = "/arcus/cache_server_mapping";

		try {
			List<String> all = zk.getChildren(mapping, false);
			for (String ipport : all) {
				List<String> codes = zk.getChildren(mapping + "/" + ipport, false);
				if (codes.size() > 0 && serviceCode.equals(codes.get(0))) {
					list.add(mapping + "/" + ipport);
				}
			}
		} catch (KeeperException e) {
			throw handleException(e);
		} catch (InterruptedException e) {
			throw handleException(e);
		}

		return list;
	}

	public List<String> getCacheListForService(ZooKeeper zk, String serviceCode) throws ArcusServiceException{
		List<String> list = new ArrayList<String>();
		String cacheList = "/arcus/cache_list";

		try {
			List<String> all = zk.getChildren(cacheList + "/" + serviceCode, false);

			for (String data : all) {
				list.add(cacheList + "/" + serviceCode + "/" + data);
			}

		} catch (KeeperException e) {
			throw handleException(e);
		} catch (InterruptedException e) {
			throw handleException(e);
		}

		return list;
	}

	public List<String> getClientListForService(ZooKeeper zk, String serviceCode) throws ArcusServiceException{
		List<String> list = new ArrayList<String>();
		String clientList = "/arcus/client_list";

		try {
			List<String> all = zk.getChildren(clientList + "/" + serviceCode, false);
			for (String data : all) {
				list.add(clientList + "/" + serviceCode + "/" + data);
			}
		} catch (KeeperException e) {
			throw handleException(e);
		} catch (InterruptedException e) {
			throw handleException(e);
		}

		return list;
	}
	
	private ArcusServiceException handleException(Exception e) {
		logger.warn(e.getLocalizedMessage(), e);
		return new ArcusServiceException(e.getLocalizedMessage());
	}


}
