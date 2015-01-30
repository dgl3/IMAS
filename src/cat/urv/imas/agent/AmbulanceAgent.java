/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent;

import static cat.urv.imas.agent.ImasAgent.OWNER;

import cat.urv.imas.agent.communication.util.AIDUtil;
import cat.urv.imas.agent.communication.util.KeyValue;
import cat.urv.imas.agent.communication.util.MessageCreator;
import cat.urv.imas.behaviour.ambulance.InformBehaviour;
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
     * Game settings in use.
     */
    private GameSettings game;
    
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
                        default:
                            log("Unsupported message received.");
                    }
                }
                block(); // Confirm. Apparently 'just' schedules next execution. 'Generally all action methods should end with a call to block() or invoke it before doing return.'
            };
        };
    }

    private void handleCFP(ACLMessage msg) {
        // TODO: For testing call this to let ambulance initiate auction.
        ACLMessage proxy = MessageCreator.createProxy(hospitalCoordinatorAgent, MessageContent.AMBULANCE_AUCTION, null);
        send(proxy);
    }

    private void handleInform(ACLMessage msg) {
        KeyValue<String, Object> content = getMessageContent(msg);

        switch(content.getKey()) {
            case MessageContent.AMBULANCE_AUCTION:
                AID targetHospital = (AID)content.getValue();

                Cell targetCell = game.getAgentList().get(AgentType.HOSPITAL).get(AIDUtil.getLocalId(targetHospital));

                log("I will go to: " + targetCell);
                break;
            case MessageContent.SEND_GAME:
                manageSendGame((GameSettings) content.getValue());
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

        // TODO: this is just a test for the movement, all of this will be changed:

        Cell cPosition = getCurrentPosition();
        int[] nextPosition = new int[2];
        nextPosition[0] = cPosition.getRow();
        nextPosition[1] = cPosition.getCol() + 1;

        Cell[][] map = getGame().getMap();
        if (!(map[nextPosition[0]][nextPosition[1]] instanceof StreetCell)) {
            nextPosition[1] = cPosition.getCol() - 1;
        }

        AgentAction nextAction = new AgentAction(getLocalName(), nextPosition);

        endTurn(nextAction);
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
        log("Position updated: " + this.currentPosition.getRow() + "," + this.currentPosition.getCol() + "");
    }
    
    /**
     * Updates the loading speed of the ambulance from the game settings
     */
    public void updateLoadingSpeed() {
        this.loadingSpeed = this.game.getAmbulanceLoadingSpeed();
        log("Loading speed updated: " + this.loadingSpeed);
    }
    
    /**
     * Updates the ambulance capacity from the game settings
     */
    public void updateAmbulanceCapacity() {
        this.ambulanceCapacity = this.game.getPeoplePerAmbulance();
        log("Capacity updated: " + this.ambulanceCapacity);
    }
    
    public void endTurn(AgentAction nextAction) {
        ACLMessage gameinformRequest = new ACLMessage(ACLMessage.INFORM);
        gameinformRequest.clearAllReceiver();
        gameinformRequest.addReceiver(this.hospitalCoordinatorAgent);
        gameinformRequest.setProtocol(InteractionProtocol.FIPA_REQUEST);
        log("Inform message to agent");
        try {
            //gameinformRequest.setContent(MessageContent.SEND_GAME);
            Map<String,AgentAction> content = new HashMap<>();
            content.put(MessageContent.END_TURN, nextAction);
            gameinformRequest.setContentObject((Serializable) content);
            log("Inform message content: " + MessageContent.END_TURN);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        InformBehaviour gameInformBehaviour = new InformBehaviour(this, gameinformRequest);
        this.addBehaviour(gameInformBehaviour);
    }
}
