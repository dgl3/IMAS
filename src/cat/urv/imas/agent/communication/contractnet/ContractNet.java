package cat.urv.imas.agent.communication.contractnet;

import cat.urv.imas.map.Cell;
import jade.core.AID;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public static final int WINNER = 0;
    public static final int LOOSER = 1;


    public ContractNet(int id, AID seller, Cell item, Collection<AID> outstandingBidders) {
        this.id = id;
        this.item = item;
        this.seller = seller;
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

    public Map<Integer,List<AID>> getWinner(){
        if ( !readyForEvaluation() ){
            throw new IllegalStateException("Winner can not be queried. Not all bidders have replied.");
        }

        if( winner == null ) {
            int highestBid = Integer.MAX_VALUE;
            for (AID bidder : bids.keySet()){
                int bid = bids.get(bidder);
                if ((bid > -1)&&(bid < highestBid)){
                    highestBid = bid;
                    winner = bidder;
                }
            }
        }
        
        Map<Integer,List<AID>> list = new HashMap<>();
        List<AID> winnerList = new ArrayList<>();
        List<AID> looserList = new ArrayList<>();
        if(winner!=null){
            winnerList.add(winner);
        }else{
            // No winner
        }
        list.put(WINNER, winnerList);
        list.put(LOOSER, looserList);
        for (AID bidder : bids.keySet()){
            if(bidder!=winner){
                looserList.add(bidder);
            }
        }
       
        return list;
    }

    public AID getSeller() {
        return seller;
    }
}
