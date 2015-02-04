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
                //performNextMove();
                break;
            default:
                log("Accept Proposal Message Content not understood");
                break;
        }
    }
    
    private void handleRejectProposal(ACLMessage msg) {

          KeyValue<String, Object> content = getMessageContent(msg);
          switch(content.getKey()) {
              case MessageContent.AMBULANCE_CONTRACT_NET:
                    //performNextMove();
                  break;
              default:
                  throw new IllegalArgumentException("Message not unterstood. Proposal from " + content.getKey());
          }
    }

    private void handleCFP(ACLMessage msg) {
        KeyValue<String, Object> content = getMessageContent(msg);
        switch(content.getKey()){
            case MessageContent.AMBULANCE_CONTRACT_NET:
                ContractOffer offer = (ContractOffer)content.getValue();

                if(getTargetCells().isEmpty()){

                    int distanceBid = -1;
                    int people = 0;

                    distanceBid = studyDistance(offer.getCell(), getMaxDistToRescue());

                    int distNotGetMaxPeople = getMaxDistToRescue()+2-getGame().getPeoplePerAmbulance();
                    if (distanceBid<distNotGetMaxPeople){
                        people = getGame().getPeoplePerAmbulance();
                    }else{
                        people = getMaxDistToRescue()-(distanceBid-1);
                    }

                    int currentLoad = getGame().getAmbulanceCurrentLoad(AIDUtil.getLocalId(getLocalName()));
                    int max = getGame().getPeoplePerAmbulance();
                    int maxBasedOnLoad = max-currentLoad;

                    //Ferran
                    int peopleICanRescue = Math.min(people, maxBasedOnLoad);

                    if ( peopleICanRescue > 0 ) {
                        offer.reply(this, peopleICanRescue, distanceBid); // Because the smaller number will be winner
                        break;
                    }
                }

                offer.reply(this, -1, -1);
                break;
            default:
                log("CFP Message Content not understood");
                break;
        }
    }
    
    private int studyDistance(Cell buildingFire, int maxDist) {
        //study distance through graph

        Path path = computeOptimumPath(getCurrentPosition(), buildingFire, maxDist);

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
                log("-- < Ended Auction > --");
                AID targetHospital = (AID)content.getValue();

                addTargetCell( getGame().getAgentList().get(AgentType.HOSPITAL).get(AIDUtil.getLocalId(targetHospital)) );
                performNextMove();
                break;
            case MessageContent.SEND_GAME:
                manageSendGame((GameSettings) content.getValue());
                sendGameUpdateConfirmation(getParent());
                newTurn();
                performNextMove();
                break;
            default:
                log("Message Content not understood");
                break;
        }

    }

    private void performNextMove() {
        if( !getTargetCells().isEmpty() ) {

            Path path;
            if ( getCurrentTargetCell() instanceof BuildingCell ) {
                path = computeOptimumPath(getCurrentPosition(), getCurrentTargetCell(), getActualMaxDist());
            }else{
                path = computeOptimumPath(getCurrentPosition(), getCurrentTargetCell(), Integer.MAX_VALUE);
            }

            if(path!=null){
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
                pollCurrentTargetCell();
                AgentAction nextAction = new AgentAction(getAID(), getCurrentPosition());
                endTurn(nextAction);
            }
        }else{
            // Move to itself --> No move..
            AgentAction nextAction = new AgentAction(this.getAID(), getCurrentPosition());
            endTurn(nextAction);
        }
    }

    private void pickUpPeople() {

        if( getCurrentLoad() == getMaxLoad() || ((BuildingCell)getCurrentTargetCell()).getBurnedRatio() == 100 ){
            pollCurrentTargetCell();

            if( getCurrentLoad() > 0 ) {
                log("-- < Started Auction > --");
                ACLMessage msg = MessageCreator.createProxy(getParent(), MessageContent.AMBULANCE_AUCTION, new Item(getCurrentPosition(), getCurrentLoad()));
                send(msg);
                return; // Don't perform action. Once auction ends, it will perform the corresponding action.
            }
            if( AIDUtil.getLocalId(getLocalName()) == 4 ){
                System.err.println("------------------I STAY");
            }
        }

        AgentAction agentAction = new AgentAction(this.getAID(), getCurrentPosition());
        agentAction.setAction(getCurrentTargetCell(), 1);
        endTurn(agentAction);
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

    @Override
    public void addTargetCell(Cell cell){
        if ( !getTargetCells().isEmpty() ) throw new IllegalArgumentException("Ambulances only support one target cell.");
        super.addTargetCell(cell);
    }


}
