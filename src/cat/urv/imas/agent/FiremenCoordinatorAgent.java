/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;

import static cat.urv.imas.agent.ImasAgent.OWNER;
import cat.urv.imas.behaviour.FiremenCoordinator.InformBehaviour;
import cat.urv.imas.behaviour.contractNet.ContractNetInitatorImpl;
import cat.urv.imas.map.Cell;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.MessageContent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.proto.ContractNetInitiator;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
     * Coordinator agent id.
     */
    private AID coordinatorAgent;

    /**
     * Coordinator agent id.
     */
    // TODO: Change to map
    private List<AID> firemenAgents;
    
    /**
     * Key: AID of an agent
     * Value: true if the agent is available; false otherwise
     */
    private Map<AID, Boolean> availableAgents;
    
    /**
     * List of agents ready to end the turn
     */
    private List<AgentAction> finishedFiremanAgents;

    public FiremenCoordinatorAgent() {
        super(AgentType.FIREMEN_COORDINATOR);
        availableAgents = new HashMap<AID, Boolean>();
    }

    @Override
    protected void setup() {
        /* ** Very Important Line (VIL) ***************************************/
        this.setEnabledO2ACommunication(true, 1);
        /* ********************************************************************/

        // Register the agent to the DF
        ServiceDescription sd1 = new ServiceDescription();
        sd1.setType(AgentType.FIREMEN_COORDINATOR.toString());
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

        // search FiremanAgent
        // TODO: There are multiple fireman agents
        //searchCriterion.setType(AgentType.FIREMAN.toString());
        //this.fireman = UtilsAgents.searchAgent(this, searchCriterion);
        // searchAgent is a blocking method, so we will obtain always a correct AID
        firemenAgents = new LinkedList<>();
        finishedFiremanAgents = new ArrayList<>();
        addBehaviour(newListenerBehaviour());
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
                log("INFORM MESSAGE RECEIVED");
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
                        default:
                            log("Unsupported message received.");
                    }
                }
                block(); // Confirm. Apparently 'just' schedults next execution. 'Generally all action methods should end with a call to block() or invoke it before doing return.'
            };
        };
    }
    
    private void handlePropose(ACLMessage msg){
        Map<String,Object> contentObject;
        try {
            contentObject = (Map<String,Object>) msg.getContentObject();
            String content = contentObject.keySet().iterator().next();
            
            switch(content) {
                case MessageContent.START_CONTRACTNET:
                    // Check if it is possible to start a ContractNet.
                    // If not, reject the proposal. 
                    this.log("Propose received from " + ((AID) msg.getSender()).getLocalName());
                    this.log("Propose type: "+MessageContent.START_CONTRACTNET);
                    try {
                        List<AID> available = enoughFiremen();
                        if(available.size()>0){
                            //Accepts the ContractNet
                            initiateContractNet(available);
                        }else{
                            //Reject the ContractNet
                            rejectContractNet();
                        }
                    } catch (Exception e) {
                        this.errorLog("Incorrect content: " + e.toString());
                    }
                    break;
                case MessageContent.BID_CONTRACTNET:
                    // Check if it is possible to start a ContractNet.
                    // If not, reject the proposal. 
                    this.log("Bid received from " + ((AID) msg.getSender()).getLocalName());
                    try {
                        //Thing about how to store the bids of each agent
                    } catch (Exception e) {
                        this.errorLog("Incorrect content: " + e.toString());
                    }
                    break;
            default:
                this.log("Message Content not understood");
                break;
            }
        } catch (UnreadableException ex) {
            Logger.getLogger(CoordinatorAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void initiateContractNet(List<AID> available){
        ACLMessage CFPproposals = new ACLMessage(ACLMessage.CFP);
        //Add receivers (available fireman agents)
        CFPproposals.clearAllReceiver();
        for(AID aid:available){
            CFPproposals.addReceiver(aid);
        }
        CFPproposals.setProtocol(InteractionProtocol.FIPA_REQUEST);
        log("ContractNet message formation sent");
        try {
            //TODO
            Map<String,GameSettings> content = new HashMap<>();
            content.put(MessageContent.PROPOSAL_CONTRACTNET, null);
            CFPproposals.setContentObject((Serializable) content);
        } catch (Exception e) {
            e.printStackTrace();
        }

        InformBehaviour gameInformBehaviour = new InformBehaviour(this, CFPproposals);
        this.addBehaviour(gameInformBehaviour);
    }
        
    public void rejectContractNet(){
        ACLMessage contractnetReject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
        contractnetReject.clearAllReceiver();
        contractnetReject.addReceiver(coordinatorAgent);
        contractnetReject.setProtocol(InteractionProtocol.FIPA_REQUEST);
        log("Reject proposal (Contract Net) to Coordinator Agent");
        try {
            Map<String,Object> content = new HashMap<>();
            content.put(MessageContent.REJECT_CONTRACTNET, null);
            contractnetReject.setContentObject((Serializable) content);
            log("Added Reject message content (null)");
        } catch (Exception e) {
            e.printStackTrace();
        }

        InformBehaviour rejectInformBehaviour = new InformBehaviour(this, contractnetReject);
        this.addBehaviour(rejectInformBehaviour);
    }
    
    private List<AID> enoughFiremen(){
        List<AID> available = new ArrayList<AID>();
        log("Available Agents...");
        for(AID agent: this.availableAgents.keySet()){
             if(availableAgents.get(agent)){
                 available.add(agent);
             }
        }
        return available;
        /**
        Cell cellFire = game.getNewFire();
        if(cellFire != null){
           for(AID agent: this.availableAgents.keySet()){
               if(availableAgents.get(agent)){
                   return true;
                   
                   int number = Integer.valueOf(agent.getLocalName().substring(agent.getLocalName().length()-1));
                   Cell currentCell = this.game.getAgentList().get(AgentType.FIREMAN).get(number);
                   //Compute distance to the fire
               }
           }
        }
        return false;
        * **/
    }
    
    private void handleInform(ACLMessage msg) {
        FiremenCoordinatorAgent agent = this;
        Map<String,Object> contentObject;
        try {
            contentObject = (Map<String,Object>) msg.getContentObject();
            String content = contentObject.keySet().iterator().next();
            
            switch(content) {
                case MessageContent.SEND_GAME:
                    agent.log("INFORM received from " + ((AID) msg.getSender()).getLocalName());
                    try {
                        finishedFiremanAgents = new ArrayList<>();
                        setGame((GameSettings) contentObject.get(content));

                        log("Game updated");

                        // When game information is updated, send it to all children
                        for (AID firemanAgent : firemenAgents) {
                            sendGame(firemanAgent);
                        }
                    } catch (Exception e) {
                        agent.errorLog("Incorrect content: " + e.toString());
                    }
                    break;
                case MessageContent.NEW_FIRE_PETITION:
                    // This will need to change to handle a new fire petition
                    agent.log("INFORM received from " + ((AID) msg.getSender()).getLocalName());
                    try {
                        finishedFiremanAgents = new ArrayList<>();
                        setGame((GameSettings) contentObject.get(content));

                        log("Game updated");

                        // When game information is updated, send it to all children
                        for (AID firemanAgent : firemenAgents) {
                            sendGame(firemanAgent);
                        }
                    } catch (Exception e) {
                        agent.errorLog("Incorrect content: " + e.toString());
                    }
                    break;
                case MessageContent.END_TURN:
                    finishedFiremanAgents.add((AgentAction) contentObject.get(content));
                    // TODO: This is not reliable enough, look for another way
                    if (finishedFiremanAgents.size() == firemenAgents.size()) {
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

    private void handleSubscribe(ACLMessage msg) {
        if (msg.getSender().getLocalName().startsWith("fireman")) {
            AID subscriber = msg.getSender();
            firemenAgents.add(subscriber);
            availableAgents.put(subscriber, Boolean.TRUE);
            log("added " + msg.getSender().getLocalName());
        }
        // If game information is set, send it to the subscriber
        if (getGame() != null) {
            sendGame(msg.getSender());
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

    private void sendGame(AID agent) {
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
            content.put(MessageContent.END_TURN, this.finishedFiremanAgents);
            gameinformRequest.setContentObject((Serializable) content);
            log("Inform message content: " + MessageContent.END_TURN);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        InformBehaviour gameInformBehaviour = new InformBehaviour(this, gameinformRequest);
        this.addBehaviour(gameInformBehaviour);
        
        finishedFiremanAgents = new ArrayList<>();
    }
}
