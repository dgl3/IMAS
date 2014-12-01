/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;

import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.behaviour.coordinator.RequesterBehaviour;
import cat.urv.imas.behaviour.hospitalCoordinator.InformBehaviour;
import cat.urv.imas.map.Cell;
import cat.urv.imas.onthology.MessageContent;
import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

        addBehaviour( newListenerBehaviour() );
    }
    
    private CyclicBehaviour newListenerBehaviour(){
        return new CyclicBehaviour(this) {
            @Override
            public void action() {
                HospitalCoordinatorAgent agent = (HospitalCoordinatorAgent)this.getAgent();
                ACLMessage msg = receive();
                if (msg != null){
                    if (msg.getPerformative() == ACLMessage.SUBSCRIBE){
                        if (msg.getSender().getLocalName().startsWith("ambu")){
                            ambulanceAgents.add(msg.getSender());
                            System.out.println(getLocalName() + ": added " + msg.getSender().getLocalName());
                        }
                        if (msg.getSender().getLocalName().startsWith("hosp")){
                            hospitalAgents.add(msg.getSender());
                            System.out.println(getLocalName() + ": added " + msg.getSender().getLocalName());
                        }
                        if (agent.getGame() != null) {
                            agent.sendGame(msg.getSender());
                        }
                    } else if (msg.getPerformative() == ACLMessage.INFORM) {
                        try {
                            agent.setGame((GameSettings) msg.getContentObject());
                        } catch (UnreadableException ex) {
                            Logger.getLogger(FiremenCoordinatorAgent.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        agent.log("Game updated");
                        
                        for (AID hospitalAgent : agent.hospitalAgents) {
                            agent.sendGame(hospitalAgent);
                        }
                        
                        for (AID ambulanceAgent : agent.ambulanceAgents) {
                            agent.sendGame(ambulanceAgent);
                        }
                    }
                }   
                block(); // Confirm. Apparently 'just' schedults next execution. 'Generally all action methods should end with a call to block() or invoke it before doing return.'
            };
        };
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
            gameinformRequest.setContentObject(this.game);
            log("Inform message content: game");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        InformBehaviour gameInformBehaviour = new InformBehaviour(this, gameinformRequest);
        this.addBehaviour(gameInformBehaviour);
    }
}
