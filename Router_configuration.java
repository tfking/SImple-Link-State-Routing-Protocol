//Importing statements
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.invoke.ConstantCallSite;

import javax.swing.*;  
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.plaf.SliderUI;

import org.omg.CORBA.portable.IndirectionException;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Queue;
import java.io.*;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.APPEND;
import java.util.zip.CRC32; 
import java.util.zip.Checksum; 

public class Router_configuration extends JFrame{

	//Declaring the router variables 
	private Container content; 
	
	private JLabel version_protocol_number_label, router_id_label, update_interval_label, neigbor_label, number_routers_label,  
	alive_interval_label, link_cost1_label, link_cost2_label, lsrp_id_label, dead_label;  
	
	private JTextField version_protocol_number_textField, router_id_textField, update_interval_textField,neigbor_textField, 
	alive_interval_textField, link_cost1_textField, link_cost2_textField, lsrp_id_textField, dead_textField, number_of_routers_textField; 
	
	private JPanel version_protocol_number_panel, router_id_panel, link1_panel, link2_panel, update_interval_panel, alive_interval_panel
	, link_cost1_panel, link_cost2_panel, lsrp_id_panel, mask1_panel, link_id1_panel, link_id2_panel, mask2_panel, dead_panel, neigbor_panel, number_of_routers_panel;
	
	private JButton ok_button; 
	
	static JMenuBar menu_bar;
	
	JMenu router_parameters_menu, help_menu; 
	
	static String router_id;

	String lsrp_id, cost1, cost2, update_interval, alive_interval, dead_interval, protocol_version_number, device_ip_address, routers_attached
	,device_ip; 
	
	static int router_receive_port, number_of_routers, update_iterval_value, alive_interval_value;
	int position, Ls_sequence_num = 0; 
	
	boolean is_router_registration_complete = false, start_alive_cost = false, start_transfer = false, send_text = true; 
	
	static Queue<String> router_packet_queue = new LinkedList<String>(); 
	static Queue<String> router_resolver_queue = new LinkedList<String>();
	static Queue<String> router_alive_queue = new LinkedList<String>();
	static Queue<String> router_link_cost_queue = new LinkedList<String>();
	static Queue<String> lsa_queue = new LinkedList<String>();
	static Queue<String> ftp_queue = new LinkedList<String>();
	
	String [] attached_routers_id; 
	LinkedList <String> attached_routers_id_info;
	LinkedList <String> link_state_database;
	String shortest_paths = ""; 
	String individual_shortest_path [] = new String[6];

	// Name resolver Method
	class Resolver_thread implements Runnable {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			//Location of the Name_Resolver file 
			    Path path = FileSystems.getDefault().getPath("C:/Users/user/Documents/Universities/Master/University of Pittsburgh/4 - Spring 2016/Wide Area Networks/Project", "Name_resolver.txt");
				String ip_address_resolver = "";
				String port_resolver = ""; 
				
				// Get the IP address of the router. 
				String device_ip_name = "";
				try {
					device_ip_name = ""+InetAddress.getLocalHost();
				} catch (UnknownHostException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} 
				device_ip= device_ip_name.substring(device_ip_name.indexOf("/")+1);
				String device_name = device_ip_name.substring(device_ip_name.indexOf(" ")+1, device_ip_name.indexOf("/"));
				
				//Check if the File does not exist and perform the required operations.
				if (Files.notExists(path)) { 
					System.out.println("The Name resolver is being created.");
					synchronized (path) {
						System.out.println("File is being created and the used by Name resolver Thread");
						try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(path, CREATE, APPEND))){
							
							ip_address_resolver = device_ip; 
							port_resolver = ""+router_receive_port; 
			
							String device_Info = "Name_Resolver:			IP_addrress: " +device_ip+" 		Port_Number: " + router_receive_port +
									" 		Device_name: " +device_name+" \n";
							//Register the Router information 
							String router_info = "Router"+router_id+":			IP_addrress: " +device_ip+" 		Port_Number: " + router_receive_port +
									" 		Device_name: " +device_name+"\n";
							
							byte data[] = (device_Info+router_info).getBytes();
							out.write(data);
							out.close(); 
						}
						catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
					}
						System.out.println("File is released");
					}					    
				}
				
				else {
					//if the name resolver already exists, do the following 
					//open the file and get the IP address of the Name resolver
					System.out.println("The Name resolver already exists");
					synchronized (path) {
						System.out.println("the file is been locked for reading the Name resolver information."); 
						try (InputStream in = Files.newInputStream(path);
								OutputStream out = new BufferedOutputStream(Files.newOutputStream(path, APPEND)); 
								BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
								String line = null; 
								while ((line = reader.readLine()) != null) {
									if (line.toLowerCase().contains("Name_resolver".toLowerCase())) {
										String [] values = line.split(" ");  
										ip_address_resolver = values [1];
										port_resolver = values[3]; 
									}
								}
								//Build the packet to be sent to the name resolver router_id-packet_type-device_name-device_ip-port_number
								String packet_resolver = router_id + "-resolver_register-" + device_name + "-" +device_ip + "-" + router_receive_port+"-"; 
								 
							    //Add the header before putting in the queue: Header is the source_ip-source_port-destination_ip-destination_port
								String packet_added_in_queue = device_ip +"-"+router_receive_port+"-"+ip_address_resolver+"-"+port_resolver +"-"+ packet_resolver; 
								out.close();
								synchronized(router_packet_queue) {
									router_packet_queue.offer(packet_added_in_queue);  
								}
							 System.out.println("The file has been released"); 
							} catch (UnknownHostException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace(); 
							}	
					}
				}
			while (true) {
				synchronized (router_resolver_queue) {
					if (router_resolver_queue.size() > 0) {
						String router_info_to_be_written = router_resolver_queue.peek();
						try  {
							if (router_info_to_be_written.contains("resolver_register")) {
								OutputStream out1 = new BufferedOutputStream(Files.newOutputStream(path, APPEND));		 
								String [] packet_field1 = router_info_to_be_written.split("-"); 
								String router_id_received = packet_field1[4];
								String router_ip_received = packet_field1[7]; 
								String router_port_received = packet_field1[8]; 
								String device_name_received = packet_field1[6]; 
								String inf_to_be_written = "Router"+router_id_received+":			IP_addrress: " +router_ip_received+" 		Port_Number: " + router_port_received +
										" 		Device_name: " +device_name_received+" \n";
								
								byte [] data1 = inf_to_be_written.getBytes(); 
								out1.write(data1);
								out1.close(); 
								
								//count the number of lines in the file
								InputStream in = Files.newInputStream(path);
							    BufferedReader reader = new BufferedReader(new InputStreamReader(in)); 
								int lines = 0; 
								while ((reader.readLine()) != null) {
									lines++; 	
								}
								in.close();
								reader.close(); 
								//checking if the registration is complete
								if (lines == (number_of_routers + 1)) {
									is_router_registration_complete = true;
									InputStream in1 = Files.newInputStream(path);
									BufferedReader reader1 = new BufferedReader(new InputStreamReader(in1));
									String line1 = null;
									while ((line1 = reader1.readLine()) != null) {
										if (line1.contains("Router")) {
											String dest_values [] = line1.split(" ");  
											String dest_ip = dest_values[1];  
											String dest_port = dest_values[3]; 
											
											//Build the packet to acknowledge that the router registration is complete router_id-packet_type-device_name-device_ip-port_number
											String resolver_complete = router_id + "-resolver_complete-"+"-"; 
											//Add the header before putting in the queue: Header is the source_ip-source_port-destination_ip-destination_port
											String packet_added_in_queue = device_ip +"-"+router_receive_port+"-"+dest_ip+"-"+dest_port +"-"+ resolver_complete;
											
											synchronized(router_packet_queue) {
												router_packet_queue.offer(packet_added_in_queue);  
											}
										}
									}
									in1.close();
								}
								
								router_resolver_queue.remove(); 
							}
							else if (router_info_to_be_written.contains("resolver_complete")) {
								is_router_registration_complete = true;
								
								for (int i=0; i<attached_routers_id.length; i++) {
									//Build a packet and send to the name resolver requesting the information of the attached router 
									//Build the packet to be sent to the name resolver router_id-packet_type-device_name-device_ip-port_number
									String packet_resolver_resolve = router_id + "-resolver_resolve-" + attached_routers_id[i] +"--"; 
									 
								    //Add the header before putting in the queue: Header is the source_ip-source_port-destination_ip-destination_port
									String packet_added_in_queue = device_ip +"-"+router_receive_port+"-"+ip_address_resolver+"-"+port_resolver +"-"+ packet_resolver_resolve; 
									 
									synchronized(router_packet_queue) {
										router_packet_queue.offer(packet_added_in_queue);  
									}
								}
								router_resolver_queue.remove();
							}
							
							else if (router_info_to_be_written.contains("resolver_resolve")) {
								String id_to_be_resolve = (router_info_to_be_written.split("-"))[6];
								String dest_ip = (router_info_to_be_written.split("-"))[0];
								String dest_port = (router_info_to_be_written.split("-"))[1];
								
								//open the file name resolver and get the proper information to send to the requesting router 
								InputStream in2 = Files.newInputStream(path);
								BufferedReader reader2 = new BufferedReader(new InputStreamReader(in2));
								String line1 = null;
								while ((line1 = reader2.readLine())!= null) {
									if (line1.contains("Router"+id_to_be_resolve)) {
										String requested_values [] = line1.split(" ");  
										String requested_ip = requested_values[1];  
										String requested_port = requested_values[3]; 
										
										//Build the packet to acknowledge that the router registration is complete router_id-packet_type-device_name-device_ip-port_number
										String requested_info = router_id + "-resolver_answer-" + requested_ip + "-"+ requested_port+ "-" + id_to_be_resolve + "-"; 
										//Add the header before putting in the queue: Header is the source_ip-source_port-destination_ip-destination_port
										String packet_added_in_queue = device_ip +"-"+router_receive_port+"-"+dest_ip+"-"+dest_port +"-"+ requested_info;
										
										synchronized(router_packet_queue) {
											router_packet_queue.offer(packet_added_in_queue);  
										}
									}
								}
								in2.close();
								reader2.close(); 
								router_resolver_queue.remove(); 
							}
							
							else if (router_info_to_be_written.contains("resolver_answer")) {
								String adjacent_router_ip = (router_info_to_be_written.split("-"))[6]; 
								String adjacent_router_port = (router_info_to_be_written.split("-"))[7];
								String adjacent_router_id = (router_info_to_be_written.split("-"))[8];
								
								if (!adjacent_router_id.equals(router_id)) {
									//Build the packet to acknowledge that the router registration is complete router_id-packet_type-device_name-device_ip-port_number
									String requested_info = router_id + "-Be_Neighbors_Request-" + adjacent_router_id + "-"+ dead_interval+ "-" + update_interval + "-" + protocol_version_number+"-"; 
									//Add the header before putting in the queue: Header is the source_ip-source_port-destination_ip-destination_port
									String packet_added_in_queue = device_ip +"-"+router_receive_port+"-"+adjacent_router_ip+"-"+adjacent_router_port +"-"+ requested_info;
									
									synchronized(router_packet_queue) {
										router_packet_queue.offer(packet_added_in_queue);  
									}
								}
							    router_resolver_queue.remove(); 
							}
							
							else if (router_info_to_be_written.contains("Be_Neighbors_Request")) {
								//get the version protocol number from the packet 
								String version_protocol_received = router_info_to_be_written.split("-")[9]; 
								String adjacent_router_id_received = router_info_to_be_written.split("-")[4];
								String dead_interval_received = router_info_to_be_written.split("-")[7]; 
								String update_interval_received = router_info_to_be_written.split("-")[8]; 
								String adjacent_router_ip = (router_info_to_be_written.split("-"))[0]; 
								String adjacent_router_port = (router_info_to_be_written.split("-"))[1];
								
								if (protocol_version_number.equals(version_protocol_received)) {
									
									start_alive_cost = true; 
									
									//check if the update interval is the same: if not create a common update interval for both routers 
									if (!(dead_interval.equals(dead_interval_received))) 
										if (Integer.parseInt(dead_interval) < Integer.parseInt(dead_interval_received))
											dead_interval =  "" + Integer.parseInt(dead_interval_received);
										else 
											dead_interval = "" + Integer.parseInt(dead_interval);
									alive_interval_value = Integer.parseInt(dead_interval) * 1000; 
									
									if (!(update_interval.equals(update_interval_received))) 
										if (Integer.parseInt(update_interval) < Integer.parseInt(update_interval_received))
											update_interval =  "" + Integer.parseInt(update_interval_received);
										else 
											update_interval =  "" + Integer.parseInt(update_interval);
									
									update_iterval_value = Integer.parseInt(update_interval) * 1000; 
									//Build the packet to confirm the neighbor  router_id-packet_type-device_name-device_ip-port_number
									String requested_info = router_id + "-Be_Neighbors_Confirm-" + adjacent_router_id_received + "-"+ dead_interval+ "-" + update_interval +"-"; 
									//Add the header before putting in the queue: Header is the source_ip-source_port-destination_ip-destination_port
									String packet_added_in_queue = device_ip +"-"+router_receive_port+"-"+adjacent_router_ip+"-"+adjacent_router_port +"-"+ requested_info;
									
									
									synchronized(router_packet_queue) {
										router_packet_queue.offer(packet_added_in_queue);  
									}
									
									String adjacent_router_cumulated_info = "Router"+adjacent_router_id_received + "-" + adjacent_router_ip + "-" + adjacent_router_port + "-" + dead_interval + update_interval; 
								    synchronized(attached_routers_id_info) {
								    	attached_routers_id_info.offer(adjacent_router_cumulated_info);
								    	System.out.println(attached_routers_id_info); 
								    }
								    router_resolver_queue.remove(); 
								}
								else {
									System.out.println("Packet have been dropped"); 
									//Build the packet to deny the neighbor  router_id-packet_type-device_name-device_ip-port_number
									String requested_info = router_id + "-Be_Neighbors_Refuse-" + adjacent_router_id_received +"-"; 
									//Add the header before putting in the queue: Header is the source_ip-source_port-destination_ip-destination_port
									String packet_added_in_queue = device_ip +"-"+router_receive_port+"-"+adjacent_router_ip+"-"+adjacent_router_port +"-"+ requested_info;
									
									synchronized(router_packet_queue) {
										router_packet_queue.offer(packet_added_in_queue);  
									}
									router_resolver_queue.remove(); 
								}
							}
							
							else if (router_info_to_be_written.contains("Cease_Neighbors_Request")) {
								//id of the neighbor to be removed 
								String adjacent_router_id_received = router_info_to_be_written.split("-")[4];
								
								synchronized(attached_routers_id_info) {
									for (int i=0; i<attached_routers_id_info.size(); i++) {
										//Get the neighbor information 
										String neighbor_information = attached_routers_id_info.get(i);
										
										String neighbor_id = (neighbor_information.split("-")[0]).substring(6); 
										System.out.println(neighbor_id); 
										String neighbor_ip = neighbor_information.split("-")[1]; 
										String neighbor_port = neighbor_information.split("-")[2];
										
										if (neighbor_id.equals(adjacent_router_id_received)) {
											
											System.out.println("Neighbor " + neighbor_id + "has been removed"); 
											attached_routers_id_info.remove(i);
											// Generate cease neighbor confirm 
											String requested_info = router_id + "-Cease_Neighbors_Confirm-" + neighbor_id +"-"; 
											//Add the header before putting in the queue: Header is the source_ip-source_port-destination_ip-destination_port
											String packet_added_in_queue = device_ip +"-"+router_receive_port+"-"+neighbor_ip+"-"+neighbor_port +"-"+ requested_info;
											
											synchronized(router_packet_queue) {
												router_packet_queue.offer(packet_added_in_queue);  
											}
										}
									}
									System.out.println(attached_routers_id_info);
								}
								router_resolver_queue.remove(); 
							}
							
							else {
								//System.out.println(router_info_to_be_written); 
								router_resolver_queue.remove(); 
							}
						
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				double rand = Math.random(); 
				synchronized(attached_routers_id_info) {
					if (rand >= 2 && attached_routers_id_info.size() > 0 && start_alive_cost == true) {
						System.out.println("Removing neighbor"); 
				    	//picked a neighbor to cease the request: 
						String neighbor_information = attached_routers_id_info.get(0);
						
						//Extract the ip and port number to send the packet
						String neighbor_id = (neighbor_information.split("-")[0]).substring(6); 
						String neighbor_ip = neighbor_information.split("-")[1]; 
						String neighbor_port = neighbor_information.split("-")[2];
						
						// Generate cease neighbor request 
						String requested_info = router_id + "-Cease_Neighbors_Request-" + neighbor_id +"-"; 
						//Add the header before putting in the queue: Header is the source_ip-source_port-destination_ip-destination_port
						String packet_added_in_queue = device_ip +"-"+router_receive_port+"-"+neighbor_ip+"-"+neighbor_port +"-"+ requested_info;
						
						synchronized(router_packet_queue) {
							router_packet_queue.offer(packet_added_in_queue);  
						}
						attached_routers_id_info.remove(0); 
				    	System.out.println(attached_routers_id_info); 
				    }
					
				}
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
	
		}
		
	}
	
	//Thread that will periodically check if the neighbors are still alive
	class Alive_Message implements Runnable {
		@Override
		public void run() {
			// TODO Auto-generated method stub
				while (true) {
					if (start_alive_cost == true) {
						System.out.println("ALIVE MESSAGE ");
						
						int number_of_packets_sent = 0; 
						long timer_start_alive = 0;
						String [] alive_router; 
						
						synchronized(attached_routers_id_info) {
							 
							number_of_packets_sent = attached_routers_id_info.size();
							//create an array to store the result of the alive checking 
							alive_router = new String [attached_routers_id_info.size()]; 
							
							//Initialize the array
							for (int l=0; l<alive_router.length; l++) {
								String entry = "Alive"+((attached_routers_id_info.get(l).split("-")[0]).substring(6)) + "=0"; 
								alive_router[l] = entry;  
							}
							
							if (attached_routers_id_info.size() > 0) {
								for (int i=0; i<attached_routers_id_info.size(); i++) {
									//Get the neighbor information 
									String neighbor_information = attached_routers_id_info.get(i);
									
									//Extract the ip and port number to send the packet
									String neighbor_id = neighbor_information.split("-")[0]; 
									String neighbor_ip = neighbor_information.split("-")[1]; 
									String neighbor_port = neighbor_information.split("-")[2];
									
									//Build the packet to check if the neighbor is still alive  router_id-packet_type-device_name-device_ip-port_number
									String neigbor_alive = router_id + "-Alive_message_check-Hello" +"-"; 
									//Add the header before putting in the queue: Header is the source_ip-source_port-destination_ip-destination_port
									String packet_added_in_queue = device_ip +"-"+router_receive_port+"-"+neighbor_ip+"-"+neighbor_port +"-"+ neigbor_alive;
									
									//System.out.println("Checking if the neigbor " + neighbor_id + " is alive"); 
									synchronized(router_packet_queue) {
										router_packet_queue.offer(packet_added_in_queue);  
									}		
								}
								//Starting the timer for the 
								timer_start_alive = System.currentTimeMillis(); 
							}
						}
							while(number_of_packets_sent > 0) {
								long time_stopped_at = System.currentTimeMillis(); 
								long max_time_to_wait = time_stopped_at - timer_start_alive;
								
								synchronized (router_alive_queue) {
									if (router_alive_queue.size() > 0) {
										String message_in_queue = router_alive_queue.peek();
										if (message_in_queue.contains("Alive_message_check")) {
											String neighbor_ip = message_in_queue.split("-")[0]; 
											String neighbor_port = message_in_queue.split("-")[1];
											String neighbor_id = message_in_queue.split("-")[4];
											
											//Build the packet to say you are alive  router_id-packet_type-device_name-device_ip-port_number
											String neigbor_alive = router_id + "-Alive_message_reply-Hello" +"-"; 
											//Add the header before putting in the queue: Header is the source_ip-source_port-destination_ip-destination_port
											String packet_added_in_queue = device_ip +"-"+router_receive_port+"-"+neighbor_ip+"-"+neighbor_port +"-"+ neigbor_alive;
											
											//System.out.println("replying to neighbor " + neighbor_id + " that i am still alive"); 
											synchronized(router_packet_queue) {
												router_packet_queue.offer(packet_added_in_queue);  
											}		
											router_alive_queue.remove(); 
										}		
										
										else if (message_in_queue.contains("Alive_message_reply")) {
											
											number_of_packets_sent --; 
											
											long timer_end_alive = System.currentTimeMillis();
											
											String neighbor_id = message_in_queue.split("-")[4];
											
											for (int m=0; m<alive_router.length; m++) {
												if ((alive_router[m]).contains("Alive"+neighbor_id)) {
													alive_router[m] = "Alive"+neighbor_id+"=alive";
												}
											}
											router_alive_queue.remove(); 
										}
										else
											router_alive_queue.remove(); 
									}
								}
								if (max_time_to_wait > 2000000) {
									System.out.println("A ROUTER IS NO MORE ALIVE ");
									for (int m=0; m<alive_router.length; m++) {
										if (alive_router[m].contains("=0")) {
											//Id of the router dead: 
											String id_dead_router = (alive_router[m].split("=")[0]).substring(5);
											synchronized (attached_routers_id_info) {
												for (int i=0; i<attached_routers_id_info.size(); i++) {
													//Get the neighbor information 
													String neighbor_information = attached_routers_id_info.get(i);
													
													String neighbor_id = (neighbor_information.split("-")[0]).substring(6); 
													System.out.println(id_dead_router); 
													String neighbor_ip = neighbor_information.split("-")[1]; 
													String neighbor_port = neighbor_information.split("-")[2];
													
													if (neighbor_id.equals(id_dead_router)) {
														System.out.println("Neighbor " + neighbor_id + "has been removed"); 
														attached_routers_id_info.remove(i);
														}
												}
											}			
										}
									}
									number_of_packets_sent = 0; 
								}
						}
					}
						try {
							Thread.sleep(alive_interval_value);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} 
				} 
		}
		
	}
	
	//To measure the link cost of the adjacent routers
	class Link_Cost_Messages implements Runnable {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			while (true) {
				if (start_alive_cost == true) {
					System.out.println("LINK COST EVALUATION");
					
					int number_of_packets_sent = 0; 
					String [] link_cost_router; 
					long timer_start_link_cost = 0; 
					
					synchronized (attached_routers_id_info) {
						int number_of_acks_received = 0; 
						number_of_packets_sent = 10*attached_routers_id_info.size(); 
						
						//create an array to take store the average delay of the links
						link_cost_router = new String [attached_routers_id_info.size()]; 
						
						//Initialize the array
						for (int l=0; l<link_cost_router.length; l++) {
							String entry = "sum"+((attached_routers_id_info.get(l).split("-")[0]).substring(6)) + "=0-count=0-delay=0"; 
							link_cost_router[l] = entry;  
						}
						
						if (attached_routers_id_info.size() > 0 ) {
							for (int i=0; i<attached_routers_id_info.size(); i++) {
								//Get the neighbor information 
								String router_to_send_link_cost_msg = attached_routers_id_info.get(i);
								
								//Extract the ip and port number to send the packet
								String neighbor_id = router_to_send_link_cost_msg.split("-")[0]; 
								String neighbor_ip = router_to_send_link_cost_msg.split("-")[1]; 
								String neighbor_port = router_to_send_link_cost_msg.split("-")[2];
								
								//Build the packet to check if the neighbor is still alive  router_id-packet_type-device_name-device_ip-port_number
								String link_cost_msg = router_id + "-link_cost_message_evaluate-Hello" +"-"; 
								//Add the header before putting in the queue: Header is the source_ip-source_port-destination_ip-destination_port
								String packet_added_in_queue = device_ip +"-"+router_receive_port+"-"+neighbor_ip+"-"+neighbor_port +"-"+ link_cost_msg;
								
								synchronized(router_packet_queue) {
									for (int j = 0; j < 10; j ++) {
										//System.out.println("evaluating the cost of the link for  " + neighbor_id); 
										router_packet_queue.offer(packet_added_in_queue);  
									}
								}	
							}
							//Starting the timer for the 
						    timer_start_link_cost = System.currentTimeMillis(); 
						}
					}
				 while (number_of_packets_sent > 0)	{
					synchronized (router_link_cost_queue) {
						if (router_link_cost_queue.size() > 0) {
							String link_cost_message = router_link_cost_queue.peek(); 
							
							if (link_cost_message.contains("evaluate")) {
								//Extract the ip and port number to send the packet
								String neighbor_id = link_cost_message.split("-")[4]; 
								String neighbor_ip = link_cost_message.split("-")[0]; 
								String neighbor_port = link_cost_message.split("-")[1];
								
								//Build the packet to check if the neighbor is still alive  router_id-packet_type-device_name-device_ip-port_number
								String link_cost_msg = router_id + "-link_cost_message_reply-Hello" +"-"; 
								//Add the header before putting in the queue: Header is the source_ip-source_port-destination_ip-destination_port
								String packet_added_in_queue = device_ip +"-"+router_receive_port+"-"+neighbor_ip+"-"+neighbor_port +"-"+ link_cost_msg;
								
								//System.out.println("I received a link cost evaluate, i am sending the acknowledgement to router" +  neighbor_id); 
								synchronized(router_packet_queue) {
									router_packet_queue.offer(packet_added_in_queue);  
								}
								
								router_link_cost_queue.remove(); 
							}
							
							else if (link_cost_message.contains("cost_message_reply")) {
								number_of_packets_sent --;
								//check which router send the reply 
								String neighbor_id = link_cost_message.split("-")[4];
								
								long timer_end_link_cost = System.currentTimeMillis();
								long cost = timer_end_link_cost - timer_start_link_cost; 
								
								for (int k=0; k<link_cost_router.length; k++) {
									if ((link_cost_router[k]).contains("sum"+neighbor_id)) {
										int sum =  (int) (Integer.parseInt((link_cost_router[k].split("-")[0]).split("=")[1]) + cost); 
										int count = (int) (Integer.parseInt((link_cost_router[k].split("-")[1]).split("=")[1]) + 1);
	                                    
										if (count == 10) {
											int delay = sum/count;
											//Mapping the delay into relative cost 
											if (delay > 0 && delay <= (500))
												delay = 1; 
											else if (delay > 500  && delay <= 1000)
												delay = 2; 
											else if (delay > 1000 && delay <= 2000)
												delay = 3; 
											else if(delay > 2000 && delay <=3000 )
												delay = 4; 
											else if(delay > 3000 && delay <=4000)
												delay = 6;
											else if(delay > 4000 && delay <=5000)
												delay = 7;
											else if(delay > 6000 && delay <=7000)
												delay = 8;
											else
												delay = 9; 
												
											link_cost_router[k] = "sum"+neighbor_id+"="+sum+"-count="+count+"-delay="+delay;
										}
										else
											link_cost_router[k] = "sum"+neighbor_id+"="+sum+"-count="+count+"-delay=0"; 
									}
								}
								 
								router_link_cost_queue.remove();
							}
							else 
								router_link_cost_queue.remove(); 
						}
					}
				  }
				 
				 
				 
				 synchronized(link_cost_router) {
					//create the LSA packet to be sent out 
					//Build the LSA packet to be sent out   router_id-packet_type-device_name-device_ip-port_number
					String a = router_id;
					int age = 0; 
					int lsa_Length = 300;
					int number_of_links = link_cost_router.length; 
					String links_info = ""; 
					//getting the links information 
					for (int w=0; w<link_cost_router.length; w++) {
						
						String link_router_id = ((link_cost_router[w].split("="))[0]).substring(3);
						String link_router_id_combined = router_id+link_router_id;
						String link_router_cost = ((link_cost_router[w].split("="))[3]); 
						String link_id_cost = link_router_id_combined + ";"+link_router_cost; 
						
						//data entry 
						String data_to_be_added = link_router_id_combined+"-"+link_router_cost+"-0-0-"; 
						boolean match_found1 = false; 
						
						synchronized(link_state_database) {
							if (link_state_database.size() == 0) {
								link_state_database.add(data_to_be_added); 
								 
							}
							else {
								for (int j=0; j<link_state_database.size(); j++) {
									if (((link_state_database.get(j)).split("-")[0]).equalsIgnoreCase(link_router_id_combined)) {
										match_found1 = true; 
										String database_entry = link_state_database.get(j); 
										String database_entry_sequence_number =  database_entry.split("-")[2]; 
										link_state_database.set(j, data_to_be_added);
									}
								}
								if (match_found1 == false) {
									link_state_database.add(data_to_be_added); 
								}
							}
						}
						links_info =  links_info + link_id_cost + ":"; 
					}
					String lsa_msg = router_id + "-lsa_advertising_router"+"-"+a+"-" +age+"-"+Ls_sequence_num+"-"+lsa_Length+"-"+number_of_links+"-"+links_info+"-"; 
					//Add the header before putting in the queue: Header is the source_ip-source_port-destination_ip-destination_port
					synchronized (attached_routers_id) {
						for (int b=0; b<attached_routers_id_info.size(); b++) {
							
							//Get the neighbor information 
							String router_to_send_link_cost_msg = attached_routers_id_info.get(b);
							
							//Extract the ip and port number to send the packet
							String neighbor_id = router_to_send_link_cost_msg.split("-")[0]; 
							String neighbor_ip = router_to_send_link_cost_msg.split("-")[1]; 
							String neighbor_port = router_to_send_link_cost_msg.split("-")[2];
							
							String packet_added_in_queue = device_ip +"-"+router_receive_port+"-"+neighbor_ip+"-"+neighbor_port +"-"+ lsa_msg;
							
							synchronized(router_packet_queue) {
								router_packet_queue.offer(packet_added_in_queue);  
								Ls_sequence_num ++; 
							}
						}
					}
				 }
				}
					try {
						Thread.sleep(update_iterval_value);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
			} 
		}
		
	}
	
	
	class Lsa_message implements Runnable  {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			while(true) {
				synchronized (lsa_queue) {
					if (lsa_queue.size() > 0 ) {
						String received_lsa_message = lsa_queue.peek();  
						
						if (received_lsa_message.contains("lsa_advertising_router")) {
							boolean match_found = false; 
							
							//retrieve the needed information
							String advertising_router_id = received_lsa_message.split("-")[6];
							String sender_router_id = received_lsa_message.split("-")[4];
							String lsa_sequence_number = received_lsa_message.split("-")[8];
							String lsa_lenght = received_lsa_message.split("-")[9];
							String number_of_links_lsa = received_lsa_message.split("-")[10];
							String age_lsa = received_lsa_message.split("-")[7];
							
							String info_link = received_lsa_message.split("-")[11];
							
							String [] info_link_array = info_link.split(":"); 
							
							for (int i=0; i<info_link_array.length; i++) {
								String current_info_link = info_link_array[i]; 
								
								String source_dest = current_info_link.split(";")[0];
								String lsa_cost = current_info_link.split(";")[1];
								
								//data entry 
								String data_to_be_added = source_dest+"-"+lsa_cost+"-"+lsa_sequence_number+"-"+age_lsa; 
								
								synchronized(link_state_database) {
									if (link_state_database.size() == 0) {
										link_state_database.add(data_to_be_added); 
										 
									}
									else {
										for (int j=0; j<link_state_database.size(); j++) {
											if (((link_state_database.get(j)).split("-")[0]).equalsIgnoreCase(source_dest)) {
												match_found = true; 
												String database_entry = link_state_database.get(j); 
												String database_entry_sequence_number =  database_entry.split("-")[2]; 
												
												if (Integer.parseInt(database_entry_sequence_number) < Integer.parseInt(lsa_sequence_number)) {
													link_state_database.set(j, data_to_be_added); 
												}
											}
										}
										if (match_found == false) {
											link_state_database.add(data_to_be_added); 
										}
									}
									
									String database = ""; 
									for (int k=0; k<link_state_database.size(); k++) {
										database = database + "_"+link_state_database.get(k); 
									}
									System.out.println("The actual link database is:  " + database);
									if (database.length() > 6) {
										//get the costs of the paths
										String split [] = database.split("_"); 
										int a = 100, b=100, c = 100, d= 100, e=100, f=100, w = 100, h=100, j=100, k=100, l=100,
												m=100; 
										for (int z=0; z<split.length; z++) {
											if ((split[z].split("-")[0]).contains("12")) 
												a = Integer.parseInt(split[z].split("-")[1]); 
											
											else if (((split[z].split("-")[0]).contains("13")))
												b = Integer.parseInt(split[z].split("-")[1]);
											
											else if (((split[z].split("-")[0]).contains("21")))
												c = Integer.parseInt(split[z].split("-")[1]);
											
											else if (((split[z].split("-")[0]).contains("23")))
												d = Integer.parseInt(split[z].split("-")[1]);
											
											else if (((split[z].split("-")[0]).contains("24")))
												e = Integer.parseInt(split[z].split("-")[1]);
											
											else if (((split[z].split("-")[0]).contains("42")))
												f = Integer.parseInt(split[z].split("-")[1]);
											
											else if (((split[z].split("-")[0]).contains("45")))
												w = Integer.parseInt(split[z].split("-")[1]);
											
											else if (((split[z].split("-")[0]).contains("31")))
												h = Integer.parseInt(split[z].split("-")[1]);
											
											else if (((split[z].split("-")[0]).contains("32")))
												j = Integer.parseInt(split[z].split("-")[1]);
											
											else if (((split[z].split("-")[0]).contains("35")))
												k = Integer.parseInt(split[z].split("-")[1]);
											
											else if (((split[z].split("-")[0]).contains("54")))
												l = Integer.parseInt(split[z].split("-")[1]);
											
											else if (((split[z].split("-")[0]).contains("53")))
												m = Integer.parseInt(split[z].split("-")[1]);
										}
										if (a < 100 && b < 100 && c < 100 && d < 100 && e < 100 && f < 100 /*&& w < 100 && h < 100 && j < 100 && k < 100*/ && l < 100 && m < 100)
											start_transfer = true; 
										
										 
										   final Graph.Edge[] GRAPH = {
												      new Graph.Edge("1", "2", a),
												      new Graph.Edge("1", "3", b),
												      new Graph.Edge("2", "1", c),
												      new Graph.Edge("2", "3", d),
												      new Graph.Edge("2", "4", e),
												      new Graph.Edge("4", "2", f),
												      new Graph.Edge("4", "5", w),
												      new Graph.Edge("3", "1", h),
												      new Graph.Edge("3", "2", j),
												      new Graph.Edge("3", "5", k),
												      new Graph.Edge("5", "4", l),
												      new Graph.Edge("5", "3", m),
												   };
											   
											   final String START = router_id;
											   final String END = "5";
											   
											   
											   Graph g = new Graph(GRAPH);
											      g.dijkstra(START);
											      //g.printPath(END);
											      ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
													PrintStream ps = new PrintStream(baos); 
													
													PrintStream old = System.out; 
													
													System.setOut(ps); 
													g.printAllPaths();
													System.out.flush(); 
													System.setOut(old);
													 
													shortest_paths = baos.toString(); 
													System.out.println(shortest_paths); 
													 
													individual_shortest_path = shortest_paths.split("\\n"); 
									}
								}
					    	}
							
							//Send the received lsa to all your neigbors; except the neigbor who sent it 
							String lsa_to_be_forwarded = router_id+"-lsa_advertising_router-"+advertising_router_id+"-"+age_lsa+"-"+lsa_sequence_number+"-"+lsa_lenght+"-"+number_of_links_lsa+"-"+info_link+"-"; 
							
							synchronized (attached_routers_id_info) {
								for (int b=0; b<attached_routers_id_info.size(); b++) {
									
									//Get the neighbor information 
									String router_to_send_link_cost_msg = attached_routers_id_info.get(b);
									
									//Extract the ip and port number to send the packet
									String neighbor_id = (router_to_send_link_cost_msg.split("-")[0])/*.substring(6)*/;  
									
									if (!neighbor_id.equals(advertising_router_id) && !neighbor_id.equals(sender_router_id)) {
										String neighbor_ip = router_to_send_link_cost_msg.split("-")[1]; 
										String neighbor_port = router_to_send_link_cost_msg.split("-")[2];
										
										String packet_added_in_queue = device_ip +"-"+router_receive_port+"-"+neighbor_ip+"-"+neighbor_port +"-"+ lsa_to_be_forwarded;
										
										synchronized(router_packet_queue) {
											router_packet_queue.offer(packet_added_in_queue);   
									}
								  }
								}
							}
							
							lsa_queue.remove(); 
						}
						else 
							lsa_queue.remove(); 
					}
				}
				try {
					Thread.sleep(update_iterval_value);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
		
	class Shortest_path_send implements Runnable  {
		@Override
		public void run() {
			while (true) {
					String text = "hello from here"; 
					System.out.println("FTP TRANSFER SEND TEXT IS  " + send_text + " " + start_transfer);
					synchronized (individual_shortest_path) {
						if (send_text == true && start_transfer == true) {
							System.out.println("I AM ABOUT TO SEND THE TEXT"); 
							for(int g=0; g<individual_shortest_path.length; g++) {
								System.out.println(individual_shortest_path[g]);
								if ((individual_shortest_path[g].split("-")[0]).contains(router_id) && individual_shortest_path[g].contains("5") && start_transfer == true && individual_shortest_path[g].split(">")[(individual_shortest_path[g].split(">").length-1)].contains("5")) {
									//Send the received lsa to all your neigbors; except the neigbor who sent it
									System.out.println("in router_id is "+ router_id + " " + individual_shortest_path[g]);
									String text_to_send = router_id+"-ftp-"+text+"-5-";
									// get the next hop in the path 
									String next_hop = (individual_shortest_path[g].split(">")[1]).split("\\(")[0].replace(" ", ""); 
									System.out.println("next hop is "+ next_hop);
									synchronized (attached_routers_id) {
										System.out.println("I HAVE ACCESS THE NEIGBOR DATA STRUCTURE ");
										for (int b=0; b<attached_routers_id_info.size(); b++) {
											
											//Get the neighbor information 
											String router_to_send_link_cost_msg = attached_routers_id_info.get(b);
											
											//Extract the ip and port number to send the packet
											String neighbor_id = (router_to_send_link_cost_msg.split("-")[0]).substring(6); 
											System.out.println("neighbior id to be checked" + neighbor_id);
											if (neighbor_id.equals(next_hop)) {
												System.out.println("I AM IN THE " + neighbor_id);
												String neighbor_ip = router_to_send_link_cost_msg.split("-")[1]; 
												String neighbor_port = router_to_send_link_cost_msg.split("-")[2];
												
												System.out.println("I AM IN THE " + neighbor_ip);
												String packet_added_in_queue = device_ip +"-"+router_receive_port+"-"+neighbor_ip+"-"+neighbor_port +"-"+ text_to_send;
												System.out.println("I SENT IT TO" + next_hop); 
												synchronized(router_packet_queue) {
													router_packet_queue.offer(packet_added_in_queue);   
											}
										  }
										}
									}
								}
							}
						}
					}
					
					synchronized (ftp_queue) {
						if (ftp_queue.size() > 0 && start_transfer == true) {
							System.out.println("I RECEIVE A TEXT TO FORWARD");
							String received_ftp_message = ftp_queue.peek();
							//get the destination ID 
							String destination_id = received_ftp_message.split("-")[7];
							String msg_to_send = received_ftp_message.split("-")[6];
							
							String text_to_forward = router_id+"-ftp-"+msg_to_send+"-" +destination_id+ "-";
							System.out.println("RECEIVED MESSAGE " + received_ftp_message);
							System.out.println("I RECEIVE A" + router_id + " " + destination_id);
							//Get the shortest path to the destination 
							if (!router_id.equals(destination_id)) {
								System.out.println("I AM FORWARDING THE TEXT to the Next hop ");
								synchronized (individual_shortest_path) {
									for(int g=0; g<individual_shortest_path.length; g++) {
										if ((individual_shortest_path[g].split("-")[0]).contains(router_id) && individual_shortest_path[g].contains(destination_id) && individual_shortest_path[g].split(">")[(individual_shortest_path[g].split(">").length-1)].contains(destination_id)) {
											
											System.out.println("shortest path is  " + individual_shortest_path[g]);
											// get the next hop in the path 
											String next_hop = (individual_shortest_path[g].split(">")[1]).split("\\(")[0].replace(" ", "");
											
											synchronized (attached_routers_id) {
												for (int b=0; b<attached_routers_id_info.size(); b++) {
													
													//Get the neighbor information 
													String router_to_send_link_cost_msg = attached_routers_id_info.get(b);
													
													//Extract the ip and port number to send the packet
													String neighbor_id = (router_to_send_link_cost_msg.split("-")[0]).substring(6);  
													
													if (neighbor_id.equals(next_hop)) {
														String neighbor_ip = router_to_send_link_cost_msg.split("-")[1]; 
														String neighbor_port = router_to_send_link_cost_msg.split("-")[2];
														
														String packet_added_in_queue = device_ip +"-"+router_receive_port+"-"+neighbor_ip+"-"+neighbor_port +"-"+ text_to_forward;
														
														System.out.println("I SENT IT TO" + next_hop);
														synchronized(router_packet_queue) {
															router_packet_queue.offer(packet_added_in_queue);   
													}
												  }
												}
											}
										}
									}
								}
								System.exit(0);
							}
							else
								System.out.println("I AM THE RECIPIENT, I WILL STORE THE MESSAGE IN MY MEMORY. No NEED To forward it");
								System.exit(0);
							ftp_queue.remove(); 
						}
					}
						
				try {
					Thread.sleep(20000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	public Router_configuration() {
		
		//getting the container and setting the layout manager 
		content = getContentPane(); 
		content.setLayout(new FlowLayout());
		
		//instantiating the different components of the GUI 
		router_id_label = new JLabel("Enter the router ID:                                                     ");
		version_protocol_number_label = new JLabel("Enter the protocol version number:                       "); 
		update_interval_label = new JLabel("Enter the value of Update Interval:                         ");
		alive_interval_label = new JLabel("Enter the value of the Alive Interval:                       ");
		link_cost1_label = new JLabel("Enter the cost for Link 1:                                          ");
		link_cost2_label = new JLabel("Enter the cost for Link 2:                                          ");
		lsrp_id_label = new JLabel("Enter the sLSRP ID:                                                    "); 
		dead_label = new JLabel("Enter the value of the dead interval:                       ");
		neigbor_label = new JLabel("IDs of the neigbors (separated with \"-\"):              ");
		number_routers_label = new JLabel("Enter the Total number of routers:                          "); 
		
		version_protocol_number_textField = new JTextField(16); 
		router_id_textField = new JTextField(16); 
		update_interval_textField = new JTextField(16); 
		alive_interval_textField = new JTextField(16); 
		link_cost1_textField = new JTextField(16);
		link_cost2_textField = new JTextField(16); 
		lsrp_id_textField = new JTextField(16);
		dead_textField = new JTextField(16);
		neigbor_textField = new JTextField(16);
		number_of_routers_textField = new JTextField(16); 
		 
		ok_button = new JButton("               OK               "); 
		
		//Instantiating the different panels where some components will be added 
		version_protocol_number_panel = new JPanel(); 
		router_id_panel = new JPanel(); 
		update_interval_panel = new JPanel(); 
		alive_interval_panel = new JPanel(); 
		link_cost1_panel = new JPanel(); 
		link_cost2_panel = new JPanel(); 
		lsrp_id_panel = new JPanel();
		dead_panel = new JPanel();
		neigbor_panel = new JPanel(); 
		number_of_routers_panel = new JPanel(); 
		
		//Instantiating the Menu bar 
		menu_bar = new JMenuBar(); 
		
		//Instantiating the Menu items of the menu bar 
		router_parameters_menu = new JMenu("Configuration");
		help_menu = new JMenu("Help!"); 
		
		//Add Menus to our menu bar
		menu_bar.add(router_parameters_menu); 
		menu_bar.add(help_menu); 
		
		//Setting the Layout of the panels
		version_protocol_number_panel.setLayout(new BoxLayout(version_protocol_number_panel, BoxLayout.X_AXIS));
		router_id_panel.setLayout(new BoxLayout(router_id_panel, BoxLayout.X_AXIS));
		update_interval_panel.setLayout(new BoxLayout(update_interval_panel, BoxLayout.X_AXIS));
		alive_interval_panel.setLayout(new BoxLayout(alive_interval_panel, BoxLayout.X_AXIS));
		link_cost1_panel.setLayout(new BoxLayout(link_cost1_panel, BoxLayout.X_AXIS));
		link_cost2_panel.setLayout(new BoxLayout(link_cost2_panel, BoxLayout.X_AXIS));
		lsrp_id_panel.setLayout(new BoxLayout(lsrp_id_panel, BoxLayout.X_AXIS));
		dead_panel.setLayout(new BoxLayout(dead_panel, BoxLayout.X_AXIS));
		neigbor_panel.setLayout(new BoxLayout(neigbor_panel, BoxLayout.X_AXIS));
		number_of_routers_panel.setLayout(new BoxLayout(number_of_routers_panel, BoxLayout.X_AXIS));
		
		//Adding the different elements in their corresponding panels 
		version_protocol_number_panel.add(version_protocol_number_label); 
		version_protocol_number_panel.add(version_protocol_number_textField);
		router_id_panel.add(router_id_label); 
		router_id_panel.add(router_id_textField);
		alive_interval_panel.add(alive_interval_label);
		alive_interval_panel.add(alive_interval_textField); 
		update_interval_panel.add(update_interval_label); 
		update_interval_panel.add(update_interval_textField);
		link_cost1_panel.add(link_cost1_label); 
		link_cost1_panel.add(link_cost1_textField);
		link_cost2_panel.add(link_cost2_label); 
		link_cost2_panel.add(link_cost2_textField);
		lsrp_id_panel.add(lsrp_id_label); 
		lsrp_id_panel.add(lsrp_id_textField);
		dead_panel.add(dead_label);
		dead_panel.add(dead_textField);
		neigbor_panel.add(neigbor_label); 
		neigbor_panel.add(neigbor_textField); 
		number_of_routers_panel.add(number_routers_label); 
		number_of_routers_panel.add(number_of_routers_textField); 
		
		
		//Adding the GUI components into the in the interface
		content.add(router_id_panel);
		content.add(version_protocol_number_panel);
		//content.add(lsrp_id_panel); 
		//content.add(link_cost1_panel);
		//content.add(link_cost2_panel);
		content.add(update_interval_panel); 
		content.add(alive_interval_panel); 
		content.add(dead_panel);
		content.add(neigbor_panel); 
		content.add(number_of_routers_panel);
		content.add(ok_button); 
		
		//instantiate the event handler 
		ButtonHandler bh = new ButtonHandler();
		
		//adding the action and Menu Listener to the different buttons so that they can perform the required actions 
		router_parameters_menu.addMenuListener(bh); 
		help_menu.addMenuListener(bh);
		
		ok_button.addActionListener(bh); 
	}
	
	//This class handles the different actions that will be performed when a button is clicked. 
	public class ButtonHandler implements ActionListener, MenuListener {

		@Override
		public void actionPerformed(ActionEvent ae) {
			// TODO Auto-generated method stub
			
			//Action performed when the ok_button is pressed. 
			if (ae.getSource() == ok_button) {
				//Disable all the TextFiels 
				router_id_textField.setEnabled(false);
				version_protocol_number_textField.setEnabled(false);
				update_interval_textField.setEnabled(false); 
				alive_interval_textField.setEnabled(false);
				dead_textField.setEnabled(false);
				number_of_routers_textField.setEnabled(false);
				
				router_id = router_id_textField.getText(); 
				protocol_version_number = version_protocol_number_textField.getText(); 
				lsrp_id = lsrp_id_textField.getText();  
				cost1 = link_cost1_textField.getText(); 
				cost2 = link_cost2_textField.getText(); 
				update_interval = update_interval_textField.getText(); 
				alive_interval = alive_interval_textField.getText(); 
				dead_interval = dead_textField.getText(); 
				number_of_routers = Integer.parseInt(number_of_routers_textField.getText());
				routers_attached = neigbor_textField.getText(); 
				
				//create an array to store the IDs of the attached routers
				attached_routers_id = routers_attached.split("-");  
				attached_routers_id_info = new LinkedList<String>(); 
				link_state_database = new LinkedList<String>();
				position = 0; 
				
				//Create the Socket to receive
				class Router_receive_socket implements Runnable {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						router_receive_port = Integer.parseInt(router_id+router_id+router_id+router_id+router_id);  
						int max_packet_length = 1500; 
						
						String received_packet, received_message; 
						
						try 
						{
							ServerSocket receive_socket = new ServerSocket(router_receive_port);
							while(true) {
								Socket socket = receive_socket.accept(); 
								InputStream input_stream = socket.getInputStream(); 
								byte [] received_pkt = new byte[max_packet_length]; 
								input_stream.read(received_pkt);
								received_message = new String(received_pkt); 
								
								received_message = received_message.substring(0, received_message.lastIndexOf("-")+1); 
								byte [] received_pkt1 = received_message.getBytes(); 
								
								if (received_message.length() > 5) {
									
									//Computing the checksum of the received packet:
									Checksum checksum1 = new CRC32(); 
									checksum1.update(received_pkt1, 0, received_pkt1.length); 
									
									long checksum_value1 = checksum1.getValue();
													
									String received_pkt_field [] = received_message.split("-"); 
									String pkt_type = received_pkt_field[5];
									
									if (pkt_type.contains("resolver") || pkt_type.contains("Neighbors")) {
										System.out.println("i received " + received_message);
										synchronized (router_resolver_queue) {
											router_resolver_queue.offer(received_message); 
										}
									}
									else if (pkt_type.contains("Alive_message")) {
										synchronized (router_alive_queue) {
											router_alive_queue.offer(received_message); 
										}
									}
									
									else if (pkt_type.contains("link_cost_message")) {
										synchronized (router_link_cost_queue) {
											router_link_cost_queue.offer(received_message); 
										}
									}
									
									else if (pkt_type.contains("lsa_")) {
										synchronized (lsa_queue) {
											lsa_queue.offer(received_message); 
										}
									}
									else if (pkt_type.contains("ftp")) {
										synchronized (lsa_queue) {
											ftp_queue.offer(received_message); 
										}
									}
								}
		
							}
						}
						catch (Exception err)
						{
							System.err.println(err); 
						}
						
					}
				}
				new Thread(new Router_receive_socket()).start();
				
				
				//Create the socket to send packets
				class Router_send_socket implements Runnable {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						try { 
								while (true) {
									synchronized(router_packet_queue) {
										
										if (router_packet_queue.size() > 0) {
											String packet_out = router_packet_queue.peek();
											String just_for_checking = packet_out; 
											
											String [] packet_field = packet_out.split("-"); 
											String dest_ip = packet_field[2]; 
											int  dest_port = Integer.parseInt(packet_field[3]); 
											
											//create the socket to send the packet out 
											Socket router_socket_send = new Socket (dest_ip, dest_port); 
											OutputStream o = router_socket_send.getOutputStream(); 
											
											byte [] pkt = packet_out.getBytes(); 
											
											//Computing the checksum 
											Checksum checksum = new CRC32(); 
											checksum.update(pkt, 0, pkt.length);
											
											long checksum_value = checksum.getValue(); 
											
											//Adding the CRC to the packet
											packet_out = packet_out+checksum_value+"-";
											pkt = packet_out.getBytes();
											
											//Generating the random number of Pe for the Packet error simulation 
											double Pe = Math.random(); 
											if (Pe >= 0.8 && packet_out.contains("lsa_")) {
												//introduce an error in the packet to be transmitted:
											 
												packet_out = packet_out+"a-";
												pkt = packet_out.getBytes();
											}
												
											//Generating the Probability for network congestion
											double Pc = Math.random(); 
											if (packet_out.contains("lsa_")) {
												if (Pc >= 2) {
													  
												}
												else {
													o.write(pkt);
												    router_socket_send.close();
												}
											}
											
											if (!packet_out.contains("lsa_")) { 
												o.write(pkt); 
											    router_socket_send.close(); 
											}
											router_packet_queue.remove();
									} 
								}
									Thread.sleep(100); 
								}
							
						}
						catch (Exception err) {
							System.err.println(err);  
						}
					}
				}
				
				new Thread(new Router_send_socket()).start();
				
				//Calling the Name Resolver Method
				new Thread(new Resolver_thread()).start();
				
				new Thread(new Link_Cost_Messages()).start();
				
				new Thread(new Alive_Message()).start();
				
				new Thread(new Lsa_message()).start();
				
				//new Thread(new Shortest_path_send()).start();
				
			}
		}

		@Override
		public void menuSelected(MenuEvent me) {
			// TODO Auto-generated method stub
			if (me.getSource() == help_menu) {
				
				content.removeAll(); 
				
				setSize(600, 400);
			}
			
			if (me.getSource() == router_parameters_menu) {
			
				content.removeAll(); 
				
				content.add(router_id_panel);
				content.add(version_protocol_number_panel);
				content.add(update_interval_panel); 
				content.add(alive_interval_panel); 
				content.add(dead_panel);
				content.add(neigbor_panel); 
				content.add(number_of_routers_panel);
				content.add(ok_button); 
				
				setSize(600, 275);
			}
		}

		@Override
		public void menuDeselected(MenuEvent me) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void menuCanceled(MenuEvent me) {
			// TODO Auto-generated method stub
			
		}
	}
	
	public static void main(String[] args) {
		
		// TODO Auto-generated method stub
		Router_configuration router_configuration = new Router_configuration();
		router_configuration.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		router_configuration.setSize(600, 275); 
		router_configuration.setVisible(true);
		router_configuration.setTitle("Router Configuration"); 
		router_configuration.setResizable(false); 
		
		router_configuration.setJMenuBar(menu_bar);
		
	}

}
