/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;

import cat.urv.imas.agent.communication.auction.Item;
import cat.urv.imas.agent.communication.auction.Offer;
import cat.urv.imas.agent.communication.util.KeyValue;
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
import jade.lang.acl.UnreadableException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        /* ** Very Important Line (VIL) ***************************************/
        this.setEnabledO2ACommunication(true, 1);
        /* ********************************************************************/

        // Register the agent to the DF
        ServiceDescription sd1 = new ServiceDescription();
        sd1.setType(AgentType.HOSPITAL.toString());
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
        KeyValue<String, Object> content = getMessageContent(msg);

        switch( content.getKey() ) {
            case MessageContent.AMBULANCE_AUCTION_BID_ACCEPTED:
                // TODO: Update Status!
                System.out.println("TODO: Update Status");

                break;
            default:
                log("Message Content not understood");
                break;
        }
    }

    private void handleRequests(ACLMessage msg) {
        KeyValue<String, Object> content = getMessageContent(msg);

        switch( content.getKey() ) {
            case MessageContent.AMBULANCE_AUCTION_BID_REQUEST:
                handleBidRequest((Offer)content.getValue());
                break;
            default:
                log("Message Content not understood");
                break;
        }
    }

    private void handleBidRequest(Offer offer) {
        Item item = offer.getItem();
        Float bid = item.getLoad()+0.1f;

        offer.reply(this, bid);
    }

    private void handleInform(ACLMessage msg) {
        KeyValue<String, Object> content = getMessageContent(msg);

        switch( content.getKey() ) {
            case MessageContent.SEND_GAME:
                setGame((GameSettings) content.getValue());
                log("Game updated");
                updatePosition();
                updateRecoveryTime();
                updateMaxCapacity();

                // TODO: this is just a test for the movement, all of this will be changed:

                break;
            default:
                log("Message Content not understood");
                break;
        }

    }

    private KeyValue<String, Object> getMessageContent(ACLMessage msg){
        KeyValue<String, Object> keyValue = null;

        try {
            Map<String,Object> contentObject = (Map<String,Object>) msg.getContentObject();
            String content = contentObject.keySet().iterator().next();
            keyValue = new KeyValue<>(content, contentObject.get(content));
        } catch (UnreadableException ex) {
            Logger.getLogger(FiremenCoordinatorAgent.class.getName()).log(Level.SEVERE, null, ex);
        }

        return keyValue;
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
        int hospitalNumber = Integer.valueOf(this.getLocalName().substring(this.getLocalName().length() - 1));
        this.currentPosition = this.game.getAgentList().get(AgentType.HOSPITAL).get(hospitalNumber);
        log("Position updated: " + this.currentPosition.getRow() + "," + this.currentPosition.getCol() + "");
    }
    
    /**
     * Updates the maximum capacity of the hospital from the game settings
     */
    public void updateMaxCapacity() {
        int hospitalNumber = Integer.valueOf(this.getLocalName().substring(this.getLocalName().length() - 1));
        this.hospitalMaxCapacity = this.game.getHospitalCapacities()[hospitalNumber];
        log("Maximum capacity set at: " + this.hospitalMaxCapacity);
    }
    
    /**
     * Updates the recovery time from the game settings
     */
    public void updateRecoveryTime() {
        this.recoveryTime = this.game.getStepsToHealth();
        log("Recovery time updated: " + this.recoveryTime);
    }
}
