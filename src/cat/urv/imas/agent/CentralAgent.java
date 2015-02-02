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

import cat.urv.imas.agent.communication.util.AIDUtil;
import cat.urv.imas.agent.communication.util.KeyValue;
import cat.urv.imas.agent.communication.util.MessageCreator;
import cat.urv.imas.constants.AgentNames;
import cat.urv.imas.graph.Graph;
import cat.urv.imas.gui.ControlWindow;
import cat.urv.imas.gui.GraphicInterface;
import cat.urv.imas.map.BuildingCell;
import cat.urv.imas.map.Cell;
import cat.urv.imas.map.HospitalCell;
import cat.urv.imas.map.StreetCell;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.InfoAgent;
import cat.urv.imas.onthology.InitialGameSettings;
import cat.urv.imas.onthology.MessageContent;
import cat.urv.imas.statistics.FireStatistics;
import cat.urv.imas.statistics.GameStatistics;
import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Central agent that controls the GUI and loads initial configuration settings.
 * TODO: You have to decide the onthology and protocol when interacting among
 * the Coordinator agent.
 */
public class CentralAgent extends ImasAgent {

    Scanner in;
    
    /**
     * Game Statistics
     */
    private GameStatistics statistics;
    
    /**
     * Number of the current turn
     */
    private int turn = 0;
    
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

    /**
     * Indicates wether the central agent is ready for the next turn
     */
    private boolean readyForNextTurn;

    /**
     * GUI Controller for the central agent
     */
    private ControlWindow controllerWindow;
    
    /**
     * Indicates if the game is running by itself
     */
    private boolean autoPlay;

    /**
     * Builds the Central agent.
     */
    public CentralAgent() {
        super(AgentType.CENTRAL);
    }
    
    /**
     * List of private vehicles
     */
    private List<PrivateVehicle> privateVehicles;

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
        registerToDF();

        // 2. Load game settings.
        this.game = InitialGameSettings.load("game.settings");
        this.game.initializeAmbulanceCapacities();
        Graph graph = new Graph(this.game);
        this.game.updateGraph(graph);
        log("Initial configuration settings loaded");
        
        this.RNG = new Random((int)this.game.getSeed());
        
        
        // 3. Load GUI
        try {
            this.gui = new GraphicInterface(this, game);
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
        this.privateVehicles = new ArrayList<>();
        for (Map.Entry<AgentType, List<Cell>> entry : agentList.entrySet()) {
            log(entry.getKey().toString() +" -> " + entry.getValue().size());
            switch (entry.getKey().toString()) {
                case "HOSPITAL":
                    for (int i=0;i<entry.getValue().size();i++) {
                        UtilsAgents.createAgent(cc, AgentNames.hospital + i, "cat.urv.imas.agent.HospitalAgent", null);
                    }
                    break;
                case "PRIVATE_VEHICLE":  
                    for (int i=0;i<entry.getValue().size();i++) {
                        this.privateVehicles.add(new PrivateVehicle("private" + i, entry.getValue().get(i), this.RNG, this.game));
                    }
                    break;
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
        this.in = new Scanner(System.in);
        this.activeFires = new ArrayList<>();
        
        this.statistics = new GameStatistics();
        
        this.addBehaviour(newListenerBehaviour());

        readyForNextTurn = true;
        autoPlay = false;

        this.newTurn();
    }
    
    /**
     * Method to send the necessary messages to start a new turn and to wait 
     * for the end turn message from the children agents
     */
    public void newTurn() {
        if( readyForNextTurn == true ){
            turn += 1;
            readyForNextTurn = false;
            if( controllerWindow != null ){
                controllerWindow.setReadyForNewTurn(false);
            }

            log("\n\n------------------ Turn No: " + turn + " ------------------\n");

            // TODO: generate new fires according to probability
            if (true) {
                Cell fire = this.generateFire();

                this.game.setNewFire(fire);

                this.statistics.newFire(fire, this.turn);
            } else {
                this.game.setNewFire(null);
            }

            this.sendGame();
        }else{
            errorLog("Not ready for next turn!");
        }
    }
    
    /**
     * Method for the central agent to check for collisions on any of the agents
     * capable of moving
     */

    private void checkMovementCollisions(Collection<AgentAction> agentActions, List<Cell> pva) {
        /*List<Boolean> agentCanPerformMovement = new ArrayList<>();
        for (AgentAction aa : agentActions) {
            agentCanPerformMovement.add(true);
        }
        List<Boolean> privateCanPerformMovement = new ArrayList<>();
        for (Cell c : pva) {
            privateCanPerformMovement.add(true);
        }*/
        this.game.emptyColisions();
        Map<String,Cell> colisions = new HashMap<>();
        
        Boolean thereAreChanges = true;
        
        while (thereAreChanges) {
            thereAreChanges = false;
            // Two agents want to go to the same cell
            for (int i=0; i<pva.size(); i++) {
                for (int j=0; j<pva.size(); j++) {
                    if (i != j && pva.get(i).getRow() == pva.get(j).getRow() &&
                            pva.get(i).getCol() == pva.get(j).getCol()) {
                        pva.set(i, this.privateVehicles.get(i).getCurrentPosition());
                        pva.set(j, this.privateVehicles.get(j).getCurrentPosition());
                        thereAreChanges = true;
                        colisions.put(this.privateVehicles.get(i).getLocalName(), this.privateVehicles.get(i).getCurrentPosition());
                        colisions.put(this.privateVehicles.get(j).getLocalName(), this.privateVehicles.get(j).getCurrentPosition());
                    }
                }
                for (AgentAction agent: agentActions) {
                    if (pva.get(i).getRow() == agent.nextPosition[0] &&
                            pva.get(i).getCol() == agent.nextPosition[1]) {
                        pva.set(i, this.privateVehicles.get(i).getCurrentPosition());
                        if (agent.agentAID.getLocalName().startsWith("fire")) {
                            agent.changeNextPosition(this.game.getAgentList().get(AgentType.FIREMAN).
                                    get(AIDUtil.getLocalId(agent.agentAID)));
                        } else {
                            agent.changeNextPosition(this.game.getAgentList().get(AgentType.AMBULANCE).
                                    get(AIDUtil.getLocalId(agent.agentAID)));
                        }
                        thereAreChanges = true;
                        colisions.put(agent.agentAID.getLocalName(), this.game.getMap()[agent.nextPosition[0]][agent.nextPosition[1]]);
                        colisions.put(this.privateVehicles.get(i).getLocalName(), this.privateVehicles.get(i).getCurrentPosition());
                    }
                }
            }
            for (AgentAction a : agentActions) {
                Cell cPos;
                if (a.agentAID.getLocalName().startsWith("fire")) {
                    cPos = this.game.getAgentList().get(AgentType.FIREMAN).
                            get(AIDUtil.getLocalId(a.agentAID));
                } else {
                    cPos = this.game.getAgentList().get(AgentType.AMBULANCE).
                            get(AIDUtil.getLocalId(a.agentAID));
                }
                for (AgentAction agent : agentActions) {
                    if (a != agent) {
                        Cell cPos2;
                        if (agent.agentAID.getLocalName().startsWith("fire")) {
                            cPos2 = this.game.getAgentList().get(AgentType.FIREMAN).
                                    get(AIDUtil.getLocalId(agent.agentAID));
                        } else {
                            cPos2 = this.game.getAgentList().get(AgentType.AMBULANCE).
                                    get(AIDUtil.getLocalId(agent.agentAID));
                        }
                        
                        if (a.nextPosition[0] == agent.nextPosition[0] &&
                                a.nextPosition[1] == agent.nextPosition[1]) {
                            a.changeNextPosition(cPos);
                            agent.changeNextPosition(cPos2);
                            thereAreChanges = true;
                            colisions.put(agent.agentAID.getLocalName(), cPos2);
                            colisions.put(a.agentAID.getLocalName(), cPos);
                        }
                    }
                }
            }
            // Two agents cross each other
            for (int i=0; i<pva.size(); i++) {
                for (int j=0; j<pva.size(); j++) {
                    if (i != j && this.privateVehicles.get(i).getCurrentPosition().getRow() == pva.get(j).getRow() &&
                            this.privateVehicles.get(i).getCurrentPosition().getCol() == pva.get(j).getCol() &&
                            pva.get(i).getRow() == this.privateVehicles.get(j).getCurrentPosition().getRow() &&
                            pva.get(i).getCol() == this.privateVehicles.get(j).getCurrentPosition().getRow()) {
                        pva.set(i, this.privateVehicles.get(i).getCurrentPosition());
                        pva.set(j, this.privateVehicles.get(j).getCurrentPosition());
                        thereAreChanges = true;
                        colisions.put(this.privateVehicles.get(i).getLocalName(), this.privateVehicles.get(i).getCurrentPosition());
                        colisions.put(this.privateVehicles.get(j).getLocalName(), this.privateVehicles.get(j).getCurrentPosition());
                    }
                }
            }
            for (AgentAction a : agentActions) {
                
                Cell cPos;
                if (a.agentAID.getLocalName().startsWith("fire")) {
                    cPos = this.game.getAgentList().get(AgentType.FIREMAN).
                            get(AIDUtil.getLocalId(a.agentAID));
                } else {
                    cPos = this.game.getAgentList().get(AgentType.AMBULANCE).
                            get(AIDUtil.getLocalId(a.agentAID));
                }
                
                for (int j=0; j<pva.size(); j++) {
                    if (a.nextPosition[0] == this.privateVehicles.get(j).getCurrentPosition().getRow() && 
                            a.nextPosition[1] == this.privateVehicles.get(j).getCurrentPosition().getCol() &&
                            cPos.getRow() == pva.get(j).getRow() &&
                            cPos.getRow() == pva.get(j).getCol()) {
                        pva.set(j, this.privateVehicles.get(j).getCurrentPosition());
                        a.changeNextPosition(cPos);
                        thereAreChanges = true;
                        colisions.put(a.agentAID.getLocalName(), this.game.getMap()[a.nextPosition[0]][a.nextPosition[1]]);
                        colisions.put(this.privateVehicles.get(j).getLocalName(), this.privateVehicles.get(j).getCurrentPosition());
                    }
                }
                for (AgentAction agent: agentActions) {
                    if (a != agent) {
                        Cell cPos2;
                        if (agent.agentAID.getLocalName().startsWith("fire")) {
                            cPos2 = this.game.getAgentList().get(AgentType.FIREMAN).
                                    get(AIDUtil.getLocalId(agent.agentAID));
                        } else {
                            cPos2 = this.game.getAgentList().get(AgentType.AMBULANCE).
                                    get(AIDUtil.getLocalId(agent.agentAID));
                        }
                        
                        if (a.nextPosition[0] == cPos2.getRow() &&
                                a.nextPosition[1] == cPos2.getCol() &&
                                cPos.getRow() == agent.nextPosition[0] &&
                                cPos.getCol() == agent.nextPosition[1]) {
                            a.changeNextPosition(cPos);
                            agent.changeNextPosition(cPos2);
                            thereAreChanges = true;
                            colisions.put(agent.agentAID.getLocalName(), cPos2);
                            colisions.put(a.agentAID.getLocalName(), cPos);
                        }
                    }
                }
            }
        }
        
        this.game.addColisions(colisions);
    }

    private void readyForNextTurn() {
        this.readyForNextTurn = true;

        if( controllerWindow != null ){
            controllerWindow.setReadyForNewTurn(true);
        }

        if( autoPlay ){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            newTurn();
        }
    }

    /**
     * Method for the central agent to finish the current turn. It updates
     * the map with the turns movement and starts a new turn
     */
    private void endTurn(Collection<AgentAction> agentActions, List<Cell> pva) {
        this.game.advanceTurn();
        
        Cell[][] map = this.game.getMap();
        
        List<Cell> modifiedFires = this.performAgentActions(agentActions);

        this.updateFires(modifiedFires);

        this.updateAgentMovements(agentActions, pva);

        List<Integer> currentOccupancy = new ArrayList<>();
        for (Cell c : this.game.getAgentList().get(AgentType.HOSPITAL)) {
            currentOccupancy.add(((HospitalCell)c).useRatio());
        }
        this.statistics.setNewTurnHospitalOccupancy(currentOccupancy);
        this.statistics.updatePeopleInRisk(this.game.getPeopleInRiskPercentage());

        this.gui.updateGame();
        this.gui.printNewStatistics(this.statistics.getCurrentStatistics());

        readyForNextTurn();
    }

    private void sendGame() {
        ACLMessage gameinformRequest = MessageCreator.createInform(coordinatorAgent, MessageContent.SEND_GAME, game);
        send(gameinformRequest);
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
            }
        };
    }
    
    /**
     * Handle new incoming INFORM message
     */
    private void handleInform(ACLMessage msg) {
        KeyValue<String, Object> content = getMessageContent(msg);
        switch(content.getKey()){
            case MessageContent.END_TURN:

                Collection<AgentAction> finishedAgents = Collections.unmodifiableCollection((List<AgentAction>)content.getValue());
                List<Cell> pva = movePrivateVehicles();
                checkMovementCollisions(finishedAgents, pva);
                endTurn(finishedAgents, pva);
                break;
            default:
                log("Message Content not understood");
                break;
        }
    }
    
    private List<Cell> movePrivateVehicles() {
        Cell[][] currentMap = this.game.getMap();
        
        List<Cell> privateVehiclesMovements = new ArrayList<>();
        
        for (PrivateVehicle pv : this.privateVehicles) {
            privateVehiclesMovements.add(pv.makeNewMovement(currentMap));
        }
        
        return privateVehiclesMovements;
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
    
    private List<Cell> performAgentActions(Collection<AgentAction> actions) {
        Cell[][] currentMap = this.game.getMap();
        List<Cell> modifiedCells = new ArrayList<>();
        
        for (AgentAction action : actions) {
            if (action.hasAction()) {
                
                switch (action.getAgentType()) {
                    case FIREMAN:
                        BuildingCell bc;
                        bc = (BuildingCell)currentMap[action.actionPosition[0]][action.actionPosition[1]];
                        bc.updateBurnedRatio(this.game.getFireSpeed());
                        FireStatistics fs = this.statistics.getActiveFireStatistics(bc);
                        if (fs != null) {
                            fs.updateBurnedRatio(bc.getBurnedRatio(), this.turn, bc.getNumberOfCitizens());
                        }
                        modifiedCells.add(bc);
                        break;
                    case AMBULANCE:
                        Cell c = currentMap[action.actionPosition[0]][action.actionPosition[1]];
                        if (c instanceof BuildingCell) {
                            BuildingCell BC = (BuildingCell)c;
                            if (!BC.isDestroyed()) {
                                int numAgent = AIDUtil.getLocalId(action.agentAID);
                                
                                int taken = BC.take(Math.min(this.game.getPeoplePerAmbulance()-
                                        this.game.getAmbulanceCurrentLoad(numAgent),this.game.getAmbulanceLoadingSpeed()));
                                
                                this.game.updateAmbulanceCurrentLoad(numAgent, taken);
                            }
                        } else if (c instanceof HospitalCell) {
                            HospitalCell hc = (HospitalCell)c;
                            int numAgent = AIDUtil.getLocalId(action.agentAID);
                            int signedIn = hc.signInPatients(Math.min(this.game.getAmbulanceCurrentLoad(numAgent), this.game.getAmbulanceLoadingSpeed()),
                                    this.game.getStepsToHealth());
                            
                            
                            this.game.updateAmbulanceCurrentLoad(numAgent, -signedIn);
                        }
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
                int newBurnedRatio = ((BuildingCell)currentMap[bof.getRow()][bof.getCol()]).getBurnedRatio();
                FireStatistics fs = this.statistics.getActiveFireStatistics(currentMap[bof.getRow()][bof.getCol()]);
                if (fs != null) {
                    fs.updateBurnedRatio(newBurnedRatio, this.turn, 
                            ((BuildingCell)currentMap[bof.getRow()][bof.getCol()]).getNumberOfCitizens());
                }
            }
        }
    }
    
    private void updateAgentMovements(Collection<AgentAction> actions, List<Cell> pva) {
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
        
        Map<AgentType, List<KeyValue<AID, Cell>>> content = new HashMap<>();

        for (AgentAction action : actions) {
            Cell position = new StreetCell(action.nextPosition[0],action.nextPosition[1]);
            KeyValue<AID, Cell> keyValue = new KeyValue<>(action.agentAID, position);

            if (action.agentAID.getLocalName().startsWith("fireman")) {
                if (content.get(AgentType.FIREMAN) == null) {
                    List<KeyValue<AID, Cell>> positions = new ArrayList<>();
                    positions.add(keyValue);
                    content.put(AgentType.FIREMAN, positions);
                } else {
                    List<KeyValue<AID, Cell>> positions = new ArrayList<>();
                    positions.addAll(content.get(AgentType.FIREMAN));
                    positions.add(keyValue);
                    content.put(AgentType.FIREMAN, positions);
                }
                int numAgent = Integer.valueOf(action.agentAID.getLocalName().substring(action.agentAID.getLocalName().length() - 1));
                this.game.getAgentList().get(AgentType.FIREMAN).set(numAgent, position);
            } else {
                if (content.get(AgentType.AMBULANCE) == null) {
                    List<KeyValue<AID, Cell>> positions = new ArrayList<>();
                    positions.add(keyValue);
                    content.put(AgentType.AMBULANCE, positions);
                } else {
                    List<KeyValue<AID, Cell>> positions = new ArrayList<>();
                    positions.addAll(content.get(AgentType.AMBULANCE));
                    positions.add(keyValue);
                    content.put(AgentType.AMBULANCE, positions);
                }
                int numAgent = AIDUtil.getLocalId(action.agentAID);
                this.game.getAgentList().get(AgentType.AMBULANCE).set(numAgent, position);
            }
        }
        
        for (Map.Entry<AgentType, List<KeyValue<AID, Cell>>> entry : content.entrySet()) {
            for (KeyValue<AID, Cell> c : entry.getValue()) {
                StreetCell sc = (StreetCell)currentMap[c.getValue().getRow()][c.getValue().getCol()];
                try {
                    if (sc.isThereAnAgent()) {
                        sc.removeAgent();
                    }
                    sc.addAgent(new InfoAgent(entry.getKey(), c.getKey()));
                } catch (Exception ex) {
                    Logger.getLogger(CentralAgent.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        int i=-1;
        for (Cell c : pva) {
            i += 1;
            StreetCell sc = (StreetCell)currentMap[c.getRow()][c.getCol()];
            try {
                if (sc.isThereAnAgent()) {
                    sc.removeAgent();
                }
                sc.addAgent(new InfoAgent(AgentType.PRIVATE_VEHICLE, null));
                this.privateVehicles.get(i).updateCurrentPosition(c);
                // TODO
            } catch (Exception ex) {
                Logger.getLogger(CentralAgent.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public ControlWindow getControllerWindow() {
        return controllerWindow;
    }

    public void setControllerWindow(ControlWindow controllerWindow) {
        this.controllerWindow = controllerWindow;
    }

    public int generateRandomNumber(int bound) {
        return this.RNG.nextInt(bound);
    }

    public void setAutoPlay(boolean autoPlay) {
        this.autoPlay = autoPlay;
        newTurn();
    }

    public boolean isAutoPlay() {
        return autoPlay;
    }

    public boolean isReadyForNextTurn() {
        return readyForNextTurn;
    }
}
