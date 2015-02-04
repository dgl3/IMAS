package cat.urv.imas.agent.communication.ambcontractnet;

import cat.urv.imas.map.Cell;
import jade.core.AID;

/**
 * Created by Philipp Oliver on 29/1/15.
 */
public class ContractBid implements java.io.Serializable {
    private int auctionID;
    private int people;
    private int distance;

    public ContractBid(int auctionID, int value, int distance) {
        this.auctionID = auctionID;
        this.people = value;
        this.distance = distance;
    }

    public int getAuctionID() {
        return auctionID;
    }

    public int getPeopleValue() {
        return people;
    }
    
    public int getDistValue(){
        return distance;
    }
}
