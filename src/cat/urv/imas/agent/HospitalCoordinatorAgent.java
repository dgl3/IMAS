/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;

import cat.urv.imas.agent.communication.auction.Bid;
import cat.urv.imas.agent.communication.auction.Item;
import cat.urv.imas.agent.communication.auction.AuctionManager;
import cat.urv.imas.agent.communication.auction.Offer;
import cat.urv.imas.agent.communication.ambcontractnet.ContractNetManager;
import cat.urv.imas.agent.communication.ambcontractnet.ContractBid;
import cat.urv.imas.agent.communication.ambcontractnet.ContractOffer;
import cat.urv.imas.agent.communication.util.KeyValue;
import cat.urv.imas.agent.communication.util.MessageCreator;
import cat.urv.imas.map.StreetCell;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.MessageContent;
import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.ACLMessage;
import java.util.*;

/**
 *
 * @author Joan Mari
 */
public class HospitalCoordinatorAgent extends ImasAgent {

    /**
     * Game settings in use.
     */
    private GameSettings game;
    
    /**
     * Coordinator agent id.
     */
    private AID coordinatorAgent;
    
    private List<AID> ambulanceAgents;

    private List<AID> hospitalAgents;

    /**
     * Class responsible of ContractNet management
     */
    private ContractNetManager contractor;
    
    /**
     * Ambulance / Hospital Auction manager
     */
    private AuctionManager auctionManager;

    /**
     * Set to keep track of who still needs to confirm that he has received the game update
     */
    private HashSet<AID> pendingGameUpdateConfirmations;

    /**
     * List of agents ready to end the turn
     */
    private List<AgentAction> finishedAmbulanceAgents;
    
    public HospitalCoordinatorAgent() {
        super(AgentType.HOSPITAL_COORDINATOR);
    }
    
    @Override
    protected void setup() {
        registerToDF();

        // search CoordinatorAgent
        ServiceDescription searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.COORDINATOR.toString());
        this.coordinatorAgent = UtilsAgents.searchAgent(this, searchCriterion);
        // searchAgent is a blocking method, so we will obtain always a correct AID

        /* ********************************************************************/
        //No idea why this message is sent
        /* 
        ACLMessage initialRequest = new ACLMessage(ACLMessage.REQUEST);
        initialRequest.clearAllReceiver();
        initialRequest.addReceiver(this.coordinatorAgent);
        initialRequest.setProtocol(InteractionProtocol.FIPA_REQUEST);
        */
        ambulanceAgents = new LinkedList<>();

        hospitalAgents = new LinkedList<>();
        
        finishedAmbulanceAgents = new ArrayList<>();

        auctionManager = new AuctionManager(this);
        
        contractor = new ContractNetManager(this, MessageContent.AMBULANCE_CONTRACT_NET);

        pendingGameUpdateConfirmations = new HashSet<>();

        addBehaviour( newListenerBehaviour() );

    }
    
    private CyclicBehaviour newListenerBehaviour(){
        return new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg;
                while ((msg = receive()) != null) {
                    switch (msg.getPerformative()){
                        case ACLMessage.SUBSCRIBE:
                            handleSubscribe(msg);
                            break;
                        case ACLMessage.INFORM:
                            handleInform(msg);
                            break;
                        case ACLMessage.PROPOSE:
                            handleProposal(msg);
                            break;
                        case ACLMessage.PROXY:
                            handleProxy(msg);
                            break;
                        case ACLMessage.CONFIRM:
                            handleConfirm(msg);
                            break;
                        default:
                            log("Unsupported message received." + msg.getPerformative() );
                    }
                }
                block(); // Confirm. Apparently 'just' schedults next execution. 'Generally all action methods should end with a call to block() or invoke it before doing return.'
            };
        };
    }

    private void handleConfirm(ACLMessage msg) {
        KeyValue<String, Object> content = getMessageContent(msg);
        switch(content.getKey()) {
            case MessageContent.AMBULANCE_CONTRACT_NET:
                contractor.confirmAction(msg.getSender(), (ContractOffer) content.getValue());
                break;
            case MessageContent.AMBULANCE_AUCTION:
                Offer offer = (Offer) content.getValue();
                auctionManager.confirmAction(msg.getSender(), offer);
                break;
            case MessageContent.SEND_GAME:
                boolean wasRemoved = pendingGameUpdateConfirmations.remove(msg.getSender());
                if ( !wasRemoved ) throw new IllegalStateException("Got game update confirmation from unknown AID");

                // Propagate confirm message
                if(pendingGameUpdateConfirmations.isEmpty()){
                    ACLMessage gameUpdateConfirmMsg = MessageCreator.createConfirm(coordinatorAgent, MessageContent.SEND_GAME, null);
                    send(gameUpdateConfirmMsg);
                }

                break;
            default:
                log("CONFIRM Message Content not understood"+ content.getKey());
                break;
        }
    }

    private void handleProxy(ACLMessage msg) {
        KeyValue<String, Object> content = getMessageContent(msg);
        switch(content.getKey()) {
            case MessageContent.AMBULANCE_AUCTION:
                handleStartHospitalAuction(msg.getSender(), (Item)content.getValue());
                break;
            case MessageContent.AMBULANCE_CONTRACT_NET:
                log("New Fire(ambulances): "+game.getNewFire().toString());
                contractor.setupNewContractNet(coordinatorAgent, game.getNewFire(), Collections.unmodifiableCollection(ambulanceAgents));
                break;
            default:
                log("PROXY Message Content not understood" + content.getKey());
                break;
        }
    }

    private void handleProposal(ACLMessage msg) {
        KeyValue<String, Object> content = getMessageContent(msg);
        switch(content.getKey()) {
            case MessageContent.AMBULANCE_AUCTION:
                Bid bid = (Bid) content.getValue();
                auctionManager.takeBid(msg.getSender(), bid);
                break;
            case MessageContent.AMBULANCE_CONTRACT_NET:
                contractor.takeBid(msg.getSender(), (ContractBid) content.getValue());
                break;
            default:
                log("PROPOSAL Message Content not understood"+ content.getKey());
                break;
        }
    }

    /**
     * This method starts an auction to assign a hospital to a ambulance.
     * @param seller
     */
    private void handleStartHospitalAuction(AID seller, Item item) {
        auctionManager.setupNewAuction(seller, item, Collections.unmodifiableCollection(hospitalAgents) );
    }
    
    private void handleSubscribe(ACLMessage msg) {
        AID sender = msg.getSender();

        if (sender.getLocalName().startsWith("ambu")){
            ambulanceAgents.add(msg.getSender());
            log(getLocalName() + ": added " + sender.getLocalName());
        }
        if (sender.getLocalName().startsWith("hosp")){
            hospitalAgents.add(msg.getSender());
            log(getLocalName() + ": added " + sender.getLocalName());
        }

        // If game information is set, send it to the subscriber
        if (this.getGame() != null) {
            ACLMessage sendGameMsg = MessageCreator.createInform(sender, MessageContent.SEND_GAME, this.game);
            pendingGameUpdateConfirmations.add(msg.getSender());
            send(sendGameMsg);
        }
    }
    
    private void handleInform(ACLMessage msg) {

        KeyValue<String, Object> content = getMessageContent(msg);

        switch(content.getKey()) {
            case MessageContent.SEND_GAME:
                manageSendGame(msg, content);
                break;
            case MessageContent.NEW_FIRE_PETITION:
                // This will need to change to handle a new fire petition
                break;
            case MessageContent.END_TURN:
                log("Remainaing: " + (ambulanceAgents.size()-finishedAmbulanceAgents.size()) + "; last: " + msg.getSender().getLocalName());
                finishedAmbulanceAgents.add((AgentAction) content.getValue());
                // TODO: This is not reliable enough, look for another way
                if (finishedAmbulanceAgents.size() == ambulanceAgents.size()) {
                    this.endTurn();
                }

                break;
        default:
            log("Message Content not understood");
            break;
        }
    }

    private void manageSendGame(ACLMessage msg, KeyValue<String, Object> content) {
        log("INFORM received from " + ((AID) msg.getSender()).getLocalName());
        setGame((GameSettings) content.getValue());

        ACLMessage sendGameMsgHospitals = MessageCreator.createInform(hospitalAgents, MessageContent.SEND_GAME, this.game);
        ACLMessage sendGameMsgAmbulances = MessageCreator.createInform(ambulanceAgents, MessageContent.SEND_GAME, this.game);

        updatePendingGameUpdateConfirmations();

        send(sendGameMsgHospitals);
        send(sendGameMsgAmbulances);
    }

    private void updatePendingGameUpdateConfirmations() {
        pendingGameUpdateConfirmations.clear();
        pendingGameUpdateConfirmations.addAll(hospitalAgents);
        pendingGameUpdateConfirmations.addAll(ambulanceAgents);
    }


    /**
     * Update the game settings.
     *
     * @param game current game settings.
     */
    public void setGame(GameSettings game) {
        this.game = game;
    }

    /**
     * Gets the current game settings.
     *
     * @return the current game settings.
     */
    public GameSettings getGame() {
        return this.game;
    }

    
    public void endTurn() {
        if (this.finishedAmbulanceAgents.size() > 4) {
            log("lksjh");
        }
        ACLMessage endTurnMsg = MessageCreator.createInform(coordinatorAgent, MessageContent.END_TURN, new ArrayList<>(finishedAmbulanceAgents));
        finishedAmbulanceAgents.clear();
        send(endTurnMsg);
    }
}
