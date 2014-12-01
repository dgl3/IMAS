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

import cat.urv.imas.behaviour.coordinator.InformBehaviour;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.behaviour.coordinator.RequesterBehaviour;
import cat.urv.imas.onthology.MessageContent;
import jade.core.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.*;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
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
        
        
        //System.out.println(this.game.getAgentList());
        // setup finished. When we receive the last inform, the agent itself will add
        // a behaviour to send/receive actions
        
        
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
        /* TODO: Define all the behaviours **/
        ACLMessage gameinformRequest = new ACLMessage(ACLMessage.INFORM);
        gameinformRequest.clearAllReceiver();
        //gameinformRequest.addReceiver(this.hospitalCoordinator);
        gameinformRequest.addReceiver(this.firemenCoordinator);
        gameinformRequest.setProtocol(InteractionProtocol.FIPA_REQUEST);
        log("Inform message to agent");
        try {
            gameinformRequest.setContentObject(this.game);
            log("Inform message content: game");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        InformBehaviour gameInformBehaviour = new InformBehaviour(this, gameinformRequest);
        this.addBehaviour(gameInformBehaviour);
    }

}
