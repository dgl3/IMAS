/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;

import cat.urv.imas.agent.communication.contractnet.ContractOffer;
import cat.urv.imas.agent.communication.util.KeyValue;
import cat.urv.imas.agent.communication.util.MessageCreator;
import cat.urv.imas.graph.Graph;
import cat.urv.imas.graph.Path;
import cat.urv.imas.map.BuildingCell;
import cat.urv.imas.map.Cell;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.MessageContent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

/**
 *
 * @author Joan Mari
 */
public class FiremanAgent extends IMASVehicleAgent{

    public FiremanAgent() {
        super(AgentType.FIREMAN);
    }
    
    @Override
    protected void setup() {
        registerToDF();
        
        ServiceDescription searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.FIREMEN_COORDINATOR.toString());
        setParent(UtilsAgents.searchAgent(this, searchCriterion));

        notifyFiremanCoordinatorAgentOfCreation();
        
        addBehaviour( newListenerBehaviour() );
    }

    /**
     * Notifies the FiremanCoordinatorAgent that this Fireman has just been created.
     * The FiremanCoordinatorAgent can than add this Fireman to his list.
     */
    private void notifyFiremanCoordinatorAgentOfCreation() {
        ACLMessage creationNotificationMsg = new ACLMessage( ACLMessage.SUBSCRIBE );
        creationNotificationMsg.addReceiver(getParent());
        send(creationNotificationMsg);
        pollCurrentTargetCell();
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
            }
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
                addTargetCell(offer.getCell());
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
                if(getTargetCell().isEmpty()){
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
                sendGameUpdateConfirmation(getParent());
                updatePosition();
                if(getGame().getNewFire()==null){
                    if(!getTargetCell().isEmpty()){
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
                if(getTargetCell().isEmpty()){
                    distanceBid = studyDistance(offer.getCell(), 18);
                }else if(getTargetCell().size()==1){
                    //take into account two possible tasks
                    int turnsExtinguish = ((BuildingCell)getCurrentTargetCell()).getBurnedRatio()/5;
                    distanceBid = studyDistance(offer.getCell(), 18-(turnsExtinguish-1));
                }
                offer.reply(this, distanceBid);
                break;
            default:
                log("CFP Message Content not understood");
                break;
                
        }
    }    
    
    private int studyDistance(Cell buildingFire, int maxDist) {
        //study distance through graph
        Graph graph = getGame().getGraph();
        Path path = graph.computeOptimumPath(getCurrentPosition(), buildingFire, maxDist);
        if(path==null){
            return -1;
        }else{
            return path.getDistance();
        }
    }

    private void actionTask() {                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     
        Path path = getGame().getGraph().computeOptimumPath(getCurrentPosition(), getCurrentTargetCell(),18);
        if(path.getDistance()==0){//ACTION
            int burned = ((BuildingCell)getCurrentTargetCell()).getBurnedRatio();
            AgentAction nextAction = new AgentAction(getAID(), getCurrentPosition());
            if(burned>5){
                //action+stay
                nextAction.setAction(getCurrentTargetCell(), 1);
            }else{
                Cell actualCell = getCurrentTargetCell();
                pollCurrentTargetCell();
                nextAction.setAction(actualCell, 1);
                if(getTargetCell().isEmpty()){
                    //position should be based on distribution...
                }else{
                    // movement based on path...
                    //if path.distance == 0 then dont move
                    Graph graph = getGame().getGraph();
                    path = graph.computeOptimumPathUnconstrained(getCurrentPosition(), getCurrentTargetCell());
                    if(path.getDistance()!=0){
                        nextAction.setPosition(path.getNextCellInPath());
                    }
                }
            }
            endTurn(nextAction);
        }else{//MOVING
            AgentAction nextAction = new AgentAction(getAID(), path.getNextCellInPath());
            endTurn(nextAction);
        }
    }

    private void dummyTask() {
        AgentAction nextAction = new AgentAction(getAID(), getCurrentPosition());
        endTurn(nextAction);
    }
}
