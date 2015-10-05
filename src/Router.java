import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 
 * @author satyajeet
 *
 */
class DistanceVector implements Serializable {
	HashMap<Integer, Integer> map;
	
	DistanceVector() {
		map = new HashMap<Integer, Integer>();
	}
	
	/**
	 * sets linkcosts map for router
	 * @param lc
	 */
	DistanceVector(HashMap<Integer,Integer> map) {
		this.map = map;
	}
	
	public String toString() {
		return map.toString();
	}
}

/**
 * Class encapsulating the functionality of a RIP executing router
 * 
 * @author satyajeet
 */
public class Router extends Thread {
	
	final InetAddress localHost;
	final InetAddress IP;
	final int port;
	private ServerSocket serverSocket;
	private DistanceVector dv;
	private HashMap<Integer, DistanceVector> nVectors;
	private ArrayList<Integer> neighbors;
	
	/**
	 * Constructor for the class
	 * 
	 * @param port - port number of this router
	 * @throws UnknownHostException
	 */
	Router(int port) throws UnknownHostException {
		localHost = InetAddress.getLocalHost();
		IP = InetAddress.getLocalHost();
		this.port = port;
		
		dv = new DistanceVector();
		nVectors = new HashMap<Integer, DistanceVector>();
		neighbors = new ArrayList<Integer>();
		//dv.map.put(port, 0);
	}
	
	void startServer() {
		System.out.println("Starting \n" + dv);
		DistanceVector receivedVector;
		ObjectInputStream is;
		try {
			serverSocket = new ServerSocket(port);
			while (true) {
				Socket clientSocket = serverSocket.accept();				 
				is = new ObjectInputStream(clientSocket.getInputStream());
				int clientPort = is.readInt();
				receivedVector =  (DistanceVector) is.readObject();
				is.close();
				clientSocket.close();
				updateForwardingTable(clientPort, receivedVector);
			}
		} catch (Exception e) {
			//e.printStackTrace();
		}
	}
	
	/**
	 * starts the router. If a router wishes to connect, its added to 
	 * the neighbors list
	 * 
	 * @throws IOException
	 */
	public void run() {
		try {
			while ( true ) {
				for ( Integer i : neighbors ) {
					try {
						//connect to that router
						Socket clientSocket = new Socket(localHost, i);
						//broadcast port and dv
						ObjectOutputStream oos = 
								new ObjectOutputStream(clientSocket.getOutputStream());
						oos.writeInt(this.port);
						oos.writeObject(dv);
						oos.close();
						clientSocket.close();
					} catch (Exception e) {
						
					}
				}
				Thread.sleep(3000);
			}
		} catch (Exception e) {
			//e.printStackTrace();
		}
	}
		
	/**
	 * Method to update forwarding table of router
	 *  
	 */
	void updateForwardingTable(int router, DistanceVector receivedVector) {	
		boolean broadcast = false;
		nVectors.put(router, receivedVector);
		
		//new node reachable from neighbor
		for ( Map.Entry<Integer, Integer> n : receivedVector.map.entrySet() ) {
			if ( dv.map.get(n.getKey()) == null ) {
				int costToNeighbor = dv.map.get(router);
				int neighborToDest = n.getValue();
				dv.map.put(router, costToNeighbor + neighborToDest);
			}				
		}
		
		int min;
		//for all nodes this current node is aware of 
		for ( Map.Entry<Integer, Integer> y : dv.map.entrySet() ) {
			min = 0;
			//for all neighbors of this current node
			for ( Map.Entry<Integer, DistanceVector> c : nVectors.entrySet() ) {
				int costToNeighbor = dv.map.get(c.getKey());
				int neighborToDest;
				if ( c.getValue().map.get(y.getKey()) == null ) 
					neighborToDest = 9999;
				else 
					neighborToDest = c.getValue().map.get(y.getKey());
				int temp = costToNeighbor + neighborToDest;
				if ( temp < min ) {
					dv.map.put(y.getKey(), temp);
					min = temp;
					broadcast = true;
					//next hop, number of hops
				}
			}
		}

		System.out.println(dv);
		
		if (broadcast) {
			System.out.println("Table updated, forcing broadcast");
			run();
		}
	}

	void setDistanceVector(DistanceVector dv) {
		this.dv = dv;		
		for ( Map.Entry<Integer, Integer> i : dv.map.entrySet() )
			neighbors.add(i.getKey());
		dv.map.put(port, 0);
	}
	
	/**
	 * Main thread for router
	 * 
	 * @param args
	 */
	public static void main(String args[]) {	
		try {
			
			HashMap<Integer, Integer> lc0 = new HashMap<Integer, Integer>();
			lc0.put(50001, 1);
			lc0.put(50002, 3);			
			
			HashMap<Integer, Integer> lc1 = new HashMap<Integer, Integer>();
			lc1.put(50000, 1);
			lc1.put(50002, 2);
			
			HashMap<Integer, Integer> lc2 = new HashMap<Integer, Integer>();
			lc2.put(50000, 3);
			lc2.put(50001, 2);
			
			Router a = new Router(50000);
			a.setDistanceVector(new DistanceVector(lc0));			
			a.start();
			a.startServer();
			
			/*Thread.sleep(2000);
			
			Router b = new Router(50001);
			b.setDistanceVector(new DistanceVector(lc1));
			b.start();
			b.startServer();
			
			Thread.sleep(2000);
			
			Router c = new Router(50002);
			c.setDistanceVector(new DistanceVector(lc2));			
			c.start();
			c.startServer();
			
			Thread.sleep(2000);
			*/
			
		} catch (Exception e) {			
			e.printStackTrace();
		}
	}
}
