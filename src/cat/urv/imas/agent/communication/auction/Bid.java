package cat.urv.imas.agent.communication.auction;

import cat.urv.imas.map.Cell;
import jade.core.AID;

/**
 * Created by Philipp Oliver on 29/1/15.
 */
public class Bid implements java.io.Serializable {
    private int auctionID;
    private Float value;

    public Bid(int auctionID, Float value) {
        this.auctionID = auctionID;
        this.value = value;
    }

    public int getAuctionID() {
        return auctionID;
    }

    public Float getValue() {
        return value;
    }
}
