import java.util.*;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.*;

public class DeviceDriver {
	private static List<String> calledOperations = new ArrayList<>();
	private static Socket socket = null;
	private static InputStream input = null;
	private static OutputStream output = null;
	static int port;
	static String over = "Over";
	
	public DeviceDriver(int port) {
		this.port = port;
	}
	
	private void Abort() {
		try {
			//Closing all i/o streams and socket.
			input.close();
			output.close();
			socket.close();
			
			System.out.println("Connection with MockBot has been terminated");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void openConnection(String IPAddress) {
		try{
			System.out.println("IP: "+ IPAddress + " " + "Port: "+ port);
			
			//Create socket and input&output streams
			socket = new Socket(IPAddress, port);
			output = socket.getOutputStream();
			input = socket.getInputStream();
			
			System.out.println("Socket and i/o streams opened");
			
		}
		catch(IOException ex) {
			//System.out.println("I/O error: " + ex.getMessage());
		}
	}
	
	private void initialize() {
		String homeCommand = "home%";
		PrintWriter writer = new PrintWriter(output, true);
		//Sending home command plus over.
		writer.println(homeCommand);
		
		System.out.println("initialized sent");
	}
	
	private int ExecuteOperation(String operation, String[] parameterNames, String[] parameterValues) {
		String command = "";
		int pid = -1;
		//Getting the last called Operation
		String lastOperation = calledOperations.get(calledOperations.size()-1);
		
		//Need to check the status of the last run command to see if the robot is ready for more
		PrintWriter writer = new PrintWriter(output, true);
		
		//*********************
		//Figure out a way to track previous PIDS!!!
		//*********************
				
		//Once there is confirmation the last operation has finished
		//See if operation is possible, then run if it is.
		if(operation.equals("pick")) {
			if(lastOperation.equals("pick")) {
				System.out.println("MockRobot is currently holding an item!");
				return -1;
			}
			else {
				//Since pick can pick up from only 1 location, parameterNames & parameterValues will
				//have a size of 1
				command = operation.concat("%".concat(parameterNames[0].concat("%".concat(parameterValues[0]))));
				//Send command to MockBot
				writer.println(command);
				
				
				//BufferReader will only read as a string
				BufferedReader reader = new BufferedReader(new InputStreamReader(input));
				
				//Wait for response from MockRobot
				while(pid == -1) {
					try {
						pid = Integer.parseInt(reader.readLine());
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				calledOperations.add(operation);
				return pid;
			}
		}
		else if(operation.equals("place")) {
			if(lastOperation.equals("place")) {
				System.out.println("MockRobot doesn't have an item to place!");
				return -1;
			}
			else {
				//Since place can only put an item at  up from only 1 location, parameterNames & parameterValues will
				//have a size of 1
				command = operation.concat("%".concat(parameterNames[0].concat("%".concat(parameterValues[0]))));
				writer.println(command);
				
				//BufferReader will only read as a string
				BufferedReader reader = new BufferedReader(new InputStreamReader(input));
				
				//Wait for response from MockRobot
				while(pid == -1) {
					try {
						pid = Integer.parseInt(reader.readLine());
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				calledOperations.add(operation);
				return pid;
			}
		}
		else {
			if(lastOperation.equals("pick")) {
				System.out.println("MockRobot is currently holding an item!");
				return -1;
			}
			else {
				//Since place can only put an item at  up from only 1 location, parameterNames & parameterValues will
				//have a size of 1
				command = operation.concat("%".concat(parameterNames[0].concat("%".concat(parameterValues[0]))));
				writer.println(command);
				
				//BufferReader will only read as a string
				BufferedReader reader = new BufferedReader(new InputStreamReader(input));
				
				//Wait for response from MockRobot
				while(pid == -1) {
					try {
						pid = Integer.parseInt(reader.readLine());
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				calledOperations.add(operation);
				return pid;
			}
		}
	}
	

	public static void main(String[] args) {
		DeviceDriver driver = new DeviceDriver(1000);
		driver.openConnection("192.168.1.25");
		driver.initialize();
		
		
	}

}
