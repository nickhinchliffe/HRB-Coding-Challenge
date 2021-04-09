import java.util.*;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.*;

public class DeviceDriver {
	//Lists
	private static List<String> calledOperations = new ArrayList<>();
	private static List<Integer> pids = new ArrayList<>();
	
	//Connection information
	private static Socket socket = null;
	private static InputStream input = null;
	private static OutputStream output = null;
	private static PrintWriter writer = null;
	private static BufferedReader reader = null;
	static int port = 1000;
	
	//Booleans
	private static boolean init = false;
	private static boolean ready = false;

	
	private String abort() {
		//Checking to see if there is an existing connection before closing the connection
		String res = "";
		if(socket.isConnected()) {
			try {
				//Closing all i/o streams and socket.
				writer.close();
				reader.close();
				input.close();
				output.close();
				socket.close();
				
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			res = "There is no connection to the MockBot";
		}
		return res;
		
	}
	
	private String openConnection(String IPAddress) {
		String res = "";
		//Checking to see if there is an existing connection before closing the connection
		if(socket.isConnected()) {
			res = "Conection already exists";
		}
		else {
			try{
				System.out.println("IP: "+ IPAddress + " " + "Port: "+ port);
				
				//Create socket and input&output streams
				socket = new Socket(IPAddress, port);
				input = socket.getInputStream();
				output = socket.getOutputStream();
				
				//BufferReader will only read as a string
				reader = new BufferedReader(new InputStreamReader(input));
				writer = new PrintWriter(output, true);
				
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
		return res;
		
	}
	
	private String initialize() {
		//Check to see if MockBot has been initialized
		String res = "";
		if(init == false  && socket.isConnected()) {
			int initPid = -1;
			String homeCommand = "home";
			//Sending home command plus over.
			writer.println(homeCommand);
			System.out.println("initialized sent");
			
			//Get home pid
			while(initPid == -1) {
				try {
					initPid = Integer.parseInt(reader.readLine());
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			res = checkInitResults(initPid);
			
		}
		else {
			res = "MockBot has already been initialized or there is no connection to MockBot";
		}
		return res;
	}
	
	//Recursively checking if MockBot is initialized
	//Will update init boolean or abort with error
	private String checkInitResults(int initPid) {
		// check status of home
		String res = "";
		//Checking In progress first so we don't have to check something else first
		if(checkBot(initPid).equals("In Progress")) {
			//Should I wait for some time???
			checkInitResults(initPid);
		}
		else if(checkBot(initPid).equals("Terminated With Error")) {
			res = "MockBot has Terminated With Error! Bot has not been initialized";
			abort();
		}
		else {
			init = true;
		}
		return res;
	}
	
	//DeviceDriver implementation to call status
	private String checkBot(int pid) {
		String pidString = String.valueOf(pid);
		String checkCommand = "status%".concat(pidString);
		String res = "";
		writer.println(checkCommand);
		
		while(res == "") {
			try {
				//Update res for ExecuteOperation
				res = reader.readLine();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return res;
		
	}
	
	//Check to see if input to ExecuteOperation is valid
	private boolean isValid(String operation, String[] parameterNames, String[] parameterValues) {
		boolean output = false;
		if(operation.equals("Pick")) {
			if(parameterNames[0].equals("Source Location")) {
				try {
					Integer.parseInt(parameterValues[0]);
					output = true;
				}
				catch(NumberFormatException e){
					e.printStackTrace();
					output = false;
				}
			}
		}
		else if(operation.equals("Place")) {
			if(parameterNames[0].equals("Destination Location")) {
				try {
					Integer.parseInt(parameterValues[0]);
					output = true;
				}
				catch(NumberFormatException e){
					e.printStackTrace();
					output = false;
				}
			}
		}
		else if(operation.equals("Transfer")) {
			if(parameterNames[0].equals("Destination Location") && parameterNames[1].equals("Source Location") || parameterNames[0].equals("Source Location") && parameterNames[1].equals("Destination Location")) {
				try {
					Integer.parseInt(parameterValues[0]);
					Integer.parseInt(parameterValues[1]);
					output = true;
				}
				catch(NumberFormatException e){
					e.printStackTrace();
					output = false;
				}
			}
		}
		else {
			output = false;
		}
		return output;
		
	}
	
	
	private String executeOperation(String operation, String[] parameterNames, String[] parameterValues) {
		String command = "";
		int pid = -1;
		String lastOperation = "";
		String res = "";
		
		//Check to see if the user input is valid
		if(isValid(operation, parameterNames, parameterValues)) {
			//Checking to see if the MockBot is ready for more commands
			if(ready) {
				//Getting the last called operation
				if(calledOperations.size() == 0) {
					lastOperation = operation;
					calledOperations.add(operation);
				}else {
					lastOperation = calledOperations.get(calledOperations.size()-1);
				}
				
				//Once there is confirmation the last operation has finished
				//See if operation is possible, then run if it is.
				if(operation.equals("pick")) {
					if(lastOperation.equals("pick")) {
						res = "MockRobot is currently holding an item! Please drop the item and try again";
					}
					else {
						//Since pick can pick up from only 1 location, parameterNames & parameterValues will
						//have a size of 1
						command = operation.concat("%".concat(parameterNames[0].concat("%".concat(parameterValues[0]))));
						//Send command to MockBot
						ready = false;
						writer.println(command);
						
						//Wait for response from MockRobot
						while(pid == -1) {
							try {
								pid = Integer.parseInt(reader.readLine());
								
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						
						//Keep track of commands sent to bot
						calledOperations.add(operation);
						pids.add(pid);
						//check the status and update is ready
						res = checkMockBotStatus(pid);
					}
				}
				else if(operation.equals("place")) {
					if(lastOperation.equals("place")) {
						res = "MockRobot doesn't have an item to place! Please pick up an item and try again.";
					}
					else {
						//Since place can only put an item at  up from only 1 location, parameterNames & parameterValues will
						//have a size of 1
						command = operation.concat("%".concat(parameterNames[0].concat("%".concat(parameterValues[0]))));
						ready = false;
						writer.println(command);
					
						
						//Wait for response from MockRobot
						while(pid == -1) {
							try {
								pid = Integer.parseInt(reader.readLine());
								
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						//Keep track of commands sent to bot
						calledOperations.add(operation);
						pids.add(pid);
						//check the status and update is ready
						res = checkMockBotStatus(pid);
					}
				}
				else {
					//Transfer first picks up an item then places it in another location
					if(lastOperation.equals("pick")) {
						res = "MockRobot is currently holding an item!";
					}
					else {
						//Since place can only put an item at  up from only 1 location, parameterNames & parameterValues will
						//have a size of 1
						command = operation.concat("%".concat(parameterNames[0].concat("%".concat(parameterValues[0].concat("%".concat(parameterNames[1].concat("%").concat(parameterValues[1])))))));
						writer.println(command);
						
						
						//Wait for response from MockRobot
						while(pid == -1) {
							try {
								pid = Integer.parseInt(reader.readLine());
								
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						//Keep track of commands sent to bot
						calledOperations.add(operation);
						pids.add(pid);
						//check the status and update is ready
						res = checkMockBotStatus(pid);
					}
				}
			}
		}
		return res;
	}
		
	
	//Recursively checking if MockBot is initialized
		//Will update init boolean or abort with error
		private String checkMockBotStatus(int pid) {
			// check status of home
			String res = "";
			if(checkBot(pid).equals("Finished Successfully")) {
				ready = true;
			}
			else if(checkBot(pid).equals("Terminated With Error")) {
				res = "Terminated With Error";
				abort();
			}
			else {
				//Should I wait for some time???
				checkMockBotStatus(pid);
			}
			return res;
		}
	
	
	

	public static void main(String[] args) {
		//NOT SURE IF I NEED THIS MAIN
		
		//Let's say the UI communicates with DeviceDriver over console input
		Scanner scan = new Scanner(System.in);
		String input = "";
		
		//Instanciating DeviceDriver
		DeviceDriver driver = new DeviceDriver(1000);
		
		//Open Network connection to MockBot
		driver.openConnection("192.168.1.25");
		
		//Initialize MockBot
		driver.initialize();
		
		//Run user commands
		//Assuming user knows all commands
		//
		while(input != "Finished") {
			input = scan.nextLine();
			
		}
		
		
		
		driver.abort();
	}

}
