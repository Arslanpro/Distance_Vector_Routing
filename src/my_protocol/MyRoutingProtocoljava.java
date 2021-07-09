package my_protocol;

import framework.*;

import java.util.ArrayList;
import java.util.HashMap;
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
public class MyRoutingProtocoljava implements IRoutingProtocol { 
    private LinkLayer linkLayer;  
    private HashMap<Integer, MyRoute> myRoutingTable = new HashMap<>(); 
    private MyRoute chosen = new MyRoute(); 
    private List<Integer> neighBour = new ArrayList<>(); 
    
    @Override
    public void init(LinkLayer linkLayer) {
        this.linkLayer = linkLayer;
       
    }
    
    @Override
    public void tick(PacketWithLinkCost[] packetsWithLinkCosts) {
    	int myAddress = this.linkLayer.getOwnAddress();
    	chosen.nextHop = myAddress;
        this.myRoutingTable.put(myAddress, chosen);
        List<Integer> currentNeighbours = new ArrayList<>();
        for (PacketWithLinkCost e : packetsWithLinkCosts) {
        	currentNeighbours.add(e.getPacket().getSourceAddress());
        }
        
        if (packetsWithLinkCosts.length < this.neighBour.size()) {
            Set<Integer> tempKeys = this.myRoutingTable.keySet();
        	for (int e : this.neighBour) {
        		if (!(currentNeighbours.contains(e))) {
        			for (int j : tempKeys) {
        				if (this.myRoutingTable.get(j).nextHop == e) {
        					this.myRoutingTable.get(j).cost = 999; 
        				}
        			}
        		}
        	}
        }
        
        System.out.println("tick; received " + packetsWithLinkCosts.length + " packets");
        int i;
        
        // first process the incoming packets; loop over them:
        for (i = 0; i < packetsWithLinkCosts.length; i++) {
            Packet packet = packetsWithLinkCosts[i].getPacket();
            int neighbour = packet.getSourceAddress();             // from whom is the packet?
            int linkcost = packetsWithLinkCosts[i].getLinkCost();  // what's the link cost from/to this neighbour?
            DataTable dt = packet.getDataTable();                  // other data contained in the packet
            System.out.printf("received packet from %d with %d rows and %d columns of data%n", neighbour, dt.getNRows(), dt.getNColumns());
            
           if (!this.neighBour.contains(neighbour)) {
            	this.neighBour.add(neighbour);
            } 
            
            Integer[] actualDestination = dt.getRow(0);
            Integer[] destinationCost = dt.getRow(2);
            
            for (int A = 0; A < actualDestination.length; A++) {
            	if ( this.myRoutingTable.containsKey(actualDestination[A]) && !(actualDestination[A] == 0)) {
            		MyRoute r = this.myRoutingTable.get(actualDestination[A]);
            		if (r.cost > destinationCost[A] + linkcost) {
            			r.nextHop = neighbour;
            			r.cost = destinationCost[A] + linkcost;
            		}
            	} else if (!(actualDestination[A] == 0)){
                    MyRoute newDest = new MyRoute();
                    newDest.nextHop = neighbour;
                    newDest.cost = destinationCost[A] + linkcost;
            		this.myRoutingTable.put(actualDestination[A], newDest);
            	}
            }
        }
        
        DataTable dt = new DataTable(7);   
        Set<Integer> actualKey = this.myRoutingTable.keySet(); 
  
        for (int e : actualKey) {
        	dt.set(0, e - 1 , e);
        	dt.set(1, e - 1, this.myRoutingTable.get(e).nextHop);
        	dt.set(2, e - 1, this.myRoutingTable.get(e).cost);
        }
        Packet pkt = new Packet(myAddress, 0, dt);
        this.linkLayer.transmit(pkt);

    }

    public Map<Integer, Integer> getForwardingTable() {
       
        // <Destination, NextHop>
        HashMap<Integer, Integer> ft = new HashMap<>();

        for (Map.Entry<Integer, MyRoute> entry : myRoutingTable.entrySet()) {
            ft.put(entry.getKey(), entry.getValue().nextHop);
        }
        return ft;
    }
}