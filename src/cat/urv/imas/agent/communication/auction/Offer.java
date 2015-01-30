package cat.urv.imas.agent.communication.auction;

import cat.urv.imas.agent.communication.util.MessageCreatorUtil;
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
    private Item item;

    public Offer(AID auctioneer, int auctionID, Item item) {
        this.auctionID = auctionID;
        this.auctioneer = auctioneer;
        this.item = item;
    }

    public Item getItem() {
        return item;
    }

    public void reply(Agent sender, Float bidValue){
        System.out.println("########## Auction Attendee Replies ##########");
        Bid bid = new Bid(auctionID, bidValue);
        ACLMessage bidMsg = MessageCreatorUtil.createProposeMessage(auctioneer, MessageContent.AMBULANCE_AUCTION_BID, bid);
        sender.send(bidMsg);
    }
}
