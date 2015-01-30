/**
 *  IMAS base code for the practical work.
 *  Copyright (C) 2014 DEIM - URV
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cat.urv.imas.agent;

import cat.urv.imas.agent.communication.contractnet.Offer;
import cat.urv.imas.agent.communication.util.KeyValue;
import cat.urv.imas.agent.communication.util.MessageCreator;
import cat.urv.imas.behaviour.coordinator.InformBehaviour;
import cat.urv.imas.behaviour.coordinator.RequesterBehaviour;
import cat.urv.imas.map.Cell;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.MessageContent;
import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.*;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The main Coordinator agent. 
 * TODO: This coordinator agent should get the game settings from the Central
 * agent every round and share the necessary information to other coordinators.
 */
public class CoordinatorAgent extends ImasAgent {

    /**
     * Game settings in use.
     */
    private GameSettings game;
    /**
     * Central agent id.
     */
    private AID centralAgent;
    
    /**
     * HospitalCoordinator agent id.
     */
    private AID hospitalCoordinator;

    /**
     * FiremenCoordinator agent id.
     */
    private AID firemenCoordinator;
    
    /**
     * List of agents ready to end the turn
     */
    private List<AgentAction> finishedFiremanAgents;
    
    /**
     * List of agents ready to end the turn
     */
    private List<AgentAction> finishedAmbulanceAgents;
    
    /**
     * Builds the coordinator agent.
     */
    public CoordinatorAgent() {
        super(AgentType.COORDINATOR);
    }

    /**
     * Agent setup method - called when it first come on-line. Configuration of
     * language to use, ontology and initialization of behaviours.
     */
    @Override
    protected void setup() {

        /* ** Very Important Line (VIL) ***************************************/
        this.setEnabledO2ACommunication(true, 1);
        /* ********************************************************************/

        // Register the agent to the DF
        ServiceDescription sd1 = new ServiceDescription();
        sd1.setType(AgentType.COORDINATOR.toString());
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

        // search CentralAgent
        ServiceDescription searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.CENTRAL.toString());
        this.centralAgent = UtilsAgents.searchAgent(this, searchCriterion);
        // searchAgent is a blocking method, so we will obtain always a correct AID
        
        // search HospitalCoordinator
        searchCriterion.setType(AgentType.HOSPITAL_COORDINATOR.toString());
        this.hospitalCoordinator = UtilsAgents.searchAgent(this, searchCriterion);
        // searchAgent is a blocking method, so we will obtain always a correct AID
        
        // search FiremenCoordinator
        searchCriterion.setType(AgentType.FIREMEN_COORDINATOR.toString());
        this.firemenCoordinator = UtilsAgents.searchAgent(this, searchCriterion);
        // searchAgent is a blocking method, so we will obtain always a correct AID
        

        /* TODO: Define all the behaviours **/
        /*
        ACLMessage initialRequest = new ACLMessage(ACLMessage.REQUEST);
        initialRequest.clearAllReceiver();
        initialRequest.addReceiver(this.centralAgent);
        initialRequest.setProtocol(InteractionProtocol.FIPA_REQUEST);
        log("Request message to agent");
        try {
            initialRequest.setContent(MessageContent.GET_MAP);
            log("Request message content:" + initialRequest.getContent());
        } catch (Exception e) {
            e.printStackTrace();
        }

        //we add a behaviour that sends the message and waits for an answer
        RequesterBehaviour initialRequestBehaviour = new RequesterBehaviour(this, initialRequest);
        this.addBehaviour(initialRequestBehaviour);
        
        
// setup finished. When we receive the last inform, the agent itself will add
        // a behaviour to send/receive actions
        */
        this.addBehaviour(newListenerBehaviour());
    }
    
    /**
     * Main Listening behaviour for the Coordinator Agent
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
                        case ACLMessage.INFORM:
                            handleInform(msg);
                            break;
                        case ACLMessage.REJECT_PROPOSAL:
                            handleRejectProposal(msg);
                            break;
                        default:
                            log("Unsupported message received.");
                    }
                }
                block(); // Confirm. Apparently 'just' schedults next execution. 'Generally all action methods should end with a call to block() or invoke it before doing return.'
            };
        };
    }
    
    /**
     * Handle new incoming INFORM message
     */
    private void handleInform(ACLMessage msg) {
        KeyValue<String, Object> content = getMessageContent(msg);
        switch(content.getKey()){
            case MessageContent.SEND_GAME:
                log("INFORM received from " + ((AID) msg.getSender()).getLocalName());
                try {
                    //GameSettings gameSettings = (GameSettings) msg.getContentObject();
                    GameSettings gameSettings = (GameSettings) content.getValue();
                    setGame(gameSettings);
                    log(gameSettings.getShortString());
                    this.newTurn();
                    //Check if there is any new fire
                    this.checkNewFires();
                } catch (Exception e) {
                    errorLog("Incorrect content: " + e.toString());
                }
                break;
            case MessageContent.END_TURN:
                if (msg.getSender().getLocalName().equals("firemenCoord")) {
                    finishedFiremanAgents = new ArrayList<>();
                    finishedFiremanAgents.addAll((List<AgentAction>) content.getValue());
                } else {
                    finishedAmbulanceAgents = new ArrayList<>();
                    finishedAmbulanceAgents.addAll((List<AgentAction>) content.getValue());
                }
                // TODO: This is not reliable enough, look for another way
                if (finishedFiremanAgents != null && finishedAmbulanceAgents != null) {
                    this.endTurn();
                }
                break;
            default:
                log("Unsupported message");
                break;
        }
    }
    
    /**
     * Handle new incoming REJECT_PROPOSAL message
     */
    private void handleRejectProposal(ACLMessage msg) {
        CoordinatorAgent agent = this;
        Map<String,Object> contentObject;
        try {
            contentObject = (Map<String,Object>) msg.getContentObject();
            String content = contentObject.keySet().iterator().next();
            switch(content) {
                case MessageContent.SEND_GAME://TODO: What kind of content will have reject proposal from fireman coordinator
                    // send proposal
                    break;
            }
        } catch (UnreadableException ex) {
            Logger.getLogger(CoordinatorAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Check if there is anynew fire in the game
     */
    private void checkNewFires(){
        //Check newFire game property to know if in this turn appeared a new fire and in which cell.
        Cell newFire = this.game.getNewFire();
        if(newFire!=null){
            //ACLMessage 
            sendProxy(this.firemenCoordinator);
        }
    }
    
    private void newTurn() {
        // Coordinator agent actively sends game info at the start of each turn
        this.sendGame();
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
    
    public void sendGame() {
        List<AID> recievers = new ArrayList();
        recievers.add(hospitalCoordinator);
        recievers.add(firemenCoordinator);
        ACLMessage gameinformRequest = MessageCreator.createInform(recievers, MessageContent.SEND_GAME, game);
        send(gameinformRequest);
    }

    public void endTurn() {
        List<AgentAction> actions = new ArrayList<>();
        actions.addAll(this.finishedFiremanAgents);
        actions.addAll(this.finishedAmbulanceAgents);
        ACLMessage gameinformRequest = MessageCreator.createInform(centralAgent, MessageContent.END_TURN, actions);
        send(gameinformRequest);
        finishedFiremanAgents = null;
        finishedAmbulanceAgents = null;
    }
    
    public void sendProxy(AID reciever) {
        //TODO: Send PROXY to firemen Coordinator but it crashes somehow...
        //ACLMessage contractNetProposal = MessageCreator.createProxy(reciever, MessageContent.FIRMEN_CONTRACTNET, null);
        //send(contractNetProposal);
    }
    
}
