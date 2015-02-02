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
import cat.urv.imas.agent.communication.util.MessageCreator;
import cat.urv.imas.constants.Direction;
import cat.urv.imas.graph.Path;
import cat.urv.imas.map.Cell;
import cat.urv.imas.map.StreetCell;
import cat.urv.imas.onthology.GameSettings;
import cat.urv.imas.onthology.MessageContent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.List;

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

    private Cell cellToAvoid;

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

//        if( inCollision() ){
//            lastMoveWasAvoid = true;
//            Direction direction = getIntendedDirection(lastAction, currentPosition);
//
//            List<String> collisions = getGame().getColisionsByName(getLocalName());
//
//            System.err.println("In Collision");
//
//            Cell escapePos = getEscapePosition(direction, collisions);
//
//            ACLMessage msg = MessageCreator.createInform(parent, MessageContent.END_TURN, new AgentAction(getAID(), escapePos));
//            send(msg);
//        }else {
            //lastMoveWasAvoid = false;
            lastAction = nextAction;
            ACLMessage msg = MessageCreator.createInform(parent, MessageContent.END_TURN, nextAction);
            send(msg);
        //}
    }

//    private Direction getIntendedDirection(AgentAction lastAction, Cell currentPosition) {
//        int horizontal = lastAction.nextPosition[0]-currentPosition.getRow();
//        int vertical = lastAction.nextPosition[1]-currentPosition.getCol();
//
//        if( horizontal != 0 ){
//            if( horizontal > 0 ){ // This agent want to move up
//                return Direction.SOUTH;
//            }else{ // This agent want to move down
//                return Direction.NORTH;
//            }
//        }else{
//            if( vertical > 0 ){ // This agent want to move right
//                return Direction.EAST;
//            }else{ // This agent want to move left
//                return Direction.WEST;
//            }
//        }
//    }

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
        if( lastAction == null || lastAction.nextPosition == null || currentPosition == null) return false;

        if( lastAction.nextPosition[0] == currentPosition.getRow()
            && lastAction.nextPosition[1] == currentPosition.getCol() ){
            return false;
        }else {
            return true;
        }
    }

//    private Cell getEscapePosition(Direction direction, List<String> collisions){
//
//        // Collided with idle agent or car
//        if ( collisions.isEmpty() ){
//            errorLog("Collided with idle agent or car");
//            return getNewAllowedRandomCell();
//        }
//
//        // Collided with other moving agent
//        String otherName = collisions.get(0);
//        AgentType myType = getType();
//        AgentType otherType = AIDUtil.getType(otherName);
//
//        int myRank = rank(myType);
//        int otherRank = rank(otherType);
//
//        if(  myRank < otherRank ){
//            return getNewAllowedRandomCell();
//        }else if( myRank == otherRank ){
//            if ( direction == direction.NORTH || direction == direction.WEST ){
//                return getNewAllowedRandomCell();
//            }
//        }

        // The other agent has to move
//        return new StreetCell( lastAction.nextPosition[0], lastAction.nextPosition[1]);
//    }

    private boolean mustAvoid(List<String> collisions){
        if ( collisions.isEmpty() ){
            errorLog("Collided with idle agent or car");
            return true;
        }

        // Collided with other moving agent
        String otherName = collisions.get(0);
        AgentType myType = getType();
        AgentType otherType = AIDUtil.getType(otherName);

        int myRank = rank(myType);
        int otherRank = rank(otherType);

        if(  myRank < otherRank ){
            return true;
        }else if( myRank == otherRank ){
            if( AIDUtil.getLocalId(getLocalName()) < AIDUtil.getLocalId(otherName)){
                return true;
            }
        }

        // The other agent has to move
        return false;
    }

    private int rank(AgentType type) {
        switch ( type ){
            case FIREMAN:
                return 2;
            case AMBULANCE:
                return 1;
            case CAR:
                return 0;
            default:
                throw new IllegalArgumentException("No rank defined for this agent type.");
        }
    }

//    public Cell getNewAllowedRandomCell() {
//
//        Cell newCell;
//        do {
//            int row = currentPosition.getRow()+CentralAgent.getRNG().nextInt(2);
//            int col = currentPosition.getCol()+CentralAgent.getRNG().nextInt(2);
//            newCell = getGame().getMap()[row][col];
//        }while ( !(newCell instanceof StreetCell) || hasSameLocation(lastAction,  newCell) );
//
//        return newCell;
//    }

    private boolean hasSameLocation(AgentAction action, Cell cell){
        if( action.nextPosition[0] == cell.getRow() && action.nextPosition[1] == cell.getCol() ){
            return true;
        }else{
            return false;
        }
    }

    protected Path computeOptimumPath(Cell from, Cell to, int maxDist){
        List<String> collisions = getGame().getColisionsByName(getLocalName());
        if ( inCollision() && mustAvoid(collisions) ){
            System.err.println("In collision");
            cellToAvoid = new StreetCell(lastAction.nextPosition[0], lastAction.nextPosition[1]);
        }

        if(cellToAvoid == null){
            return getGame().getGraph().computeOptimumPath(from, to, maxDist);
        }
        return getGame().getGraph().computeOptimumPathWithRestrictions(from, to, cellToAvoid, maxDist);
    }
}
