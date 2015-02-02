package cat.urv.imas.agent.communication.ambcontractnet;

import cat.urv.imas.map.Cell;
import jade.core.AID;

/**
 * Created by Philipp Oliver on 29/1/15.
 */
public class ContractBid implements java.io.Serializable {
    private int auctionID;
    private int value;

    public ContractBid(int auctionID, int value) {
        this.auctionID = auctionID;
        this.value = value;
    }

    public int getAuctionID() {
        return auctionID;
    }

    public int getValue() {
        return value;
    }
}
