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
import cat.urv.imas.map.Cell;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.MessageContent;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import java.util.ArrayList;
import java.util.List;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Agent abstraction used in this practical work.
 * It gathers common attributes and functionality from all agents.
 */
public class IMASVehicleAgent extends ImasAgent {

    /**
     * Last action sent to the parent
     */
    private AgentAction lastAction;

    /**
     * Current agent position
     */
    private Cell currentPosition;

    /**
     * Game settings in use.
     */
    private GameSettings game;

    /**
     * The cell the agent wants to move to.
     */
    private List<Cell> targetCell;

    /**
     * Agent this one reports to
     */
    private AID parent;

    /**
     * Creates the agent.
     *
     * @param type type of agent to set.
     */
    public IMASVehicleAgent(AgentType type) {
        super(type);
        targetCell = new ArrayList<>();
    }

    public void endTurn(AgentAction nextAction) {
        if( inCollision() ){
            AgentType agentType = getVehicleTypeOfCollision();
            errorLog("GET OUT OF MY WAY!");
            errorLog("Colliding with: " + agentType);


            ACLMessage msg = MessageCreator.createInform(parent, MessageContent.END_TURN, nextAction);
            send(msg);
        }else {
            lastAction = nextAction;
            ACLMessage msg = MessageCreator.createInform(parent, MessageContent.END_TURN, nextAction);
            send(msg);
        }
    }

    private AgentType getVehicleTypeOfCollision() {
        return AgentType.PRIVATE_VEHICLE;
    }

    /**
     * Updates the new current position from the game settings
     */
    public void updatePosition() {
        int ambulanceNumber = AIDUtil.getLocalId(this.getAID());
        setCurrentPosition(getGame().getAgentList().get(getType()).get(ambulanceNumber));
    }

    public Cell getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(Cell currentPosition) {
        this.currentPosition = currentPosition;
    }

    public GameSettings getGame() {
        return game;
    }

    public void setGame(GameSettings game) {
        this.game = game;
    }

    public List<Cell> getTargetCell() {
        return targetCell;
    }
    
    public void addTargetCell(Cell cell){
        targetCell.add(cell);
    }
    
    public void pollCurrentTargetCell(){
        if(!targetCell.isEmpty()){
            targetCell.remove(0);
        }
    }
    
    public Cell getCurrentTargetCell(){
        Cell cell = targetCell.get(0);
        return game.get(cell.getRow(), cell.getCol());
    }

    public AID getParent() {
        return parent;
    }

    public void setParent(AID parent) {
        this.parent = parent;
    }

    private boolean inCollision(){
        if(lastAction == null || lastAction.actionPosition == null || currentPosition == null) return false;

        if( lastAction.actionPosition[0] == currentPosition.getRow()
            && lastAction.actionPosition[1] == currentPosition.getCol() ){
            return false;
        }else {
            return true;
        }
    }
}
