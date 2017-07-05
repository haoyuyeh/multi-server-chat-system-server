/**
 * Author: Hao Yu Yeh
 * Date: 2016�~10��12��
 * Project: Assignment2 of Distributed System
 * Comment: this class is used to store the command line arguments
 */

//Remember to add the args4j jar to your project's build path 
import org.kohsuke.args4j.Option;

//This class is where the arguments read from the command line will be stored
//Declare one field for each argument and use the @Option annotation to link the field
//to the argument name, args4J will parse the arguments and based on the name,  
//it will automatically update the field with the parsed argument value
public class CmdLineArgs {

	@Option(required = true, name = "-n", aliases = {
			"--serverName" }, usage = "Hostname")
	private String host;

	@Option(required = true, name = "-l", aliases = {
			"--serverConfigPath" }, usage = "serverConfigurationPath")
	private String path;
	
	@Option(required = false, name = "-a", aliases = "--addNewServer", usage = "enable to add new chat server")
	private boolean newServer = false;
	
	public String getHost() {
		return host;
	}

	public String getServerConfigPath() {
		return path;
	}
	
	public boolean isNewServer(){
		return newServer;
	}

}
