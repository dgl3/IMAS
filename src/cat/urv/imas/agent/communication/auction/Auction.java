package cat.urv.imas.agent.communication.auction;

import jade.core.AID;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This class keeps track of the bidders and their bids.
 * Everyone bids and bids only once.
 * The winner can only be queried when all bidder responded.
 *
 * Created by Philipp Oliver on 29/1/15.
 */
public class Auction {
    private int id;
    private Item item;
    private AID seller;
    private HashSet<AID> outstandingBidders;
    private HashMap<AID, Float> bids;
    private AID winner;


    public Auction(int id, AID seller, Item item, HashSet<AID> outstandingBidders) {
        this.id = id;
        this.item = item;
        this.outstandingBidders = outstandingBidders;
        this.bids = new HashMap<>();
        this.seller = seller;
    }

    public void takeBid(AID aid, Float bid){
        if( outstandingBidders.contains(aid) ){
            outstandingBidders.remove(aid);
            bids.put(aid, bid);
        }else{
            throw new IllegalStateException("Received response from non-participant. Or, participant already replied earlier.");
        }
    }

    public Item getItem() {
        return item;
    }

    public HashSet<AID> getOutstandingBidders() {
        return outstandingBidders;
    }

    public boolean readyForEvaluation(){
        return outstandingBidders.isEmpty();
    }

    public int getID(){
        return id;
    }

    public AID getWinner(){
        if ( !readyForEvaluation() ){
            throw new IllegalStateException("Winner can not be queried. Not all bidders have replied.");
        }

        if( this.winner == null ) {
            Float highestBid = Float.MAX_VALUE;
            for (AID bidder : bids.keySet()) {

                Float bid = bids.get(bidder);
                if (bid < highestBid) {
                    highestBid = bid;
                    this.winner = bidder;
                }
            }
        }

        return this.winner;
    }

    public AID getSeller() {
        return seller;
    }
}
