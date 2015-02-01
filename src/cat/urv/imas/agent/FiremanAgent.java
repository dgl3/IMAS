/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;

import static cat.urv.imas.agent.ImasAgent.OWNER;
import cat.urv.imas.agent.communication.contractnet.ContractOffer;
import cat.urv.imas.agent.communication.util.KeyValue;
import cat.urv.imas.agent.communication.util.MessageCreator;
import cat.urv.imas.graph.Graph;
import cat.urv.imas.graph.Path;
import cat.urv.imas.map.BuildingCell;
import cat.urv.imas.map.Cell;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.MessageContent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

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
                ContractOffer offer = (ContractOffer) content.getValue();
                ACLMessage confirmation = MessageCreator.createConfirm(msg.getSender(), content.getKey(), offer);
                send(confirmation);
                //TODO: set extinguish goal
                extinguishCell = offer.getCell();
                //Action related to added pending task...
                actionTask();
                break;
            default:
                log("Accept Proposal Message Content not understood");
                break;
        }
    }
    
    private void handleRejectProposal(ACLMessage msg) {
        //This agent was no selected for the contract net --> actions possible
        //nextAction <-- movement related to distribution
        KeyValue<String, Object> content = getMessageContent(msg);
        switch(content.getKey()){
            case MessageContent.FIRMEN_CONTRACTNET:
                if(extinguishCell==null){
                    dummyTask();
                }else{
                    actionTask();
                }
                break;
        }
        //TODO: If there is a new fire I wait for the CFP and there agent will not bid if
        //it can arrive or if he has an action, so here (when it is reject by the contractor)
        //is dicriminating about which case was the reason to not bid.
    }

    private void handleInform(ACLMessage msg) {
        KeyValue<String, Object> content = getMessageContent(msg);
        switch(content.getKey()){
            case MessageContent.SEND_GAME:
                setGame((GameSettings) content.getValue());
                sendGameUpdateConfirmation(firemanCoordinatorAgent);
                log("Game updated");
                updatePosition();
                if(game.getNewFire()==null){
                    if(extinguishCell!=null){
                        actionTask();
                    }else{
                        dummyTask();
                    }
                }
                break;
            default:
                log("Inform Message Content not understood: " + content.getKey());
                break;
        }
    }
    
    private void handleCFP(ACLMessage msg) {
        KeyValue<String, Object> content = getMessageContent(msg);
        switch(content.getKey()){
            case MessageContent.FIRMEN_CONTRACTNET:
                ContractOffer offer = (ContractOffer)content.getValue();
                int distanceBid = -1;
                if(extinguishCell==null){
                    distanceBid = studyDistance(offer.getCell());
                }
                offer.reply(this, distanceBid);
                break;
            default:
                log("CFP Message Content not understood");
                break;
                
        }
    }    
    
    private int studyDistance(Cell buildingFire) {
        //study distance through graph
        Graph graph = game.getGraph();
        log("Studying path... CurrentPosition: "+currentPosition.toString()+" BuilldingOnFire: "+buildingFire.toString());
        Path path = graph.computeOptimumPath(currentPosition, buildingFire);
        log("Path studied!");
        if(path==null){
            return -1;
        }else{
            return path.getDistance();
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
    
    /**
     * Updates the new current position from the game settings
     */
    public void updatePosition() {
        //AID agentID;
        int firemanNumber = Integer.valueOf(this.getLocalName().substring(this.getLocalName().length() - 1));
        this.currentPosition = this.game.getAgentList().get(AgentType.FIREMAN).get(firemanNumber);
    }
    
    public void endTurn(AgentAction nextAction) {
        ACLMessage actionInfo = MessageCreator.createInform(firemanCoordinatorAgent, MessageContent.END_TURN, nextAction);
        send(actionInfo);
    }

    private void actionTask() {                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     
        Path path = game.getGraph().computeOptimumPath(currentPosition, extinguishCell);
        if(path.getDistance()==0){//ACTION
            AgentAction nextAction = new AgentAction(getAID(), currentPosition);
            nextAction.setAction(extinguishCell, 1);
            endTurn(nextAction);
            errorLog("Extinguishing...");
            int row = extinguishCell.getRow();
            int col = extinguishCell.getCol();
            int burned = ((BuildingCell)game.get(row, col)).getBurnedRatio();
            errorLog("Cell: ["+((BuildingCell)extinguishCell).getRow()+"]["+((BuildingCell)extinguishCell).getCol()+"] of type ("+((BuildingCell)extinguishCell).getCellType().name()+")Burned Ratio: "+burned);
            if(burned<10){
                errorLog("I'M DONE OF EXTINGUISHING!!!");
                extinguishCell = null;
                //TODO: consider also moving since the world-norms dictate agents can action+movement
                //Here it is supposse that agent will do his last extinguish action and have a free movement...
                //It should be considered that if the extinguishCell==5% then he is like free for the ContractNet
            }
        }else{//MOVING
            AgentAction nextAction = new AgentAction(getAID(), path.getNextCellInPath());
            endTurn(nextAction);
            errorLog("Moving...");
        }
    }

    private void dummyTask() {
        AgentAction nextAction = new AgentAction(getAID(), currentPosition);
        endTurn(nextAction);
    }
}
