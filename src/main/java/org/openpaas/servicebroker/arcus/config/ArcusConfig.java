package org.openpaas.servicebroker.arcus.config;

import javax.sql.DataSource;

import org.openpaas.servicebroker.util.JSchUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
//import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.ArrayList;
import java.util.List;

@Configuration
@PropertySource("classpath:datasource.properties")
public class ArcusConfig {

	@Autowired
	private Environment env;

	@Bean
	public String zkPort() {
		String zkPort = env.getRequiredProperty("arcus.zookeeper_port");
		return zkPort;
	}

	@Bean
	public ArrayList<JSchUtil> jschList() {
		ArrayList<JSchUtil> hosts = new ArrayList<JSchUtil>();

		String serverUser = env.getRequiredProperty("arcus.server.userName");
		String[] serverHosts = env.getRequiredProperty("arcus.server.hosts").split(",");

		String serverIdentity = env.getRequiredProperty("arcus.server.identity");
		String serverPassword = env.getRequiredProperty("arcus.server.password");
		String serverSudoPassword = env.getRequiredProperty("arcus.server.sudo.password");

		int serverCount = serverHosts.length;
		for(int i = 0; i < serverCount; i++) {
			JSchUtil jsch = new JSchUtil(serverUser, serverHosts[i]);
			if( !"".equals(serverSudoPassword) && serverSudoPassword != null) jsch.setSudoPassword(serverSudoPassword);
			if( !"".equals(serverIdentity) && serverIdentity != null) jsch.setIdentity(serverIdentity);
			else jsch.setPassword(serverPassword);
			jsch.enableDebug();
			hosts.add(jsch);
		}

		return hosts;
	}

	@Bean
	public DataSource dataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(env.getRequiredProperty("jdbc.driver"));
		dataSource.setUrl(env.getRequiredProperty("db.url"));
		dataSource.setUsername(env.getRequiredProperty("db.username"));
		dataSource.setPassword(env.getRequiredProperty("db.password"));

		return dataSource;
	}


}
