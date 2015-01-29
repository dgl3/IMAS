/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;

import static cat.urv.imas.agent.ImasAgent.OWNER;
import cat.urv.imas.behaviour.fireman.InformBehaviour;
import cat.urv.imas.map.Cell;
import cat.urv.imas.map.StreetCell;
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
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Joan Mari
 */
public class FiremanAgent extends ImasAgent{
    
    
    /**
     * Hospital position
     */
    private Cell currentPosition;
    
    /**
     * Game settings in use.
     */
    private GameSettings game;
    
    /**
     * Fireman-Coordinator agent id.
     */
    private AID firemanCoordinatorAgent;
    
    public FiremanAgent() {
        super(AgentType.FIREMAN);
    }
    
    @Override
    protected void setup() {
        /* ** Very Important Line (VIL) ***************************************/
        this.setEnabledO2ACommunication(true, 1);
        /* ********************************************************************/

        // Register the agent to the DF
        ServiceDescription sd1 = new ServiceDescription();
        sd1.setType(AgentType.FIREMAN.toString());
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
        
        ServiceDescription searchCriterion = new ServiceDescription();
        //searchCriterion.setType(AgentType.COORDINATOR.toString());
        //this.coordinatorAgent = UtilsAgents.searchAgent(this, searchCriterion);
        
        searchCriterion.setType(AgentType.FIREMEN_COORDINATOR.toString());
        this.firemanCoordinatorAgent = UtilsAgents.searchAgent(this, searchCriterion);
        
        notifyFiremanCoordinatorAgentOfCreation();
        
        addBehaviour( newListenerBehaviour() );
        
    }

    /**
     * Notifies the FiremanCoordinatorAgent that this Fireman has just been created.
     * The FiremanCoordinatorAgent can than add this Fireman to his list.
     */
    private void notifyFiremanCoordinatorAgentOfCreation() {
        ACLMessage creationNotificationMsg = new ACLMessage( ACLMessage.SUBSCRIBE );
        creationNotificationMsg.addReceiver(this.firemanCoordinatorAgent);
        send(creationNotificationMsg);
        
        System.out.println(getLocalName() + " sent subscription request.");
        
    }
    
    private CyclicBehaviour newListenerBehaviour(){
        return new CyclicBehaviour(this) {
            @Override
            public void action() {
                FiremanAgent agent = (FiremanAgent)this.getAgent();
                ACLMessage msg = receive();
                if (msg != null){
                    if (msg.getPerformative() == ACLMessage.INFORM) {
                        try {
                            agent.setGame((GameSettings) msg.getContentObject());
                        } catch (UnreadableException ex) {
                            Logger.getLogger(FiremenCoordinatorAgent.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        agent.log("Game updated");
                        agent.updatePosition();
                        
                        // TODO: this is just a test for the movement, all of this will be changed:
                        
                        Cell currentPosition = agent.getCurrentPosition();
                        int[] nextPosition = new int[2];
                        nextPosition[0] = currentPosition.getRow();
                        nextPosition[1] = currentPosition.getCol() + 1;
                        
                        Cell[][] map = agent.getGame().getMap();
                        if (!(map[nextPosition[0]][nextPosition[1]] instanceof StreetCell)) {
                            nextPosition[1] = currentPosition.getCol() - 1;
                        }
                        
                        AgentAction nextAction = new AgentAction(agent.getLocalName(), nextPosition);
                        
                        agent.endTurn(nextAction);
                    }
                }   
                block();
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
    
    /**
     * Updates the new current position from the game settings
     */
    public void updatePosition() {
        int firemanNumber = Integer.valueOf(this.getLocalName().substring(this.getLocalName().length() - 1));
        this.currentPosition = this.game.getAgentList().get(AgentType.FIREMAN).get(firemanNumber);
        log("Position updated: " + this.currentPosition.getRow() + "," + this.currentPosition.getCol() + "");
    }
    
    public Cell getCurrentPosition() {
        return this.currentPosition;
    }
    
    public void endTurn(AgentAction nextAction) {
        ACLMessage gameinformRequest = new ACLMessage(ACLMessage.INFORM);
        gameinformRequest.clearAllReceiver();
        gameinformRequest.addReceiver(this.firemanCoordinatorAgent);
        gameinformRequest.setProtocol(InteractionProtocol.FIPA_REQUEST);
        log("Inform message to agent");
        try {
            //gameinformRequest.setContent(MessageContent.SEND_GAME);
            Map<String,AgentAction> content = new HashMap<>();
            content.put(MessageContent.END_TURN, nextAction);
            gameinformRequest.setContentObject((Serializable) content);
            log("Inform message content: " + MessageContent.END_TURN);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        InformBehaviour gameInformBehaviour = new InformBehaviour(this, gameinformRequest);
        this.addBehaviour(gameInformBehaviour);
    }
}
