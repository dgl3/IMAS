/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;

import static cat.urv.imas.agent.ImasAgent.OWNER;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.behaviour.FiremenCoordinator.InformBehaviour;
import jade.domain.FIPANames.InteractionProtocol;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import java.util.LinkedList;
import java.util.List;
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

    public FiremenCoordinatorAgent() {
        super(AgentType.FIREMEN_COORDINATOR);
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
        addBehaviour( newListenerBehaviour() );
    }
    
    /**
     * Checks every cycle if a new fireman occured (Fireman sends a message).
     * If yes, this fireman is added to the coordinators fireman list.
     * @return 
     */
    private CyclicBehaviour newListenerBehaviour(){
        return new CyclicBehaviour(this) {
            @Override
            public void action() {
                FiremenCoordinatorAgent agent = (FiremenCoordinatorAgent)this.getAgent();
                ACLMessage msg = receive();
                if (msg != null){
                    if (msg.getPerformative() == ACLMessage.SUBSCRIBE){
                        if (msg.getSender().getLocalName().startsWith("fireman")){
                            firemenAgents.add(msg.getSender());
                            System.out.println(getLocalName() + ": added " + msg.getSender().getLocalName());
                        }
                        // If game information is set, send it to the subscriber
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
                        
                        // When game information is updated, send it to all children
                        
                        for (AID firemanAgent : agent.firemenAgents) {
                            agent.sendGame(firemanAgent);
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
