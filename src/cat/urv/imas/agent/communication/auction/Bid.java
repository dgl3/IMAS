package cat.urv.imas.agent.communication.auction;

import cat.urv.imas.map.Cell;

/**
 * Created by Philipp Oliver on 29/1/15.
 */
public class Bid implements java.io.Serializable {
    private int auctionID;
    private Float value;
    private Cell bidderLocation;

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

    public Cell getBidderLocation() {
        return bidderLocation;
    }
}
