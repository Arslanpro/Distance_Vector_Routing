package my_protocol;

import framework.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @version 12-03-2019
 *
 * Copyright University of Twente, 2013-2019
 *
 **************************************************************************
 *                            Copyright notice                            *
 *                                                                        *
 *             This file may ONLY be distributed UNMODIFIED.              *
 * In particular, a correct solution to the challenge must NOT be posted  *
 * in public places, to preserve the learning effect for future students. *
 **************************************************************************
 */
public class MyRoutingProtocol1 implements IRoutingProtocol {
    private LinkLayer linkLayer;

    // You can use this data structure to store your routing table.
    private HashMap<Integer, MyRoute> myRoutingTable = new HashMap<>();
    private MyRoute self = new MyRoute();
    private List<Integer> neighBourList = new ArrayList<Integer>();
    private int myAddress;
    private Set<Integer> currentNeighbours;
    private Iterator<Integer> it;
    private Iterator<Integer> it2;
    private Iterator<Integer> it3;

    private int perUpdate = 0;
    
    @Override
    public void init(LinkLayer linkLayer) {
        this.linkLayer = linkLayer;
        self.cost = 0;
        // Get the address of this node
        myAddress = this.linkLayer.getOwnAddress();
        self.nextHop = myAddress;
        this.myRoutingTable.put(myAddress, self);
        currentNeighbours = new HashSet<Integer>();
    }


    @Override
    public void tick(PacketWithLinkCost[] packetsWithLinkCosts) {
    	perUpdate++;
       // System.out.println("tick; received " + packetsWithLinkCosts.length + " packets");

    	Set<Integer> localNeighbours = new HashSet<Integer>();
   	
        //Add the neighbours of the nodes, identified by their packets.
        for (PacketWithLinkCost e : packetsWithLinkCosts) {
        	int neighbourSource = e.getPacket().getSourceAddress();
        	currentNeighbours.add(neighbourSource);
        	localNeighbours.add(neighbourSource);

        }
    	// Also check if any neighbours have disappeared.
        if (!localNeighbours.containsAll(currentNeighbours)) {
            // Something must be done if the amount of neighbours suddenly shrinks.
        	it = this.currentNeighbours.iterator();
        	while(it.hasNext()) {
        		int currentHop = it.next();
            	if (!localNeighbours.contains(currentHop)) {
                    Set<Integer> tempKeys = this.myRoutingTable.keySet();
            		for (int f : tempKeys) {
            			if (this.myRoutingTable.get(f).nextHop == currentHop) {
            				this.myRoutingTable.get(f).cost = 100000; //TODO remove magic number
            			}
            		}
            		it.remove();
            	}
        	}
        }
    	
        
        // If no packets have been received, send a broadcast to everyone.
        if (packetsWithLinkCosts.length == 0) {
        	
            // The data table must contain the known data of the routing table of this node.
            // This is done by placing the available data of the routing table inside the data table.
            DataTable dt = new DataTable(6);
            Set<Integer> currentKeys = this.myRoutingTable.keySet();
            it2 = currentKeys.iterator();
            while (it2.hasNext()) {
            	int currentKey = it2.next();
            	dt.set(0, currentKey - 1, currentKey);
            	dt.set(1, currentKey - 1, this.myRoutingTable.get(currentKey).cost);
            	// dt.set(2, currentKey - 1, this.myRoutingTable.get(currentKey).nextHop);
            }
            Packet pkt = new Packet(myAddress, 0, dt);
            this.linkLayer.transmit(pkt);
        } else {
        	
        	// first process the incoming packets; loop over them:
            for (int i = 0; i < packetsWithLinkCosts.length; i++) {
                Packet packet = packetsWithLinkCosts[i].getPacket();
                int neighbour = packet.getSourceAddress();             // from whom is the packet?
                int linkcost = packetsWithLinkCosts[i].getLinkCost();  // what's the link cost from/to this neighbour?
                DataTable dtOut = packet.getDataTable();                  // other data contained in the packet
                
                    // System.out.printf("received packet from %d with %d rows and %d columns of data%n", neighbour, dt.getNRows(), dt.getNColumns());
                    //Saving the data into arrays. TODO probably can be done more efficient.
                    Integer[] currentDestinations = dtOut.getRow(0);
                    Integer[] destinationCosts = dtOut.getRow(1);
                   // Integer[] destinationHops = dt.getRow(2);
                    
                    //Check for the current destinations if it is already known and if so then if it is cheaper.
                    for (int L = 0; L < currentDestinations.length; L++) {
                    	if (currentDestinations[L] != 0 && this.myRoutingTable.containsKey(currentDestinations[L])) {
                    		MyRoute r = this.myRoutingTable.get(currentDestinations[L]);
                    		if (r.cost > destinationCosts[L] + linkcost) {
                    			r.cost = destinationCosts[L] + linkcost;
                    			r.nextHop = neighbour;
                    		}
                    	} else if (currentDestinations[L] != 0){ // If it is not known, add a new map entry.
                            MyRoute newDest = new MyRoute();
                            newDest.nextHop = neighbour;
                            newDest.cost = destinationCosts[L] + linkcost;
                    		this.myRoutingTable.put(currentDestinations[L], newDest);
                    	}
                    }
                    // The data table must contain the known data of the routing table of this node.
                    // This is done by placing the available data of the routing table inside the data table.
                    dtOut = new DataTable(6);
                    Set<Integer> currentKeys = this.myRoutingTable.keySet();
                	/*
                    it3 = currentKeys.iterator();
                	while(it3.hasNext()) {
                    	int tempKey = it3.next();
                    	dtOut.set(0, tempKey - 1, tempKey);
                    	//If the neighbour is also the nexthop, increase the cost.
                    	if (this.myRoutingTable.get(tempKey).nextHop == neighbour) {
                        	dtOut.set(1, tempKey - 1, 100000);
                    	} else {
                        	dtOut.set(1, tempKey - 1, this.myRoutingTable.get(tempKey).cost);
                    	}
                	}*/
                    for (int e : currentKeys) {
                    	dtOut.set(0, e - 1, e);
                    	//If the neighbour is also the nexthop, increase the cost.
                    	if (this.myRoutingTable.get(e).nextHop == neighbour) {
                        	dtOut.set(1, e - 1, 100000);
                    	} else {
                        	dtOut.set(1, e - 1, this.myRoutingTable.get(e).cost);
                    	}
                    	// dt.set(2, e - 1, this.myRoutingTable.get(e).nextHop);
                    	
                    }
                    
                    Packet pkt = new Packet(myAddress, neighbour, dtOut);
                    this.linkLayer.transmit(pkt);
            }        

        }


    }

    public Map<Integer, Integer> getForwardingTable() {
        // This code extracts from your routing table the forwarding table.
        // The result of this method is send to the server to validate and score your protocol.

        // <Destination, NextHop>
        HashMap<Integer, Integer> ft = new HashMap<>();

        for (Map.Entry<Integer, MyRoute> entry : myRoutingTable.entrySet()) {
            ft.put(entry.getKey(), entry.getValue().nextHop);
        }

        return ft;
    }
}