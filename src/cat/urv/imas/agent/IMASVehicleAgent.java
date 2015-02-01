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

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Agent abstraction used in this practical work.
 * It gathers common attributes and functionality from all agents.
 */
public class IMASVehicleAgent extends ImasAgent {

    /**
     * Current agent position
     */
    private Cell lastPosition;

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
    private Cell targetCell;

    /**
     * Creates the agent.
     *
     * @param type type of agent to set.
     */
    public IMASVehicleAgent(AgentType type) {
        super(type);
    }

    /**
     * Updates the new current position from the game settings
     */
    public void updatePosition() {
        int ambulanceNumber = AIDUtil.getLocalId(this.getAID());
        setLastPosition( getCurrentPosition() );
        setCurrentPosition(getGame().getAgentList().get(getType()).get(ambulanceNumber));

        if( currentPosition.equals(lastPosition) ){
            System.err.println("GET OUT OF MY WAY!!!");
        }
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

    public Cell getTargetCell() {
        return targetCell;
    }

    public void setTargetCell(Cell targetCell) {
        this.targetCell = targetCell;
    }

    public Cell getLastPosition() {
        return lastPosition;
    }

    public void setLastPosition(Cell lastPosition) {
        this.lastPosition = lastPosition;
    }
}
