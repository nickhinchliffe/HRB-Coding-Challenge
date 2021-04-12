import java.util.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.*;

public class DeviceDriver {
	
	//Connection information
	private Socket socket = null;
	private InputStream input = null;
	private OutputStream output = null;
	private PrintWriter writer = null;
	private BufferedReader reader = null;
	private int port = 1000;
	
	//Lists
	private List<String> calledOperations = new ArrayList<>();
	
	//Booleans
	private boolean init = false;
	private boolean ready = false;
	
	//Required Interface method
	//When a user presses the “Abort” button, the UI calls this function and expects that the Device Driver will terminate communication with the MockRobot. 
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
				
			} catch (Exception e) {
				res = "Could not close i/o streams and or socket";
			}
		}
		else {
			res = "There is no connection to the MockBot";
		}
		return res;
		
	}
	
	//Required Interface method
	//When a user presses the “Open Connection” button, the UI calls this function
	//and expects the Device Driver to establish a connection with the MockRobot onboard software.
	private String openConnection(String IPAddress) {
		String res = "";
		//Checking to see if there is an existing connection before opening one
		if(socket.isConnected()) {
			res = "Conection to MockBot already exists";
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
			catch(Exception e) {
				res = "Could not open i/o streams and or socket";
			}
		}
		return res;
		
	}
	
	//Required Interface method.
	//When a user presses the “Initialize” button, the UI calls this function 
	//and expects that the Device Driver will put the MockRobot into an automation-ready (homed) state.
	private String initialize() {
		//Check to see if MockBot has been initialized and a connection exists.
		String res = "";
		if(init == false  && socket.isConnected()) {
			int initPid = -1;
			
			//Sending home command to MockBot.
			writer.println("home");
			
			//Get home pid.
			while(initPid == -1) {
				try {
					initPid = Integer.parseInt(reader.readLine());
					
				} catch (Exception e) {
					res = "Could not parse Integer";
				}
			}
			//Check to see if initialize() has completed
			res = checkInitResults(initPid, res);
			
		}
		else {
			res = "MockBot has already been initialized or there is no connection to MockBot";
		}
		return res;
	}
	
	//Recursively checking if MockBot is initialized
	//Will update init boolean or abort with error
	private String checkInitResults(int initPid, String res) {
		if(checkBot(initPid).equals("In Progress")) {
			checkInitResults(initPid, res);
		}
		else if(checkBot(initPid).equals("Terminated With Error")) {
			res = "Terminated With Error";
			abort();
		}
		else if(checkBot(initPid).equals("Could not read from MockBot")) {
			res = "Could not read from MockBot";
		}
		else {
			init = true;
			ready = true;
		}
		return res;
	}
	
	//DeviceDriver implementation to call status on MockBot
	private String checkBot(int pid) {
		String pidString = String.valueOf(pid);
		String checkCommand = "status%".concat(pidString);
		String res = "";
		//Sending checkCommand to MockBot
		writer.println(checkCommand);
		
		while(res == "") {
			try {
				//Update res for ExecuteOperation
				res = reader.readLine();
				
			} catch (Exception e) {
				res = "Could not read from MockBot";
			}
		}
		
		return res;
	}
	
	//Check to see if input to ExecuteOperation is valid
	private boolean isValid(String operation, String[] parameterNames, String[] parameterValues) {
		boolean output = false;
		//Checks operation, then parameterNames, then parameterValues
		if(operation.equals("Pick")) {
			if(parameterNames[0].equals("Source Location")) {
				try {
					Integer.parseInt(parameterValues[0]);
					output = true;
				}
				catch(Exception e){
					System.out.println("The parameter value can't be changed into an integer");
					output = false;
				}
			}
			else {
				System.out.println("Pick needs to have Source Location as it's parameterNames[0]");
			}
		}
		else if(operation.equals("Place")) {
			if(parameterNames[0].equals("Destination Location")) {
				try {
					Integer.parseInt(parameterValues[0]);
					output = true;
				}
				catch(Exception e){
					System.out.println("The parameter value can't be changed into an integer");
					output = false;
				}
			}
			else {
				System.out.println("Place needs to have Destination Location as it's parameterNames[0]");
			}
		}
		else if(operation.equals("Transfer")) {
			if(parameterNames[0].equals("Destination Location") && parameterNames[1].equals("Source Location") || parameterNames[0].equals("Source Location") && parameterNames[1].equals("Destination Location")) {
				try {
					Integer.parseInt(parameterValues[0]);
					Integer.parseInt(parameterValues[1]);
					output = true;
				}
				catch(Exception e){
					System.out.println("The parameter value(s) can't be changed into an integer");
					output = false;
				}
			}
		}
		else {
			output = false;
		}
		return output;
	}
	
	private String pick(String location, String res, int pid, String lastOperation) {
		if(lastOperation.equals("pick")) {
			res = "MockRobot is currently holding an item! Please place the item and try again";
		}
		else {
			//Since pick can pick up from only 1 location, parameterNames & parameterValues will
			//have a size of 1
			String command = "pick%Source Location%".concat(location);
			ready = false;
			//Sending pick command to MockRobot.
			writer.println(command);
		
			//Wait for response from MockBot.
			while(pid == -1) {
				try {
					pid = Integer.parseInt(reader.readLine());
				
				} catch (Exception e) {
					res = "The parameter value can't be changed into an integer";
				}
			}
			//Since this block will run after catch, we need to make sure there is a need to run this.
			if(res == "") {
				//check the status and update is ready
				res = checkMockBotStatus(pid, res);
				//Keep track of commands sent to bot
				calledOperations.add("pick");
			}
		}
		return res;
	}
	
	private String place(String location, String res, int pid, String lastOperation) {
		if(lastOperation.equals("place")) {
			res = "MockRobot doesn't have an item to place! Please pick up an item and try again.";
		}else {
			//Since place can only put an item at  up from only 1 location, parameterNames & parameterValues will
			//have a size of 1.
			String command = "place%Destination Location%".concat(location);
			ready = false;
			//Sending place command to MockBot.
			writer.println(command);
		
			
			//Wait for response from MockRobot
			while(pid == -1) {
				try {
					pid = Integer.parseInt(reader.readLine());
					
				} catch (Exception e) {
					res = "The parameter value can't be changed into an integer";
				}
			}
			//Since this block will run after catch, we need to make sure there is a need to run this.
			if(res == "") {
				//check the status and update is ready
				res = checkMockBotStatus(pid, res);
				//Keep track of commands sent to bot
				calledOperations.add("place");
			}
		}
		return res;
	}
	
	private String transfer(String[] parameterNames, String[] parameterValues, String res, int pid, String lastOperation) {
		//Check to see what the parameterName is to determine what operation to complete first
		if(parameterNames[0].equals("Destination Location")) {
			res = place(parameterValues[0], res, pid, lastOperation);
			if(res == "") {
				res = pick(parameterValues[1], res, pid, lastOperation);
			}
		}
		else {
			res = pick(parameterValues[0], res, pid, lastOperation);
			if(res == "") {
				res = place(parameterValues[1], res, pid, lastOperation);
			}
		}
		return res;
	}
	
	
	//When a user presses the “Execute Operation” button, the UI calls this function and 
	//expects that the Device Driver will perform an operation determined by the parameter operation.
	private String executeOperation(String operation, String[] parameterNames, String[] parameterValues) {
		int pid = -1;
		String lastOperation = operation;
		String res = "";
		
		//Check to see if the user input is valid
		if(isValid(operation, parameterNames, parameterValues)) {
			//Checking to see if the MockBot is ready for more commands
			if(ready) {
				//Getting the last called operation
				if(calledOperations.size() != 0) {
					lastOperation = calledOperations.get(calledOperations.size()-1);
				}
				//Once Bot is ready and operation is valid
				//See if operation is possible, then run if it is.
				if(operation.equals("pick")) {
					//Call pick method
					res = pick(parameterValues[0], res, pid, lastOperation);
				}
				else if(operation.equals("place")) {
					//Call place method
					res = place(parameterValues[0], res, pid, lastOperation);
				}
				else {
					//Call transfer method
					res = transfer(parameterNames, parameterValues, res, pid, lastOperation);
					
					//Don't want to keep track of transfer
					//Because of cases like Transfer(place->pick) then another pick.
					//A transfer as an operation would allow that pick where as
					//not including it wouldn't.
				}
			}
		}
		return res;
	}
		
		//Recursively checking MockBot's status.
		//Will update init boolean or abort with error.
		private String checkMockBotStatus(int pid, String res) {
			if(checkBot(pid).equals("In Progress")) {
				checkMockBotStatus(pid, res);
			}
			else if(checkBot(pid).equals("Terminated With Error")) {
				res = "Terminated With Error";
				abort();
			}
			else if(checkBot(pid).equals("Could not read from MockBot")) {
				res = "Could not read from MockBot";
			}
			else {
				ready = true;
			}
			return res;
		}
	

}
