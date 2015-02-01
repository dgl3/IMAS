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
 * Allows only one contractNet to be running at a time.
 New contractNet will be stored in a queue and executed in order.

 Created by Philipp Oliver on 29/1/15.
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
        contractNetIds++;
        ContractNet contractNet = new ContractNet(contractNetIds, item, new LinkedList(participants));
        pendingContractNets.add(contractNet);
        if( !contractNetInProgress ){
            startNextAuction();
        }else{
            System.out.println("Another ContractNet is already in progress!!!!");
        }
    }

    private void startNextAuction(){
        System.out.println("STARTING NEW CONTRACTNET");
        currentContractNet = pendingContractNets.poll();
        contractNetInProgress = true;
        startContractNet(currentContractNet);
    }

    private void startContractNet(ContractNet currentContractNet) {
        Collection<AID> participants = currentContractNet.getOutstandingBidders();
        String messageType = MessageContent.FIRMEN_CONTRACTNET;
        ContractOffer offer = new ContractOffer(contractor.getAID(), currentContractNet.getID(), currentContractNet.getItem());
        ACLMessage bidRequestMsg = MessageCreator.createMessage(ACLMessage.CFP, participants, messageType, offer);
        contractor.send(bidRequestMsg);
    }

    public void takeBid(AID sender, ContractBid bid) {
        if( bid.getAuctionID() == currentContractNet.getID() ) {
            currentContractNet.takeBid(sender, bid.getValue());

            if( currentContractNet.readyForEvaluation() ){
                Map<Integer,List<AID>> list = currentContractNet.getWinner();
                //TODO: Maybe the case there is no winner because none bid for the ContractNet!!!
                
                if(list.get(ContractNet.WINNER).isEmpty()){
                    notifySeller(null);
                }else{
                    notifySeller(list.get(ContractNet.WINNER).get(0));
                }
                notifyWinnerLossers(list);
            }
        }else{
            throw new IllegalStateException("Received Bid for non-existing, or pending ContractNet.");
        }

    }

    private void notifySeller(AID winner) {
        ACLMessage msg = MessageCreator.createInform(currentContractNet.getSeller(), MessageContent.FIRMEN_CONTRACTNET, winner);
        contractor.send(msg);
    }

    private void notifyWinnerLossers(Map<Integer,List<AID>> list){

        String messageType = MessageContent.FIRMEN_CONTRACTNET;
        ACLMessage lostNotification = MessageCreator.createMessage(ACLMessage.REJECT_PROPOSAL, list.get(ContractNet.LOOSER), messageType, null);
        contractor.send(lostNotification);
        if(!list.get(ContractNet.WINNER).isEmpty()){
            ContractOffer offer = new ContractOffer(contractor.getAID(), currentContractNet.getID(), currentContractNet.getItem());
            ACLMessage winNotification = MessageCreator.createMessage(ACLMessage.ACCEPT_PROPOSAL, list.get(ContractNet.WINNER).get(0), messageType, offer);
            contractor.send(winNotification);
        }else{
            closeContractNet();
        }
    }

    public void confirmAction(AID sender, ContractOffer offer){
        if( currentContractNet.readyForEvaluation()
            && currentContractNet.getWinner().get(ContractNet.WINNER).get(0).equals(sender)
            && offer.getContractNetID() == currentContractNet.getID() )
        {
            closeContractNet();
        }else{
            throw new IllegalStateException("Received illegal contractNet confirmation from agent aid " + sender.getLocalName() + " for contractNet id " + offer.getContractNetID() + ", current auction id is " + currentContractNet.getID() + ", winner is "+currentContractNet.getWinner().get(ContractNet.WINNER).get(0).getLocalName() + " and auction is ready for evaluation: " + currentContractNet.readyForEvaluation());
        }
    }
    
    private void closeContractNet() {
        currentContractNet = null;
        contractNetInProgress = false;
        if( !pendingContractNets.isEmpty() ) {
            startNextAuction();
        }
    }
}
