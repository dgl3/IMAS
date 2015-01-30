package cat.urv.imas.agent.communication.contractnet;

import cat.urv.imas.agent.communication.util.MessageCreator;
import cat.urv.imas.map.Cell;
import cat.urv.imas.onthology.MessageContent;
import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

/**
 * Created by Philipp Oliver on 29/1/15.
 */
public class Offer implements java.io.Serializable {
    private AID auctioneer;
    private int auctionID;
    private Cell cell;

    public Offer(AID auctioneer, int auctionID, Cell cell) {
        this.auctionID = auctionID;
        this.auctioneer = auctioneer;
        this.cell = cell;
    }

    public Cell getCell() {
        return cell;
    }

    public AID getAuctioneer() {
        return auctioneer;
    }

    public void reply(Agent sender, int bidValue){
        System.out.println("########## Auction Attendee Replies ##########");
        Bid bid = new Bid(auctionID, bidValue);
        ACLMessage bidMsg = MessageCreator.createPropose(auctioneer, MessageContent.FIRMEN_CONTRACTNET, bid);
        sender.send(bidMsg);
    }

    public int getAuctionID() {
        return auctionID;
    }
}
