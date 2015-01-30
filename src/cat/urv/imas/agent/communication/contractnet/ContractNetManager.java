package cat.urv.imas.agent.communication.contractnet;

import cat.urv.imas.agent.communication.util.MessageCreator;
import cat.urv.imas.map.Cell;
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
public class ContractNetManager {
    private Agent contractor;
    private Integer contractNetIds;
    private Queue<ContractNet> pendingContractNets;
    private ContractNet currentContractNet;
    private boolean contractNetInProgress;

    public ContractNetManager(Agent contractor){
        contractNetIds = 0;
        this.contractor = contractor;
        this.pendingContractNets = new LinkedList<>();
        this.contractNetInProgress = false;
    }

    public void setupNewContractNet(AID seller, Cell item, Collection<AID> participants){
        System.out.println("########## Setup Contract Net ##########");
        contractNetIds++;
        ContractNet auction = new ContractNet(contractNetIds, item, participants);
        //ContractNet ney = new ContractNetz
        pendingContractNets.add(auction);

        if( !contractNetInProgress ){
            startNextAuction();
        }
    }

    private void startNextAuction(){
        currentContractNet = pendingContractNets.poll();
        startContractNet(currentContractNet);
        contractNetInProgress = true;
    }

    private void startContractNet(ContractNet currentAuction) {
        System.out.println("########## Sending CFP's ##########");
        Collection<AID> participants = currentAuction.getOutstandingBidders();
        String messageType = MessageContent.FIRMEN_CONTRACTNET;
        Offer offer = new Offer(contractor.getAID(), currentAuction.getID(), currentAuction.getItem());

        ACLMessage bidRequestMsg = MessageCreator.createMessage(ACLMessage.CFP, participants, messageType, offer);

        contractor.send(bidRequestMsg);
    }

    public void takeBid(AID sender, Bid bid) {
        System.out.println("########## Received Bid ##########");
        if( bid.getAuctionID() == currentContractNet.getID() ) {
            currentContractNet.takeBid(sender, bid.getValue());

            if( currentContractNet.readyForEvaluation() ){
                List<AID> winner = currentContractNet.getWinner();
                System.out.println("Winner: "+winner);
                //TODO: Maybe the case there is no winner because anyone bid for the ContractNet!!!
                notifyWinnerLossers(winner);
                notifySeller(winner.get(0));
            }
        }else{
            throw new IllegalStateException("Received Bid for non-existing, or pending ContractNet.");
        }

    }

    private void notifySeller(AID winner) {
        ACLMessage msg = MessageCreator.createInform(currentContractNet.getSeller(), MessageContent.FIRMEN_CONTRACTNET, winner);
        contractor.send(msg);
    }

    private void notifyWinnerLossers(List<AID> list) {
        System.out.println("########### ANNOUNCING WINNER #############");

        String messageType = MessageContent.FIRMEN_CONTRACTNET;
        Offer offer = new Offer(contractor.getAID(), currentContractNet.getID(), currentContractNet.getItem());
        ACLMessage winNotification = MessageCreator.createMessage(ACLMessage.ACCEPT_PROPOSAL, list.get(0), messageType, offer);
        contractor.send(winNotification);
        
        list.remove(0);
        ACLMessage lostNotification = MessageCreator.createMessage(ACLMessage.REJECT_PROPOSAL, list, messageType, null);
        contractor.send(lostNotification);
    }

    public void confirmAction(AID sender, Offer offer) {

        if( currentContractNet.readyForEvaluation()
            && currentContractNet.getWinner().equals(sender)
            && offer.getContractNetID() == currentContractNet.getID() )
        {
            System.out.println("########### ANNOUNCING COMPLETED #############");
            currentContractNet = null;
            contractNetInProgress = false;

            if( !pendingContractNets.isEmpty() ) {
                startNextAuction();
            }
        }else{
            throw new IllegalStateException("Received illegal contractNet confirmation from agent aid " + sender + " for auction id " + offer.getContractNetID() + ", current auction id is " + currentContractNet.getID() + ", winner is "+currentContractNet.getWinner() + " and auction is ready for evaluation: " + currentContractNet.readyForEvaluation());
        }
    }
}
