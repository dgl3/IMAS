/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;


import cat.urv.imas.agent.communication.ambcontractnet.ContractOffer;
import cat.urv.imas.agent.communication.auction.Item;
import cat.urv.imas.agent.communication.util.AIDUtil;
import cat.urv.imas.agent.communication.util.KeyValue;
import cat.urv.imas.agent.communication.util.MessageCreator;
import cat.urv.imas.graph.Path;
import cat.urv.imas.map.BuildingCell;
import cat.urv.imas.map.Cell;
import cat.urv.imas.map.HospitalCell;
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
            case MessageContent.AMBULANCE_CONTRACT_NET:
                ContractOffer offer = (ContractOffer) content.getValue();
                ACLMessage confirmation = MessageCreator.createConfirm(msg.getSender(), content.getKey(), offer);
                send(confirmation);

                addTargetCell(offer.getCell());
                performNextMove();
                //actionTask();
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
            case MessageContent.AMBULANCE_CONTRACT_NET:
                if(getTargetCell().isEmpty()){
                    //distributionTask(null);
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
            case MessageContent.AMBULANCE_CONTRACT_NET:
                ContractOffer offer = (ContractOffer)content.getValue();
                int distanceBid = -1;
                if(getTargetCell().isEmpty()){
                    distanceBid = studyDistance(offer.getCell(), 18, Boolean.FALSE);
                }
                
                int people = 0;
                if(distanceBid!=-1){
                    if (distanceBid<17){
                        people = getGame().getPeoplePerAmbulance();
                    }else{
                        people = 19-distanceBid;
                    }
                }
                
                int currentLoad = getGame().getAmbulanceCurrentLoad(AIDUtil.getLocalId(getLocalName()));
                int max = getGame().getPeoplePerAmbulance();
                int maxBasedOnLoad = max-currentLoad;
                int peopleICanRescue = Math.min(people, maxBasedOnLoad);
                
                if ( peopleICanRescue > 0 ){
                    offer.reply(this, peopleICanRescue ); // Because the smaller number will be winner
                }else{
                    offer.reply(this, -1);
                }
                break;
            default:
                log("CFP Message Content not understood");
                break;
        }
    }
    
    private int studyDistance(Cell buildingFire, int maxDist, Boolean future) {
        //study distance through graph

        Path path = null;
        if(future){
            Path auxPath = computeOptimumPath(getCurrentPosition(), getCurrentTargetCell(), 18);
            path = computeOptimumPath(auxPath.getPath().get(auxPath.getPath().size() - 1).getCell(), buildingFire, maxDist);
        }else{
            if ( buildingFire == null ) System.err.println("#################### NULL FIRE");
            if (this.getLocalName().equals("fireman2")) {
                log("This is the fireman 2");
            }
            path = computeOptimumPath(getCurrentPosition(), buildingFire, maxDist);
        }

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
                addTargetCell( getGame().getAgentList().get(AgentType.HOSPITAL).get(AIDUtil.getLocalId(targetHospital)) );
                performNextMove();
                break;
            case MessageContent.SEND_GAME:
                manageSendGame((GameSettings) content.getValue());
                if( AIDUtil.getLocalId(getLocalName()) == 1) System.err.println(getCurrentLoad());

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

                if (getCurrentTargetCell() instanceof HospitalCell) {
                    dropInjuredPeople();
                }else if( getCurrentTargetCell() instanceof BuildingCell ){
                    pickUpPeople();
                }else{
                    throw new IllegalArgumentException("Target is an unknown cell.");
                }

            }
        }else{
            // Move to itself --> No move..
            AgentAction nextAction = new AgentAction(this.getAID(), getCurrentPosition());
            endTurn(nextAction);
        }
    }

    private void pickUpPeople() {

        if( getCurrentLoad() == getMaxLoad() ){
            pollCurrentTargetCell();

            ACLMessage msg = MessageCreator.createProxy(getParent(), MessageContent.AMBULANCE_AUCTION, new Item(getCurrentPosition(), getCurrentLoad()));
            send(msg);
            // Don't perform action. Once auction ends, it will perform the corresponding action.
        }else {
            AgentAction agentAction = new AgentAction(this.getAID(), getCurrentPosition());
            agentAction.setAction(getCurrentTargetCell(), 1);
            endTurn(agentAction);
        }
    }

    private void dropInjuredPeople() {
        if(getCurrentLoad() == 0){
            pollCurrentTargetCell();
            dummyTask();
        }else {
            AgentAction agentAction = new AgentAction(this.getAID(), getCurrentPosition());

            int currentLoad = getGame().getAmbulanceCurrentLoad(AIDUtil.getLocalId(getAID()));
            agentAction.setAction(getCurrentTargetCell(), currentLoad);
            endTurn(agentAction);
        }
    }

    private void manageSendGame(GameSettings gameSettings) {
        setGame(gameSettings);
        updatePosition();
    }

    private void actionTask() {                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     
        Path path = computeOptimumPath(getCurrentPosition(), getCurrentTargetCell(), 18);
        if(path != null) {
            if (path.getDistance() == 0) {//ACTION+POSSIBLE MOVEMENT
                int burned = ((BuildingCell) getCurrentTargetCell()).getBurnedRatio();
                AgentAction nextAction = new AgentAction(getAID(), getCurrentPosition());

                if (burned > getGame().getFireSpeed() && burned < 100) {
                    //action+stay
                    nextAction.setAction(getCurrentTargetCell(), 1);
                } else {
                    Cell actualCell = getCurrentTargetCell();
                    pollCurrentTargetCell();
                    nextAction.setAction(actualCell, 1);
                    //POSSIBLE MOVEMENT
                    if (getTargetCell().isEmpty()) {
                        //position should be based on distribution...
                        //TODO: It never finish doing something!!!!
                        //distributionTask(nextAction);
                    } else {
                        // movement based on path...
                        //if path.distance == 0 then dont move
                        computeOptimumPath(getCurrentPosition(), getCurrentTargetCell(), 18);
                        if (path.getDistance() != 0) {
                            nextAction.setPosition(path.getNextCellInPath());
                        }
                    }
                }
                endTurn(nextAction);

            } else {//MOVING
                AgentAction nextAction = new AgentAction(getAID(), path.getNextCellInPath());
                endTurn(nextAction);
            }
        }else{
            pollCurrentTargetCell();

            AgentAction nextAction = new AgentAction(getAID(), getCurrentPosition());
            endTurn(nextAction);
        }
    }

    private void dummyTask() {
        AgentAction action = new AgentAction(getAID(), getCurrentPosition());
        endTurn(action);
    }

    private int getCurrentLoad(){
        return getGame().getAmbulanceCurrentLoad(AIDUtil.getLocalId(getLocalName()));
    }

    private int getMaxLoad(){
        return getGame().getPeoplePerAmbulance();
    }
    
}
