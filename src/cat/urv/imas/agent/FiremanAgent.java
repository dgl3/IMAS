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
import cat.urv.imas.graph.GraphUtils;
import cat.urv.imas.graph.Path;
import cat.urv.imas.map.BuildingCell;
import cat.urv.imas.map.Cell;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.MessageContent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import java.util.List;
import java.util.Map;

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

    private void handleInform(ACLMessage msg) {
        KeyValue<String, Object> content = getMessageContent(msg);
        switch(content.getKey()){
            case MessageContent.SEND_GAME:
                setGame((GameSettings) content.getValue());
                updatePosition();

                //setListOtherGraphs((Map<AID,Graph>)content.getValue());
                // confirm
                sendGameUpdateConfirmation(getParent());
                if(getGame().getNewFire()==null){
                    if(!getTargetCell().isEmpty()){
                        actionTask();
                    }else{
                        //distributionTask(null);
                        dummyTask();
                    }
                }
                break;

                /*
                //dani's method that return the graph gicen the position and the number
                int numActions = getTargetCell().size();
                Graph graph = getGame().getGraph();
                Graph clonedGraph = null;
                try{
                    if(numActions==0){
                        clonedGraph = GraphUtils.actionArea(graph, getCurrentPosition(), 18);
                    }else if(numActions==1){
                        int turnsExtinguish = 18-((((BuildingCell)getCurrentTargetCell()).getBurnedRatio()/5)-1);
                        clonedGraph = GraphUtils.actionArea(graph, getCurrentPosition(), turnsExtinguish);
                    }else{
                        int turnsExtinguish = (((BuildingCell)getCurrentTargetCell()).getBurnedRatio()/5)-1;
                        Path path = graph.computeOptimumPath(getCurrentPosition(), getTargetCell().get(1), turnsExtinguish);
                        int turnsDist = path.getDistance();
                        clonedGraph = GraphUtils.actionArea(graph, path.getPath().get(path.getPath().size()-1).getCell(), turnsExtinguish+turnsDist);
                    }
                }catch(Exception e){
                    log("Cloned graph ERROR!!!");
                }
                ACLMessage graphmsg = MessageCreator.createConfirm(getParent(), MessageContent.ACTION_AREAS, clonedGraph);
                send(graphmsg);

                break;
                */
            /*
            case MessageContent.ACTION_AREAS:
                //TODO: 
                setListOtherGraphs((Map<AID,Graph>)content.getValue());
                // confirm
                sendGameUpdateConfirmation(getParent());
                if(getGame().getNewFire()==null){
                    if(!getTargetCell().isEmpty()){
                        actionTask();
                    }else{
                        //distributionTask(null);
                        dummyTask();
                    }
                }
                break;
                */
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
                    distanceBid = studyDistance(offer.getCell(), 18, Boolean.FALSE);
                }else if(getTargetCell().size()==1){
                    //take into account two possible tasks
                    int distanceToFire = studyDistance(getCurrentTargetCell(), 18, Boolean.FALSE);
                    int turnsExtinguish = ((BuildingCell)getCurrentTargetCell()).getBurnedRatio()/5;
                    log("Distance to my assigned fire: "+distanceToFire);
                    if(distanceToFire==0){
                        //take into account the number of turns needed to extniguish
                        distanceBid = studyDistance(offer.getCell(), 18-(turnsExtinguish-1), Boolean.FALSE);
                    }else if(distanceToFire>0){
                        //Take into account the distance to arrive and number of turns to extinguish when arrive
                        if(18-((turnsExtinguish-1)+distanceToFire*2)<1){
                            distanceBid = -1;
                        }else{
                            distanceBid = studyDistance(offer.getCell(), 18-((turnsExtinguish-1)+distanceToFire*2), Boolean.TRUE);
                        }
                    }
                }
                offer.reply(this, distanceBid);
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
            path = computeOptimumPath(getCurrentPosition(), buildingFire, maxDist);
        }

        if(path==null){
            return -1;
        }else{
            return path.getDistance();
        }
    }

    private void actionTask() {                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     
        Path path = computeOptimumPath(getCurrentPosition(), getCurrentTargetCell(),18);
        if(path != null) {
            if (path.getDistance() == 0) {//ACTION+POSSIBLE MOVEMENT
                int burned = ((BuildingCell) getCurrentTargetCell()).getBurnedRatio();
                AgentAction nextAction = new AgentAction(getAID(), getCurrentPosition());

                if (burned > 5 && burned < 100) {
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
            AgentAction nextAction = new AgentAction(getAID(), getCurrentPosition());
            endTurn(nextAction);
        }
    }

//    private void distributionTask(AgentAction nextAction) {
//        //Movement based on distribution
//        Graph graph = getGame().getGraph();
//        List<Cell> adjacent = graph.getAdjacentCells(getCurrentPosition());
//        int distribution = GraphUtils.distributionValue(getActionArea(), getForeignActionAreas());
//        Cell nextCell = getCurrentPosition();
//        for(Cell cell: adjacent){
//            try{
//                Graph graphadjacent = GraphUtils.actionArea(graph, cell, 18);
//                int distributionadja = GraphUtils.distributionValue(graphadjacent, getForeignActionAreas());
//                if(distributionadja>distribution){
//                    distribution = distributionadja;
//                    nextCell = cell;
//                }
//            }catch(Exception e){
//                log("Action area adjacent exception!");
//            }
//        }
//        if (nextAction==null){
//            nextAction = new AgentAction(getAID(), nextCell);
//        }else{
//            nextAction.setPosition(nextCell);
//        }
//        endTurn(nextAction);
//    }

    private void dummyTask(){
        AgentAction action = new AgentAction(getAID(), getCurrentPosition());
        endTurn(action);
    }

    private void setListOtherGraphs(Map<AID, Graph> map) {
        getForeignActionAreas().clear();
        for(AID fireman: map.keySet()){
            if(!fireman.equals(getAID())){
                addForeignActionAreas(map.get(fireman));
            }else{
                setActionArea(map.get(fireman));
            }
        }
    }
}
