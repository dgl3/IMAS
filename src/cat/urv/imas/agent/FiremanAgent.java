/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;

import static cat.urv.imas.agent.ImasAgent.OWNER;
import cat.urv.imas.agent.communication.contractnet.Bid;
import cat.urv.imas.agent.communication.contractnet.Offer;
import cat.urv.imas.agent.communication.util.KeyValue;
import cat.urv.imas.agent.communication.util.MessageCreator;
import cat.urv.imas.behaviour.fireman.InformBehaviour;
import cat.urv.imas.graph.Graph;
import cat.urv.imas.graph.Path;
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
import java.util.List;
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
     * Goal (extinguish) building in Fire cell.
     */
    private Cell extinguishCell;
    
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
        extinguishCell = null;
    }
    
    private CyclicBehaviour newListenerBehaviour(){
        return new CyclicBehaviour(this) {
            @Override
            public void action() {
                
                ACLMessage msg;
                while ((msg = receive()) != null){
                    switch (msg.getPerformative()){
                        case ACLMessage.INFORM:
                            handleInform(msg);
                            break;
                        case ACLMessage.CFP:
                            handleCFP(msg);
                            break;
                        case ACLMessage.ACCEPT_PROPOSAL:
                            handleAcceptProposal(msg);
                            break;
                        case ACLMessage.REJECT_PROPOSAL:
                            handleRejectProposal(msg);
                            break;
                        default:
                            log("Unsupported message received.");
                    }
                }   
                block();
            };
        };
    }
    
    private void handleAcceptProposal(ACLMessage msg) {
        KeyValue<String, Object> content = getMessageContent(msg);
        switch(content.getKey()){
            case MessageContent.FIRMEN_CONTRACTNET:
                Offer offer = (Offer) content.getValue();
                log("OK! I go there! " + offer.getCell());
                ACLMessage confirmation = MessageCreator.createConfirm(msg.getSender(), content.getKey(), offer);
                send(confirmation);
                //TODO: set extinguish goal
                extinguishCell = offer.getCell();
                break;
            default:
                log("Unsupported message");
                break;
        }
    }
    
    private void handleRejectProposal(ACLMessage msg) {
        //This agent was no selected for the contract net --> actions possible
    }

    private void handleInform(ACLMessage msg) {
        FiremanAgent agent = this;
        Map<String,Object> contentObject;
        
        try {
            contentObject = (Map<String,Object>) msg.getContentObject();
            String content = contentObject.keySet().iterator().next();
            
            switch(content) {
                case MessageContent.SEND_GAME:
                    agent.setGame((GameSettings) contentObject.get(content));
                    agent.log("Game updated");
                    agent.updatePosition();

                    // TODO: this is just a test for the movement, all of this will be changed:

                    Cell cPosition = agent.getCurrentPosition();
                    int[] nextPosition = new int[2];
                    nextPosition[0] = cPosition.getRow();
                    nextPosition[1] = cPosition.getCol() + 1;

                    Cell[][] map = agent.getGame().getMap();
                    if (!(map[nextPosition[0]][nextPosition[1]] instanceof StreetCell)) {
                        nextPosition[1] = cPosition.getCol() - 1;
                    }
                    
                    if(game.getNewFire()==null){
                        //TODO: nextAction based on distribution
                    }else{
                        //TODO: wait for contractNet, if ACCEPT-->action, if REJECT nextAction based on distribution
                    }
                    
                    AgentAction nextAction = new AgentAction(agent.getLocalName(), nextPosition);
                    agent.endTurn(nextAction);
                    break;
                default:
                    agent.log("Message Content not understood");
                    break;
            }
        } catch (UnreadableException ex) {
            Logger.getLogger(FiremenCoordinatorAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void handleCFP(ACLMessage msg) {
        FiremanAgent agent = this;
        Map<String,Object> contentObject;
        
        try {
            contentObject = (Map<String,Object>) msg.getContentObject();
            String content = contentObject.keySet().iterator().next();
            
            switch(content) {
                case MessageContent.PROPOSAL_CONTRACTNET:
                    agent.log("Contract Net request recieved from agent " + msg.getSender().getLocalName());
                    //TODO: Study if bid or not bid for the Contract Net...
                    //1.Consider the contiguos street cells of the building with the new fire
                    //Cell builtFire = this.game.getNewFire();
                    
                    //2.Possibilities: 1 street-cell (out-corner) 3 street-cells (aperture), 5 street-cells (in-corner)
                    //3.Use middle street-cell to cumpute the RBF minimum path.
                    //4.Based on the possible scenario substract (0, 1 or 2 respectively) to the path length (if negative number then 0).
                    //5.Since the agents are forced to first do the action and then move, the minimum number
                    //of cells should be 18 or less to arrive at 95% of building in fire. If it is the case
                    //bid with the number of turns to arrive.
                    
                    break;
                case MessageContent.FIRMEN_CONTRACTNET:
                    
                    Offer offer = (Offer)contentObject.get(content);
                    int distanceBid = studyDistance(offer.getCell());
                    log("I replied");
                    offer.reply(this, distanceBid);
                    break;
            }
                
        } catch (UnreadableException ex) {
            Logger.getLogger(FiremenCoordinatorAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    
    
    private int studyDistance(Cell buildingFire) {
        //study distance through danis code
        
        Graph graph = new Graph(game);
        log("getting path...");
        Path path = graph.computeOptimumPath(currentPosition, buildingFire);
        if(path==null){
            log("WARNING!! --> path is null");
        }
        int distance = path.getDistance();
        if(distance<19){
            return distance;
        }
        return -1;
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
        AID agentID;
        int firemanNumber = Integer.valueOf(this.getLocalName().substring(this.getLocalName().length() - 1));
        this.currentPosition = this.game.getAgentList().get(AgentType.FIREMAN).get(firemanNumber);
        log("Position updated: " + this.currentPosition.getRow() + "," + this.currentPosition.getCol() + "");
    }
    
    public Cell getCurrentPosition() {
        return this.currentPosition;
    }
    
    public void endTurn(AgentAction nextAction) {
        ACLMessage actionInfo = MessageCreator.createInform(firemanCoordinatorAgent, MessageContent.END_TURN, nextAction);
        send(actionInfo);
    }
}
