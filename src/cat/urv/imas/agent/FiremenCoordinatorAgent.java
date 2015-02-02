/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;

import cat.urv.imas.agent.communication.contractnet.ContractBid;
import cat.urv.imas.agent.communication.contractnet.ContractNetManager;
import cat.urv.imas.agent.communication.contractnet.ContractOffer;
import cat.urv.imas.agent.communication.util.KeyValue;
import cat.urv.imas.agent.communication.util.MessageCreator;
import cat.urv.imas.graph.Graph;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.MessageContent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

import java.util.*;

/**
 *
 * @author Joan Mari
 */
public class FiremenCoordinatorAgent extends ImasAgent {

    /**
     * Game settings in use.
     */
    private GameSettings game;
    
    /**
     * Class responsible of ContractNet management
     */
    private ContractNetManager contractor;
    
    /**
     * Class responsible of ContractNet management
     */
    private Map<AID,Graph> mapActionAreas;

    /**
     * Coordinator agent id.
     */
    private AID coordinatorAgent;

    /**
     * Coordinator agent id.
     */
    // TODO: Change to map
    private List<AID> firemenAgents;

    /**
     * Set to keep track of who still needs to confirm that he has received the game update
     */
    private HashSet<AID> pendingGameUpdateConfirmations;
    
    /**
     * List of agents ready to end the turn
     */
    private List<AgentAction> finishedFiremanAgents;

    public FiremenCoordinatorAgent() {
        super(AgentType.FIREMEN_COORDINATOR);
    }

    @Override
    protected void setup() {
        registerToDF();

        // search CoordinatorAgent
        ServiceDescription searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.COORDINATOR.toString());
        this.coordinatorAgent = UtilsAgents.searchAgent(this, searchCriterion);
        // searchAgent is a blocking method, so we will obtain always a correct AID

        // search FiremanAgent
        // TODO: There are multiple fireman agents
        //searchCriterion.setType(AgentType.FIREMAN.toString());
        //this.fireman = UtilsAgents.searchAgent(this, searchCriterion);
        // searchAgent is a blocking method, so we will obtain always a correct AID
        firemenAgents = new LinkedList<>();
        finishedFiremanAgents = new ArrayList<>();
        contractor = new ContractNetManager(this);
        addBehaviour(newListenerBehaviour());

        pendingGameUpdateConfirmations = new HashSet<>();
        mapActionAreas = new HashMap<>();
    }

    /**
     * Checks every cycle if a new fireman occured (Fireman sends a message). If
     * yes, this fireman is added to the coordinators fireman list.
     *
     * @return
     */
    private CyclicBehaviour newListenerBehaviour() {
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
                            handlePropose(msg);
                            break;
                        case ACLMessage.PROXY:
                            handleProxy(msg);
                            break;
                        case ACLMessage.CONFIRM:
                            handleConfirm(msg);
                            break;
                        default:
                            log("Unsupported message received." + msg.getPerformative());
                    }
                }
                block(); // Confirm. Apparently 'just' schedults next execution. 'Generally all action methods should end with a call to block() or invoke it before doing return.'
            };
        };
    }
    
    private void handleProxy(ACLMessage msg){
        KeyValue<String, Object> content = getMessageContent(msg);
        switch(content.getKey()){
            case MessageContent.FIRMEN_CONTRACTNET:
                contractor.setupNewContractNet(coordinatorAgent, game.getNewFire(), Collections.unmodifiableCollection(firemenAgents));
                break;
            default:
                log("Unsupported message");
                break;
        }
    }
    
    private void handleConfirm(ACLMessage msg) {
        KeyValue<String, Object> content = getMessageContent(msg);
        switch(content.getKey()){
            case MessageContent.FIRMEN_CONTRACTNET:
                contractor.confirmAction(msg.getSender(), (ContractOffer) content.getValue());
                break;
            case MessageContent.ACTION_AREAS:
                //Store action areas and send list back
                mapActionAreas.put(msg.getSender(), (Graph) content.getValue());
                if(mapActionAreas.size()==firemenAgents.size()){
                    ACLMessage mapGraphs = MessageCreator.createInform(firemenAgents, MessageContent.ACTION_AREAS, mapActionAreas);
                    send(mapGraphs);
                    mapActionAreas.clear();
                }
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
                log("Unsupported message");
                break;
        }
    }
    
    private void handlePropose(ACLMessage msg){
        KeyValue<String, Object> content = getMessageContent(msg);
        switch(content.getKey()){
            case MessageContent.FIRMEN_CONTRACTNET:
                contractor.takeBid(msg.getSender(), (ContractBid) content.getValue());
                break;
            default:
                log("Unsupported message");
                break;
                   
        }
    }
    
    private void handleInform(ACLMessage msg) {
        KeyValue<String, Object> content = getMessageContent(msg);
        switch(content.getKey()) {
            case MessageContent.SEND_GAME:
                setGame((GameSettings) content.getValue());
                // Send game to children
                pendingGameUpdateConfirmations.addAll(firemenAgents);
                ACLMessage gameInformRequest = MessageCreator.createMessage(ACLMessage.INFORM, firemenAgents, MessageContent.SEND_GAME, this.game);
                send(gameInformRequest);


                break;
            case MessageContent.END_TURN:
                finishedFiremanAgents.add((AgentAction) content.getValue());
                // TODO: This is not reliable enough, look for another way
                if (finishedFiremanAgents.size() == firemenAgents.size()){
                    endTurn();
                }
                break;
        default:
            log("Message Content not understood");
            break;
        }
    }

    private void handleSubscribe(ACLMessage msg) {
        if (msg.getSender().getLocalName().startsWith("fireman")) {
            AID subscriber = msg.getSender();
            firemenAgents.add(subscriber);
        }
        // If game information is set, send it to the subscriber
        if (getGame() != null) {
            pendingGameUpdateConfirmations.add(msg.getSender());
            ACLMessage gameInformRequest = MessageCreator.createInform(msg.getSender(), MessageContent.SEND_GAME, this.game);
            send(gameInformRequest);
        }
    }

    /**
     * Update the game settings.
     *
     * @param game current game settings.
     */
    private void setGame(GameSettings game) {
        this.game = game;
    }

    /**
     * Gets the current game settings.
     *
     * @return the current game settings.
     */
    private GameSettings getGame() {
        return this.game;
    }

    
    public void endTurn() {
        ACLMessage msg = MessageCreator.createInform(coordinatorAgent, MessageContent.END_TURN, finishedFiremanAgents);
        errorLog("Sending actions...");
        send(msg);
        finishedFiremanAgents.clear();
    }
}
