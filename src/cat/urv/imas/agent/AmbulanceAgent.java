/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;

import static cat.urv.imas.agent.ImasAgent.OWNER;
import cat.urv.imas.agent.communication.contractnet.ContractOffer;

import cat.urv.imas.agent.communication.util.AIDUtil;
import cat.urv.imas.agent.communication.util.KeyValue;
import cat.urv.imas.agent.communication.util.MessageCreator;
import cat.urv.imas.behaviour.ambulance.InformBehaviour;
import cat.urv.imas.graph.Graph;
import cat.urv.imas.graph.Path;
import cat.urv.imas.map.BuildingCell;
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
public class AmbulanceAgent extends ImasAgent{
    
    /**
     * Ambulance loading speed
     */
    private int loadingSpeed;
    
    /**
     * Ambulance capacity
     */
    private int ambulanceCapacity;
    
    /**
     * Ambulance position
     */
    private Cell currentPosition;
    
    /**
     * Goal (rescue people from) building in Fire cell.
     */
    private Cell rescueCell;
    
    /**
     * Game settings in use.
     */
    private GameSettings game;

    /**
     * The cell the ambulance wants to move to.
     */
    private Cell targetCell;

    
    /**
     * Coordinator agent id.
     */
    private AID hospitalCoordinatorAgent;
    
    public AmbulanceAgent() {
        super(AgentType.AMBULANCE);
    }
    
    @Override
    protected void setup() {
        /* ** Very Important Line (VIL) ***************************************/
        this.setEnabledO2ACommunication(true, 1);
        /* ********************************************************************/

        // Register the agent to the DF
        ServiceDescription sd1 = new ServiceDescription();
        sd1.setType(AgentType.AMBULANCE.toString());
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

        searchCriterion.setType(AgentType.HOSPITAL_COORDINATOR.toString());
        this.hospitalCoordinatorAgent = UtilsAgents.searchAgent(this, searchCriterion);
        
        notifyHospitalCoordinatorAgentOfCreation();
        
        addBehaviour( newListenerBehaviour() );
    }
    
    private void notifyHospitalCoordinatorAgentOfCreation() {
        ACLMessage creationNotificationMsg = new ACLMessage( ACLMessage.SUBSCRIBE );
        creationNotificationMsg.addReceiver(this.hospitalCoordinatorAgent);
        send(creationNotificationMsg);

        System.out.println(getLocalName() + " sent subscription request.");
        rescueCell = null;
    }
    
    private CyclicBehaviour newListenerBehaviour(){
        return new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg;
                while ((msg = receive()) != null){
                    switch (msg.getPerformative()){
                        case ACLMessage.CFP:
                            handleCFP(msg);
                            break;
                        case ACLMessage.INFORM:
                            handleInform(msg);
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
                block(); // Confirm. Apparently 'just' schedules next execution. 'Generally all action methods should end with a call to block() or invoke it before doing return.'
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
                rescueCell = offer.getCell();
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
                if(rescueCell==null){
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

    private void handleCFP(ACLMessage msg) {
        // TODO: For testing call this to let ambulance initiate auction.
        KeyValue<String, Object> content = getMessageContent(msg);
        switch(content.getKey()){
            case MessageContent.AMBULANCES_CONTRACTNET:
                ContractOffer offer = (ContractOffer)content.getValue();
                int distanceBid = -1;
                if(rescueCell==null){
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

    private void handleInform(ACLMessage msg) {
        KeyValue<String, Object> content = getMessageContent(msg);

        switch(content.getKey()) {
            case MessageContent.AMBULANCE_AUCTION:
                AID targetHospital = (AID)content.getValue();

                if( targetCell != null ) throw new IllegalStateException("Can't send ambulance. Ambulance has already another target.");
                targetCell = game.getAgentList().get(AgentType.HOSPITAL).get(AIDUtil.getLocalId(targetHospital));

                log("I will go to: " + targetCell);
                break;
            case MessageContent.SEND_GAME:
                manageSendGame((GameSettings) content.getValue());
                sendGameUpdateConfirmation(hospitalCoordinatorAgent);

                if( targetCell != null ) {

                    Path path = game.getGraph().computeOptimumPathUnconstrained(currentPosition, targetCell);
                    if (path.getDistance() > 0){
                        // Move towards the hospital
                        AgentAction agentAction = new AgentAction(this.getAID(), path.getNextCellInPath());
                        endTurn(agentAction);
                    }else{
                        // Drop Injured People
                        AgentAction agentAction = new AgentAction(this.getAID(), getCurrentPosition());

                        int currentLoad = game.getAmbulanceCurrentLoad( AIDUtil.getLocalId(getAID()) );
                        agentAction.setAction(targetCell, currentLoad);
                        endTurn(agentAction);
                    }
                }else{
                    // Move to itself --> No move..
                    AgentAction nextAction = new AgentAction(this.getAID(), getCurrentPosition());
                    endTurn(nextAction);
                }

                break;
            default:
                log("Message Content not understood");
                break;
        }

    }

    private void manageSendGame(GameSettings gameSettings) {
        setGame(gameSettings);
        log("Game updated");
        updatePosition();
        updateLoadingSpeed();
        updateAmbulanceCapacity();
    }

    public Cell getCurrentPosition() {
        return this.currentPosition;
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
        int ambulanceNumber = Integer.valueOf(this.getLocalName().substring(this.getLocalName().length() - 1));
        this.currentPosition = this.game.getAgentList().get(AgentType.AMBULANCE).get(ambulanceNumber);
    }
    
    /**
     * Updates the loading speed of the ambulance from the game settings
     */
    public void updateLoadingSpeed() {
        this.loadingSpeed = this.game.getAmbulanceLoadingSpeed();
    }
    
    /**
     * Updates the ambulance capacity from the game settings
     */
    public void updateAmbulanceCapacity() {
        this.ambulanceCapacity = this.game.getPeoplePerAmbulance();
    }
    
    public void endTurn(AgentAction nextAction) {
        ACLMessage msg = MessageCreator.createInform(hospitalCoordinatorAgent, MessageContent.END_TURN, nextAction);
        errorLog("Ambulance has sent his next action to the hospitalCoordinator!");
        send(msg);
    }
    
    private void actionTask() {                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     
        Path path = game.getGraph().computeOptimumPath(currentPosition, rescueCell);
        if(path.getDistance()==0){//ACTION
            AgentAction nextAction = new AgentAction(getAID(), currentPosition);
            nextAction.setAction(rescueCell, 1);
            endTurn(nextAction);
            errorLog("Extinguishing...");
            int row = rescueCell.getRow();
            int col = rescueCell.getCol();
            int burned = ((BuildingCell)game.get(row, col)).getBurnedRatio();
            errorLog("Cell: ["+((BuildingCell)rescueCell).getRow()+"]["+((BuildingCell)rescueCell).getCol()+"] of type ("+((BuildingCell)rescueCell).getCellType().name()+")Burned Ratio: "+burned);
            if(burned<10){
                errorLog("I'M DONE OF EXTINGUISHING!!!");
                rescueCell = null;
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
