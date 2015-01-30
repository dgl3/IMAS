package cat.urv.imas.agent.communication.auction;

import cat.urv.imas.agent.communication.util.MessageCreator;
import cat.urv.imas.onthology.MessageContent;
import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import java.util.*;

/**
 * Class to manage auctions.
 * Allows only one auction to be running at a time.
 * New auction will be stored in a queue and executed in order.
 *
 * Created by Philipp Oliver on 29/1/15.
 */
public class AuctionManager {
    private Agent auctioneer;
    private Integer auctionIds;
    private Queue<Auction> pendingAuctions;
    private Auction currentAuction;
    private boolean auctionInProgress;

    public AuctionManager(Agent auctioneer){
        auctionIds = 0;
        this.auctioneer = auctioneer;
        this.pendingAuctions = new LinkedList<>();
        this.auctionInProgress = false;
    }

    public void setupNewAuction(AID seller, Item item, HashSet<AID> participants){
        System.out.println("########## Setup Auction ##########");
        auctionIds++;
        Auction auction = new Auction(auctionIds, seller, item, participants);
        pendingAuctions.add(auction);

        if( !auctionInProgress ){
            startNextAuction();
        }
    }

    private void startNextAuction(){
        currentAuction = pendingAuctions.poll();
        startAuction(currentAuction);
        auctionInProgress = true;
    }

    private void startAuction(Auction currentAuction) {
        System.out.println("########## Sending Auction Letters ##########");
        Set<AID> participants = currentAuction.getOutstandingBidders();
        String messageType = MessageContent.AMBULANCE_AUCTION;
        Offer offer = new Offer(auctioneer.getAID(), currentAuction.getID(), currentAuction.getItem());

        ACLMessage bidRequestMsg = MessageCreator.createRequest(participants, messageType, offer);

        auctioneer.send(bidRequestMsg);
    }

    public void takeBid(AID sender, Bid bid) {
        System.out.println("########## Received Bid ##########");
        if( bid.getAuctionID() == currentAuction.getID() ) {
            currentAuction.takeBid(sender, bid.getValue());

            if( currentAuction.readyForEvaluation() ){
                AID winner = currentAuction.getWinner();
                notifyWinner(winner);
                notifySeller(winner);
            }
        }else{
            throw new IllegalStateException("Received Bid for non-existing, or pending auction.");
        }

    }

    private void notifySeller(AID winner) {
        ACLMessage msg = MessageCreator.createInform(currentAuction.getSeller(), MessageContent.AMBULANCE_AUCTION, winner);
        auctioneer.send(msg);
    }

    private void notifyWinner(AID winner) {
        System.out.println("########### ANNOUNCING WINNER #############");

        String messageType = MessageContent.AMBULANCE_AUCTION;
        Offer offer = new Offer(auctioneer.getAID(), currentAuction.getID(), currentAuction.getItem());

        ACLMessage bidRequestMsg = MessageCreator.createMessage(ACLMessage.ACCEPT_PROPOSAL, winner, messageType, offer);

        auctioneer.send(bidRequestMsg);
    }

    public void confirmAction(AID sender, Offer offer) {

        if( currentAuction.readyForEvaluation()
            && currentAuction.getWinner().equals(sender)
            && offer.getAuctionID() == currentAuction.getID() )
        {
            System.out.println("########### ANNOUNCING COMPLETED #############");
            currentAuction = null;
            auctionInProgress = false;

            if( !pendingAuctions.isEmpty() ) {
                startNextAuction();
            }
        }else{
            throw new IllegalStateException("Received illegal auction confirmation from agent aid " + sender + " for auction id " + offer.getAuctionID() + ", current auction id is " + currentAuction.getID() + ", winner is "+currentAuction.getWinner() + " and auction is ready for evaluation: " + currentAuction.readyForEvaluation());
        }
    }
}
