/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;

import cat.urv.imas.agent.communication.auction.Item;
import cat.urv.imas.agent.communication.auction.Offer;
import cat.urv.imas.agent.communication.util.AIDUtil;
import cat.urv.imas.agent.communication.util.KeyValue;
import cat.urv.imas.agent.communication.util.MessageCreator;
import cat.urv.imas.graph.Path;
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
public class HospitalAgent extends ImasAgent{
    
    /**
     * Hospital position
     */
    private Cell currentPosition;
    
    /**
     * Game settings in use.
     */
    private GameSettings game;
    
    /**
     * Hospital maximum capacity
     */
    private int hospitalMaxCapacity;
    
    /**
     * Hospital recovery time
     */
    private int recoveryTime;
    
    /**
     * Coordinator agent id.
     */
    private AID hospitalCoordinatorAgent;
    
    public HospitalAgent() {
        super(AgentType.HOSPITAL);
    }
    
    @Override
    protected void setup() {
        registerToDF();

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
                        case ACLMessage.REQUEST:
                            handleRequests(msg);
                            break;
                        case ACLMessage.ACCEPT_PROPOSAL:
                            handleAcceptProposal(msg);
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
        KeyValue<String, Offer> content = getMessageContent(msg);

        switch( content.getKey() ) {
            case MessageContent.AMBULANCE_AUCTION:
                // TODO: Update Status!
                Offer offer = content.getValue();

                ACLMessage bidRequestMsg = MessageCreator.createMessage(ACLMessage.CONFIRM, offer.getAuctioneer(), MessageContent.AMBULANCE_AUCTION, offer);
                send(bidRequestMsg);

                break;
            default:
                log("Message Content not understood");
                break;
        }
    }

    private void handleRequests(ACLMessage msg) {
        KeyValue<String, Offer> content = getMessageContent(msg);

        switch( content.getKey() ) {
            case MessageContent.AMBULANCE_AUCTION:
                handleBidRequest(content.getValue());
                break;
            default:
                log("Message Content not understood");
                break;
        }
    }

    private void handleBidRequest(Offer offer) {
        errorLog("--> Processing bid.");
        Item item = offer.getItem();
        Cell myPos = getGame().getAgentList().get(getType()).get(AIDUtil.getLocalId(getAID()));

        errorLog("MyPos: " + myPos);
        errorLog("ToPos: " + item.getPosition());

        Path p = getGame().getGraph().computeOptimumPath(item.getPosition(), myPos, 100);
        float bid = p==null?Integer.MAX_VALUE:p.getDistance();


        errorLog("--> Sending bid.");
        offer.reply(this, bid);
    }

    private void handleInform(ACLMessage msg) {
        KeyValue<String, Object> content = getMessageContent(msg);

        switch( content.getKey() ) {
            case MessageContent.SEND_GAME:
                setGame((GameSettings) content.getValue());
                updatePosition();
                updateRecoveryTime();
                updateMaxCapacity();

                sendGameUpdateConfirmation(hospitalCoordinatorAgent);

                break;
            default:
                log("Message Content not understood");
                break;
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
        int hospitalNumber = AIDUtil.getLocalId( getAID() );
        this.currentPosition = this.game.getAgentList().get(AgentType.HOSPITAL).get(hospitalNumber);
    }
    
    /**
     * Updates the maximum capacity of the hospital from the game settings
     */
    public void updateMaxCapacity() {
        int hospitalNumber = AIDUtil.getLocalId(getAID());
        this.hospitalMaxCapacity = this.game.getHospitalCapacities()[hospitalNumber];
    }
    
    /**
     * Updates the recovery time from the game settings
     */
    public void updateRecoveryTime() {
        this.recoveryTime = this.game.getStepsToHealth();
    }
}
