/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;


import cat.urv.imas.agent.communication.contractnet.ContractOffer;
import cat.urv.imas.agent.communication.util.AIDUtil;
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
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

/**
 *
 * @author Joan Mari
 */
public class AmbulanceAgent extends IMASVehicleAgent {
    
    /**
     * Ambulance loading speed
     */
    private int loadingSpeed;
    
    /**
     * Ambulance capacity
     */
    private int ambulanceCapacity;

    /**
     * Goal (rescue people from) building in Fire cell.
     */
    private Cell rescueCell;

    
    public AmbulanceAgent() {
        super(AgentType.AMBULANCE);
    }
    
    @Override
    protected void setup() {
        registerToDF();
        
        ServiceDescription searchCriterion = new ServiceDescription();

        searchCriterion.setType(AgentType.HOSPITAL_COORDINATOR.toString());
        setParent(UtilsAgents.searchAgent(this, searchCriterion));

        notifyHospitalCoordinatorAgentOfCreation();
        
        addBehaviour( newListenerBehaviour() );
    }
    
    private void notifyHospitalCoordinatorAgentOfCreation() {
        ACLMessage creationNotificationMsg = new ACLMessage( ACLMessage.SUBSCRIBE );
        creationNotificationMsg.addReceiver(getParent());
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
        Graph graph = getGame().getGraph();
        Path path = graph.computeOptimumPath(getCurrentPosition(), buildingFire,18);
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
                if( !getTargetCell().isEmpty() ) throw new IllegalStateException("Can't send ambulance. Ambulance has already another target.");
                addTargetCell( getGame().getAgentList().get(AgentType.HOSPITAL).get(AIDUtil.getLocalId(targetHospital)) );
                break;
            case MessageContent.SEND_GAME:
                manageSendGame((GameSettings) content.getValue());
                sendGameUpdateConfirmation(getParent());
                performNextMove();
                break;
            default:
                log("Message Content not understood");
                break;
        }

    }

    private void performNextMove() {
        if( !getTargetCell().isEmpty() ) {

            Path path = getGame().getGraph().computeOptimumPathUnconstrained(getCurrentPosition(), getCurrentTargetCell());
            if (path.getDistance() > 0){
                // Move towards the hospital
                AgentAction agentAction = new AgentAction(this.getAID(), path.getNextCellInPath());
                endTurn(agentAction);
            }else{
                // Drop Injured People
                AgentAction agentAction = new AgentAction(this.getAID(), getCurrentPosition());

                int currentLoad = getGame().getAmbulanceCurrentLoad(AIDUtil.getLocalId(getAID()));
                agentAction.setAction(getCurrentTargetCell(), currentLoad);
                endTurn(agentAction);
            }
        }else{
            // Move to itself --> No move..
            AgentAction nextAction = new AgentAction(this.getAID(), getCurrentPosition());
            endTurn(nextAction);
        }
    }

    private void manageSendGame(GameSettings gameSettings) {
        setGame(gameSettings);
        updatePosition();
        updateLoadingSpeed();
        updateAmbulanceCapacity();
    }

    /**
     * Updates the loading speed of the ambulance from the game settings
     */
    public void updateLoadingSpeed() {
        this.loadingSpeed = getGame().getAmbulanceLoadingSpeed();
    }
    
    /**
     * Updates the ambulance capacity from the game settings
     */
    public void updateAmbulanceCapacity() {
        this.ambulanceCapacity = getGame().getPeoplePerAmbulance();
    }

    private void actionTask() {                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     
        Path path = getGame().getGraph().computeOptimumPath(getCurrentPosition(), rescueCell, 18);

        if(path.getDistance()==0){//ACTION
            AgentAction nextAction = new AgentAction(getAID(), getCurrentPosition());
            nextAction.setAction(rescueCell, 1);
            endTurn(nextAction);
            errorLog("Extinguishing...");
            int row = rescueCell.getRow();
            int col = rescueCell.getCol();
            int burned = ((BuildingCell)getGame().get(row, col)).getBurnedRatio();
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
        AgentAction nextAction = new AgentAction(getAID(), getCurrentPosition());
        endTurn(nextAction);
    }
    
}
