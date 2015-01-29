/**
 *  IMAS base code for the practical work.
 *  Copyright (C) 2014 DEIM - URV
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cat.urv.imas.agent;

import cat.urv.imas.behaviour.central.InformBehaviour;
import cat.urv.imas.behaviour.central.RequestResponseBehaviour;
import cat.urv.imas.constants.AgentNames;
import cat.urv.imas.graph.Graph;
import cat.urv.imas.gui.GraphicInterface;
import cat.urv.imas.map.BuildingCell;
import cat.urv.imas.map.Cell;
import cat.urv.imas.map.CellType;
import cat.urv.imas.map.StreetCell;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.InfoAgent;
import cat.urv.imas.onthology.InitialGameSettings;
import cat.urv.imas.onthology.MessageContent;
import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPANames.InteractionProtocol;
import jade.lang.acl.*;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Central agent that controls the GUI and loads initial configuration settings.
 * TODO: You have to decide the onthology and protocol when interacting among
 * the Coordinator agent.
 */
public class CentralAgent extends ImasAgent {

    /**
     * GUI with the map, central agent log and statistics.
     */
    private GraphicInterface gui;
    /**
     * Game settings. At the very beginning, it will contain the loaded
     * initial configuration settings.
     */
    private GameSettings game;
    /**
     * The Coordinator agent with which interacts sharing game settings every
     * round.
     */
    private AID coordinatorAgent;
    
    /**
     * List of active fires
     */
    private List<Cell> activeFires;
    
    /**
     * Random Number Generator
     */
    private Random RNG;
    
    private Graph graph;

    /**
     * Builds the Central agent.
     */
    public CentralAgent() {
        super(AgentType.CENTRAL);
    }

    /**
     * A message is shown in the log area of the GUI, as well as in the 
     * stantard output.
     *
     * @param log String to show
     */
    @Override
    public void log(String log) {
        if (gui != null) {
            gui.log(getLocalName()+ ": " + log + "\n");
        }
        super.log(log);
    }
    
    /**
     * An error message is shown in the log area of the GUI, as well as in the 
     * error output.
     *
     * @param error Error to show
     */
    @Override
    public void errorLog(String error) {
        if (gui != null) {
            gui.log("ERROR: " + getLocalName()+ ": " + error + "\n");
        }
        super.errorLog(error);
    }

    /**
     * Gets the game settings.
     *
     * @return game settings.
     */
    public GameSettings getGame() {
        return this.game;
    }
    
    /**
     * Agent setup method - called when it first come on-line. Configuration of
     * language to use, ontology and initialization of behaviours.
     */
    @Override
    protected void setup() {
        
        /* ** Very Important Line (VIL) ************************************* */
        this.setEnabledO2ACommunication(true, 1);

        // 1. Register the agent to the DF
        ServiceDescription sd1 = new ServiceDescription();
        sd1.setType(AgentType.CENTRAL.toString());
        sd1.setName(getLocalName());
        sd1.setOwnership(OWNER);
        
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.addServices(sd1);
        dfd.setName(getAID());
        try {
            DFService.register(this, dfd);
            log("Registered to the DF");
        } catch (FIPAException e) {
            System.err.println(getLocalName() + " failed registration to DF [ko]. Reason: " + e.getMessage());
            doDelete();
        }

        // 2. Load game settings.
        this.game = InitialGameSettings.load("game.settings");
        log("Initial configuration settings loaded");

        //Creates the graph corresponding to the map cells
        this.graph = new Graph(game);
        
        
        // 3. Load GUI
        try {
            this.gui = new GraphicInterface(game);
            gui.setVisible(true);
            log("GUI loaded");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // 4. Create other agents
        jade.wrapper.AgentContainer cc = getContainerController();
        
        UtilsAgents.createAgent(cc, "hospCoord", "cat.urv.imas.agent.HospitalCoordinatorAgent", null);
        UtilsAgents.createAgent(cc, "firemenCoord", "cat.urv.imas.agent.FiremenCoordinatorAgent", null);
        
        Map<AgentType, List<Cell>> agentList = this.game.getAgentList();
        
        for (Map.Entry<AgentType, List<Cell>> entry : agentList.entrySet()) {
            log(entry.getKey().toString() +" -> " + entry.getValue().size());
            switch (entry.getKey().toString()) {
                case "HOSPITAL":
                    for (int i=0;i<entry.getValue().size();i++) {
                        UtilsAgents.createAgent(cc, AgentNames.hospital + i, "cat.urv.imas.agent.HospitalAgent", null);
                    }
                    break;
                /*case "PRIVATE_VEHICLE":  
                        break;*/
                case "FIREMAN":
                    for (int i=0;i<entry.getValue().size();i++) {
                        UtilsAgents.createAgent(cc, AgentNames.fireman + i, "cat.urv.imas.agent.FiremanAgent", null);
                    }
                    break;
                case "AMBULANCE":
                    for (int i=0;i<entry.getValue().size();i++) {
                        UtilsAgents.createAgent(cc, AgentNames.ambulance + i, "cat.urv.imas.agent.AmbulanceAgent", null);
                    }
                break;
            }
        }

        // search CoordinatorAgent
        ServiceDescription searchCriterion = new ServiceDescription();
        searchCriterion.setType(AgentType.COORDINATOR.toString());
        this.coordinatorAgent = UtilsAgents.searchAgent(this, searchCriterion);
        // searchAgent is a blocking method, so we will obtain always a correct AID
        
        /*
        // add behaviours
        // we wait for the initialization of the game
        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchProtocol(InteractionProtocol.FIPA_REQUEST), MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

        
        
        this.addBehaviour(new RequestResponseBehaviour(this, mt));

        // Setup finished. When the last inform is received, the agent itself will add
        // a behaviour to send/receive actions
        */
        this.activeFires = new ArrayList<>();
        this.RNG = new Random((int)this.game.getSeed());
        
        
        this.addBehaviour(newListenerBehaviour());
        this.newTurn();
    }
    
    public void updateGUI() {
        System.out.println("CENTRAL AGENT:" + this.game.get(2, 2).toString());
        this.gui.updateGame();
    }
    
    /**
     * Method to send the necessary messages to start a new turn and to wait 
     * for the end turn message from the children agents
     */
    private void newTurn() {
        // Central agent actively sends game info at the start of each turn
        
        // TODO: generate new fires acording to probability
        if (true) {
            Cell fire = this.generateFire();
            
            this.game.setNewFire(fire);
        } else {
            this.game.setNewFire(null);
        }
        
        this.sendGame();
        
    }
    
    /**
     * Method for the central agent to check for collisions on any of the agents
     * capable of moving
     */
    private void checkMovementCollisions(List<AgentAction> agentActions) {
        // TODO: this will be a dummy method for now
        this.endTurn(agentActions);
    }
    
    /**
     * Method for the central agent to finish the current turn. It updates
     * the map with the turns movement and starts a new turn
     */
    private void endTurn(List<AgentAction> agentActions) {
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(CentralAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        Scanner in = new Scanner(System.in);
        this.log("Press Enter for New Turn");
        String s = in.nextLine();
        in.close();
        this.log("NEW TURN");
        
        Cell[][] currentMap = this.game.getMap();
        
        int act[] = new int[2];
        act[0] = 8;
        act[1] = 3;
        agentActions.get(0).setAction(act);
        
        List<Cell> modifiedFires = this.performAgentActions(currentMap, agentActions);
        log("HEEEEEEEEEEERRRRRRRRRRRREEEEEEEEEEEEE");
        log(modifiedFires.toString());
        this.updateFires(modifiedFires);
        
        this.updateAgentMovements(agentActions);
        
        //this.gui.showGameMap(emptyMap);
        this.gui.showGameMap(this.game.getMap());
        //this.newTurn();
    }

    private void sendGame() {
        ACLMessage gameinformRequest = new ACLMessage(ACLMessage.INFORM);
        gameinformRequest.clearAllReceiver();
        gameinformRequest.addReceiver(this.coordinatorAgent);
        gameinformRequest.setProtocol(InteractionProtocol.FIPA_REQUEST);
        log("Inform message to agent");
        try {
            //gameinformRequest.setContent(MessageContent.SEND_GAME);
            Map<String,GameSettings> content = new HashMap<>();
            content.put(MessageContent.SEND_GAME, this.game);
            gameinformRequest.setContentObject((Serializable) content);
            log("Inform message content: " + MessageContent.SEND_GAME);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        InformBehaviour gameInformBehaviour = new InformBehaviour(this, gameinformRequest);
        this.addBehaviour(gameInformBehaviour);
    }
    
    private CyclicBehaviour newListenerBehaviour(){
        return new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg;
                while ((msg = receive()) != null) {
                    switch (msg.getPerformative()){
                        case ACLMessage.INFORM:
                            handleInform(msg);
                            break;
                        default:
                            log("Unsupported message received.");
                    }
                } 
                block();
            };
        };
    }
    
    /**
     * Handle new incoming INFORM message
     */
    private void handleInform(ACLMessage msg) {
        CentralAgent agent = this;
        Map<String,Object> contentObject;
        try {
            contentObject = (Map<String,Object>) msg.getContentObject();
            String content = contentObject.keySet().iterator().next();
            
            switch(content) {
                case MessageContent.END_TURN:
                    List<AgentAction> finishedAgents = new ArrayList<>();
                    finishedAgents.addAll((List<AgentAction>) contentObject.get(content));
                    //List<AID> agents = (List<AID>) msg.getContentObject();
                    this.checkMovementCollisions(finishedAgents);
                    break;
                default:
                    agent.log("Message Content not understood");
                    break;
            }
        } catch (UnreadableException ex) {
            Logger.getLogger(CoordinatorAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private Cell generateFire() {
        List<Cell> buildings = this.game.getClearBuildings();
        if (buildings.size() > 0) {
            int fireIndex = this.RNG.nextInt(buildings.size());
            return buildings.get(fireIndex);
        } else {
            return null;
        }
    }
    
    private List<Cell> performAgentActions(Cell[][] currentMap, List<AgentAction> actions) {
        
        List<Cell> modifiedCells = new ArrayList<>();
        
        for (AgentAction action : actions) {
            if (action.hasAction()) {
                BuildingCell bc;
                log("HERE");
                log(action.actionPosition.toString());
                switch (action.getAgentType()) {
                    case FIREMAN:
                        bc = (BuildingCell)currentMap[action.actionPosition[0]][action.actionPosition[1]];
                        bc.updateBurnedRatio(this.game.getFireSpeed());
                        modifiedCells.add(bc);
                        break;
                    case AMBULANCE:
                        bc = (BuildingCell)currentMap[action.actionPosition[0]][action.actionPosition[1]];
                        bc.updateBurnedRatio(this.game.getFireSpeed());
                        modifiedCells.add(bc);
                        // TODO: perform acion depending on type of cell
                        break;
                }
            }
        }
        
        return modifiedCells;
    }
    
    private void updateFires(List<Cell> modifiedFires) {
        Cell[][] currentMap = this.game.getMap();
        
        List<Cell> currentFires = this.game.getBuildingsOnFire();
        currentFires.add(this.game.getNewFire());
        
        for (Cell bof : currentFires) {
            Boolean modified = false;
            for (Cell mbof : modifiedFires) {
                if (bof.getRow() == mbof.getRow() &&
                        bof.getCol() == mbof.getCol()) {
                    modified = true;
                }
            }
            if (!modified) {
                ((BuildingCell)currentMap[bof.getRow()][bof.getCol()]).updateBurnedRatio(-this.game.getFireSpeed());
            }
        }
    }
    
    private void updateAgentMovements(List<AgentAction> actions) {
        Cell[][] currentMap = this.game.getMap();
        
        for (Cell[] cl : currentMap) {
            for (Cell c : cl) {
                if (c instanceof StreetCell) {
                    StreetCell sc = (StreetCell)c;
                    try {
                        if (sc.isThereAnAgent()) {
                            sc.removeAgent();
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(CentralAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        
        Map<AgentType, List<Cell>> content = new HashMap<>();
        
        for (AgentAction action : actions) {
            Cell position = new StreetCell(action.nextPosition[0],action.nextPosition[1]);
            
            if (action.agentName.startsWith("fireman")) {
                if (content.get(AgentType.FIREMAN) == null) {
                    List<Cell> positions = new ArrayList<>();
                    positions.add(position);
                    content.put(AgentType.FIREMAN, positions);
                } else {
                    List<Cell> positions = new ArrayList<>();
                    positions.addAll(content.get(AgentType.FIREMAN));
                    positions.add(position);
                    content.put(AgentType.FIREMAN, positions);
                }
            } else {
                if (content.get(AgentType.AMBULANCE) == null) {
                    List<Cell> positions = new ArrayList<>();
                    positions.add(position);
                    content.put(AgentType.AMBULANCE, positions);
                } else {
                    List<Cell> positions = new ArrayList<>();
                    positions.addAll(content.get(AgentType.AMBULANCE));
                    positions.add(position);
                    content.put(AgentType.AMBULANCE, positions);
                }
            }
        }
        
        for (Map.Entry<AgentType, List<Cell>> entry : content.entrySet()) {
            for (Cell c : entry.getValue()) {
                StreetCell sc = (StreetCell)currentMap[c.getRow()][c.getCol()];
                try {
                    if (sc.isThereAnAgent()) {
                        sc.removeAgent();
                    }
                    sc.addAgent(new InfoAgent(entry.getKey()));
                } catch (Exception ex) {
                    Logger.getLogger(CentralAgent.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
