package cat.urv.imas.agent.communication.contractnet;

import cat.urv.imas.map.Cell;
import jade.core.AID;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * This class keeps track of the bidders and their bids.
 * Everyone bids and bids only once.
 * The winner can only be queried when all bidder responded.
 *
 * Created by Philipp Oliver on 29/1/15.
 */
public class ContractNet {
    private int id;
    private Cell item;
    private AID seller;
    private Collection<AID> outstandingBidders;
    private HashMap<AID, Integer> bids;
    private AID winner;


    public ContractNet(int id, Cell item, Collection<AID> outstandingBidders) {
        this.id = id;
        this.item = item;
        this.outstandingBidders = outstandingBidders;
        this.bids = new HashMap<>();
    }

    public void takeBid(AID aid, int bid){
        if( outstandingBidders.contains(aid) ){
            outstandingBidders.remove(aid);
            bids.put(aid, bid);
        }else{
            throw new IllegalStateException("Received response from non-participant. Or, participant already replied earlier.");
        }
    }

    public Cell getItem() {
        return item;
    }

    public Collection<AID> getOutstandingBidders() {
        return outstandingBidders;
    }

    public boolean readyForEvaluation(){
        return outstandingBidders.isEmpty();
    }

    public int getID(){
        return id;
    }

    public List<AID> getWinner(){
        List<AID> winnerList = new ArrayList<>();
        if ( !readyForEvaluation() ){
            throw new IllegalStateException("Winner can not be queried. Not all bidders have replied.");
        }

        if( winner == null ) {
            int highestBid = Integer.MAX_VALUE;
            System.out.println("Int initial value: "+highestBid);
            for (AID bidder : bids.keySet()){
                int bid = bids.get(bidder);
                System.out.println("bid: "+bid);
                if ((bid > -1)&&(bid < highestBid)){
                    System.out.println("BBBBB");
                    highestBid = bid;
                    winner = bidder;
                }
            }
        }
        
        winnerList.add(winner);
        for (AID bidder : bids.keySet()){
            if(bidder!=winner){
                winnerList.add(bidder);
            }
        }

        return winnerList;
    }

    public AID getSeller() {
        return seller;
    }
}
