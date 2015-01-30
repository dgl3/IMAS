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
import cat.urv.imas.agent.communication.util.KeyValue;
import cat.urv.imas.behaviour.hospitalCoordinator.InformBehaviour;
import cat.urv.imas.map.StreetCell;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.MessageContent;
import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.*;
import jade.lang.acl.ACLMessage;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private AuctionManager auctionManager;

    /**
     * List of agents ready to end the turn
     */
    private List<AgentAction> finishedAmbulanceAgents;
    
    public HospitalCoordinatorAgent() {
        super(AgentType.HOSPITAL_COORDINATOR);
    }
    
    @Override
    protected void setup() {
        /* ** Very Important Line (VIL) ***************************************/
        this.setEnabledO2ACommunication(true, 1);
        /* ********************************************************************/

        // Register the agent to the DF
        ServiceDescription sd1 = new ServiceDescription();
        sd1.setType(AgentType.HOSPITAL_COORDINATOR.toString());
        sd1.setName(getLocalName());
        sd1.setOwnership(OWNER);
        
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.addServices(sd1);
        dfd.setName(getAID());
        try {
            DFService.register(this, dfd);
            log("Registered to the DF");
        } catch (FIPAException e) {
            System.err.println(getLocalName() + " registration with DF unsucceeded. Reason: " + e.getMessage());
            doDelete();
        }

        // search CoordinatorAgent
        ServiceDescription searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.COORDINATOR.toString());
        this.coordinatorAgent = UtilsAgents.searchAgent(this, searchCriterion);
        // searchAgent is a blocking method, so we will obtain always a correct AID

        /* ********************************************************************/
        ACLMessage initialRequest = new ACLMessage(ACLMessage.REQUEST);
        initialRequest.clearAllReceiver();
        initialRequest.addReceiver(this.coordinatorAgent);
        initialRequest.setProtocol(InteractionProtocol.FIPA_REQUEST);

        ambulanceAgents = new LinkedList<>();

        hospitalAgents = new LinkedList<>();
        
        finishedAmbulanceAgents = new ArrayList<>();

        auctionManager = new AuctionManager(this);

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
                            log("Unsupported message received.");
                    }
                }
                block(); // Confirm. Apparently 'just' schedults next execution. 'Generally all action methods should end with a call to block() or invoke it before doing return.'
            };
        };
    }

    private void handleConfirm(ACLMessage msg) {
        KeyValue<String, Offer> content = getMessageContent(msg);
        switch(content.getKey()) {
            case MessageContent.AMBULANCE_AUCTION:
                Offer offer = content.getValue();
                auctionManager.confirmAction(msg.getSender(), offer);
                break;
            default:
                log("Message Content not understood");
                break;
        }
    }

    private void handleProxy(ACLMessage msg) {
        String content = msg.getContent();
        switch(content) {
            case MessageContent.AMBULANCE_AUCTION:
                handleStartHospitalAuction(msg.getSender());
                break;
            default:
                log("Message Content not understood");
                break;
        }
    }

    private void handleProposal(ACLMessage msg) {
        KeyValue<String, Bid> content = getMessageContent(msg);
        switch(content.getKey()) {
            case MessageContent.AMBULANCE_AUCTION:
                Bid bid = content.getValue();
                auctionManager.takeBid(msg.getSender(), bid);
                break;
            default:
                log("Message Content not understood");
                break;
        }
    }

    /**
     * This method starts an auction to assign a hospital to a ambulance.
     * @param seller
     */
    private void handleStartHospitalAuction(AID seller) {
        // Dummy. To be replaced by real ambulance details.
        Item item = new Item(new StreetCell(10, 10), 2);
        HashSet<AID> participants = new HashSet<AID>(hospitalAgents);

        auctionManager.setupNewAuction(seller, item, participants);
    }
    
    private void handleSubscribe(ACLMessage msg) {
        if (msg.getSender().getLocalName().startsWith("ambu")){
            ambulanceAgents.add(msg.getSender());
            System.out.println(getLocalName() + ": added " + msg.getSender().getLocalName());
        }
        if (msg.getSender().getLocalName().startsWith("hosp")){
            hospitalAgents.add(msg.getSender());
            System.out.println(getLocalName() + ": added " + msg.getSender().getLocalName());
        }
        // If game information is set, send it to the subscriber
        if (this.getGame() != null) {
            this.sendGame(msg.getSender());
        }
    }
    
    private void handleInform(ACLMessage msg) {
        HospitalCoordinatorAgent agent = this;
        Map<String,Object> contentObject;
        try {
            contentObject = (Map<String,Object>) msg.getContentObject();
            String content = contentObject.keySet().iterator().next();
            
            switch(content) {
                case MessageContent.SEND_GAME:
                    agent.log("INFORM received from " + ((AID) msg.getSender()).getLocalName());
                    agent.setGame((GameSettings) contentObject.get(content));
                    agent.log("Game updated");

                    // When game information is updated, send it to all children

                    for (AID hospitalAgent : agent.hospitalAgents) {
                        agent.sendGame(hospitalAgent);
                    }

                    for (AID ambulanceAgent : agent.ambulanceAgents) {
                        agent.sendGame(ambulanceAgent);
                    }
                    break;
                case MessageContent.NEW_FIRE_PETITION:
                    // This will need to change to handle a new fire petition
                    break;
                case MessageContent.END_TURN:
                    finishedAmbulanceAgents.add((AgentAction) contentObject.get(content));
                    // TODO: This is not reliable enough, look for another way
                    if (finishedAmbulanceAgents.size() == ambulanceAgents.size()) {
                        this.endTurn();
                    }

                    break;
            default:
                agent.log("Message Content not understood");
                break;
            }
        } catch (UnreadableException ex) {
            Logger.getLogger(CoordinatorAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
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
    
    public void sendGame(AID agent) {
        /* TODO: Define all the behaviours **/
        ACLMessage gameinformRequest = new ACLMessage(ACLMessage.INFORM);
        gameinformRequest.clearAllReceiver();
        gameinformRequest.addReceiver(agent);
        gameinformRequest.setProtocol(InteractionProtocol.FIPA_REQUEST);
        log("Inform message to agent");
        try {
            Map<String,GameSettings> content = new HashMap<>();
            content.put(MessageContent.SEND_GAME, this.game);
            gameinformRequest.setContentObject((Serializable) content);
            log("Inform message content: game");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        InformBehaviour gameInformBehaviour = new InformBehaviour(this, gameinformRequest);
        this.addBehaviour(gameInformBehaviour);
    }
    
    public void endTurn() {
        ACLMessage gameinformRequest = new ACLMessage(ACLMessage.INFORM);
        gameinformRequest.clearAllReceiver();
        gameinformRequest.addReceiver(this.coordinatorAgent);
        gameinformRequest.setProtocol(InteractionProtocol.FIPA_REQUEST);
        log("Inform message to agent");
        try {
            //gameinformRequest.setContent(MessageContent.SEND_GAME);
            Map<String,List<AgentAction>> content = new HashMap<>();
            content.put(MessageContent.END_TURN, this.finishedAmbulanceAgents);
            gameinformRequest.setContentObject((Serializable) content);
            log("Inform message content: " + MessageContent.END_TURN);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        InformBehaviour gameInformBehaviour = new InformBehaviour(this, gameinformRequest);
        this.addBehaviour(gameInformBehaviour);
        
        this.finishedAmbulanceAgents = new ArrayList<>();
    }
}
