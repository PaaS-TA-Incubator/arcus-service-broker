package org.openpaas.servicebroker.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * ssh접속을 위한 Util 클래스.
 * 
 * @author Cho Mingu
 *
 */
public class JSchUtil {
	private String hostname;
	private String username;
	private String identity=null;
	private String password=null;
	private String sudoPassword=null;
	private boolean isDebugMode=false;

	public void enableDebug(){
		isDebugMode=true;
	}

	public void disableDebug(){
		isDebugMode=false;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}

	public String getIdentity() {
		return identity;
	}

	public void setIdentity(String identity) {
		this.identity = identity;
		this.password =null;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
		this.identity=null;
	}

	public String getSudoPassword() {
		return sudoPassword;
	}

	public void setSudoPassword(String sudoPassword) {
		this.sudoPassword = sudoPassword;
	}

	public JSchUtil(){ }

	public JSchUtil(String username,String hostname){
		this.username = username;
		this.hostname = hostname;
	}

	private Session getSession() throws JSchException{
		JSch jsch=new JSch();
		if (identity!=null) {
			jsch.addIdentity(identity);
			//jsch.setKnownHosts(new ByteArrayInputStream(hostname.getBytes()));
		}

		Session session=jsch.getSession(username, hostname);
		session.setConfig("StrictHostKeyChecking", "no");
		if (password!=null)	session.setPassword(password);
		return session;
	}

	public Map<String, List<String>> shell(String command){
		List<String> commands = new ArrayList<String>();
		commands.add(command);

		return shell(commands);
	}/*
	public void exec(List<String> commands){
		exec(commands.toArray(new String[]{}));
		return ;
	}*/


	public Map<String, List<String>> shell(List<String> commands) {

		int exitStatus = 1;

		commands.add("exit");
		
		StringBuilder sb = new StringBuilder();

		try{
			Session session = getSession();
			session.connect();
			Channel channel = session.openChannel("shell");//only shell
			//channel.setPtyType("vt102");

			// create the IO streams to send input to remote session.
			PipedOutputStream commandIO = new PipedOutputStream();
			InputStream sessionInput = new PipedInputStream(commandIO);
			// this set's the InputStream the remote server will read from.
			channel.setInputStream(sessionInput);

			//PrintStream shellStream = new PrintStream(channel.getOutputStream());  // printStream for convenience 
			// this will have the STDOUT from server.
			InputStream sessionOutput = channel.getInputStream();

			// this will have the STDERR from server
			InputStream sessionError = channel.getExtInputStream();

			channel.connect();

			byte[] tmp = new byte[1024];
			String stdOut = "";
			String stdErr = "";

			int i;

			if (this.sudoPassword != null) {
				commands.add("exit");
				
				commandIO.write(("su -\n").getBytes());
				commandIO.flush();
	
	
				while (true) {
					if (sessionError.available() > 0) {
						i = sessionError.read(tmp, 0, tmp.length);
						if (i < 0) {
							System.err.println("input stream closed earlier than expected");
							System.exit(1);
						}
						stdErr += new String(tmp, 0, i);
						if (isDebugMode) System.out.print(stdErr);
					}
	
					if (sessionOutput.available() > 0) {
						i = sessionOutput.read(tmp, 0, tmp.length);
						if (i < 0) {
							System.err.println("input stream closed earlier than expected");
							System.exit(1);
						}
						stdOut += new String(tmp, 0, i);
						if (isDebugMode) System.out.print(stdOut);
					}
	
					if (stdOut.contains("assword")) {
						break;
					}
	
					Thread.sleep(1000);
				}
	
				commandIO.write((this.sudoPassword+"\n").getBytes());
				commandIO.flush();

			}
			
			for(String command: commands) {

				commandIO.write((command+"\n").getBytes()); 
				commandIO.flush();
			}


			while (true) {

				while (sessionOutput.available() > 0) {
					i = sessionOutput.read(tmp);
					if (i < 0)
						break;
					sb.append(new String(tmp, 0, i));
					stdOut = new String(tmp, 0, i);
					if (isDebugMode) System.out.print(stdOut);
				}

				if (channel.isClosed()) {
					if (sessionOutput.available() > 0)
						continue;
					if (isDebugMode) System.out.println("exit-status: " + channel.getExitStatus());

					exitStatus = channel.getExitStatus();
					break;
				}

				try {Thread.sleep(1000);}
				catch (Exception ee) { }
			}
			sessionOutput.close();

			channel.disconnect();
			session.disconnect();
		}catch(Exception e){
			e.printStackTrace();
		}

		return getResults(commands, sb.toString().split("\n"), exitStatus);
	}

	public List<String> exec(String command) {
		List<String> ret = new ArrayList<String>();
		try{
			Session session = getSession();
			session.connect();

			ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
			channelExec.setPty(true);
			if (isDebugMode) System.out.println("command : "+command);
			channelExec.setCommand(command);
			InputStream inputStream = channelExec.getInputStream();
			//InputStream ext = channelExec.getExtInputStream();
			InputStream err = channelExec.getErrStream();
			channelExec.connect(3000);

			if (isDebugMode) System.out.print("stdout : ");
			String output="";
			byte[] buf = new byte[1024];
			int length;
			while ((length=inputStream.read(buf))!=-1){
				output+=new String(buf,0,length);
				if (isDebugMode) System.out.print(new String(buf,0,length));
			}
			if (isDebugMode) System.out.println("\nerr : "+err.toString());
			ret.add(output);
			channelExec.disconnect();

			session.disconnect();
		}catch(Exception e){
			e.printStackTrace();
		}

		return ret;

	}

	public Map<String, List<String>> getResults(List<String> commands, String[] results, int exitStatus) {
		Map<String, List<String>> rsMap = new HashMap<String, List<String>>();
		List<String> rsList = null;

		int i = 0;
		int j = 0;
		boolean commandStart = false;
		while (i < results.length ) {
			if (!commandStart) {
				if(results[i].contains(commands.get(commands.size()-1))) {
					commandStart = true;
				}
			} else if (commandStart) {
				if ( results[i].contains(commands.get(j))) {
					if (j != 0) {
						rsMap.put(commands.get(j-1), rsList);
					}

					if (j >= commands.size()-1) break;

					j++;
					rsList = new ArrayList<String>();					
				} else if ( rsList != null) {
					rsList.add(results[i]);
				}
			}

			i++;
		}

		rsMap.put("exitStatus", Arrays.asList(""+exitStatus));

		return rsMap;
	}


}