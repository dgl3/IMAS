
package cat.urv.imas.agent;

import cat.urv.imas.map.Cell;
import cat.urv.imas.onthology.GameSettings;
import jade.core.AID;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * Not Really an agent
 */
public class PrivateVehicleAgent {
    /**
     * Private vehicle current position
     */
    private Cell currentPosition;
    
    /**
     * Previous vehicle previous position
     */
    private Cell previousPosition;
    
    /**
     * LocalName
     */
    private String localName;
    
    /**
     * Central Agent
     */
    private Random RNG;
    
    /**
     * Game Settings
     */
    private GameSettings game;
    
    PrivateVehicleAgent(String name, Cell currentPosition, Random RNG, GameSettings game) {
        this.localName = name;
        this.currentPosition = currentPosition;
        this.previousPosition = currentPosition;
        this.RNG = RNG;
        this.game = game;
    }
    
    public void updateCurrentPosition(Cell currentPosition) {
        this.previousPosition = this.currentPosition;
        this.currentPosition = currentPosition;
    }
    
    public Cell getCurrentPosition() {
        return this.currentPosition;
    }
    
    public Cell makeNewMovement(Cell[][] map) {
        List<Cell> possibleNearbyCells = new ArrayList<>();
        if (this.game.isEmptyStreet(map[this.currentPosition.getRow()+1][this.currentPosition.getCol()])) {
            possibleNearbyCells.add(map[this.currentPosition.getRow()+1][this.currentPosition.getCol()]);
        }
        if (this.game.isEmptyStreet(map[this.currentPosition.getRow()-1][this.currentPosition.getCol()])) {
            possibleNearbyCells.add(map[this.currentPosition.getRow()-1][this.currentPosition.getCol()]);
        }
        if (this.game.isEmptyStreet(map[this.currentPosition.getRow()][this.currentPosition.getCol()+1])) {
            possibleNearbyCells.add(map[this.currentPosition.getRow()][this.currentPosition.getCol()+1]);
        }
        if (this.game.isEmptyStreet(map[this.currentPosition.getRow()][this.currentPosition.getCol()-1])) {
            possibleNearbyCells.add(map[this.currentPosition.getRow()][this.currentPosition.getCol()-1]);
        }
        
        if (this.currentPosition.getRow() == this.previousPosition.getRow() &&
                this.currentPosition.getCol() == this.previousPosition.getCol()) {
            // There is no current direction
            if (possibleNearbyCells.size() > 0) {
                int movement = this.RNG.nextInt(possibleNearbyCells.size());
                return possibleNearbyCells.get(movement);
            } else {
                return this.currentPosition;
            }
        } else {
            // There is a current direction
            int[] forwardDirection = new int[2];
            forwardDirection[0] = this.currentPosition.getRow() - this.previousPosition.getRow();
            forwardDirection[1] = this.currentPosition.getCol() - this.previousPosition.getCol();
            Cell forwardCell = map[this.currentPosition.getRow() + forwardDirection[0]]
                    [this.currentPosition.getCol() + forwardDirection[1]];
            Cell sideCell1;
            Cell sideCell2;
            if (forwardDirection[0] == 0) {
                sideCell1 = map[this.currentPosition.getRow() + 1]
                    [this.currentPosition.getCol()];
                sideCell2 = map[this.currentPosition.getRow() - 1]
                    [this.currentPosition.getCol()];
            } else {
                sideCell1 = map[this.currentPosition.getRow()]
                    [this.currentPosition.getCol() + 1];
                sideCell2 = map[this.currentPosition.getRow()]
                    [this.currentPosition.getCol() - 1];
            }
            Cell backwardCell = map[this.currentPosition.getRow() - forwardDirection[0]]
                    [this.currentPosition.getCol() - forwardDirection[1]];
            
            if (this.game.isEmptyStreet(forwardCell)) {
                if (this.game.isEmptyStreet(sideCell1) && this.game.isEmptyStreet(sideCell2)) {
                    int rand = this.RNG.nextInt(10);
                    if (rand < 8) {
                        return forwardCell;
                    } else if (rand == 8) {
                        return sideCell1;
                    } else {
                        return sideCell2;
                    }
                } else if (this.game.isEmptyStreet(sideCell1)) {
                    int rand = this.RNG.nextInt(9);
                    if (rand < 8) {
                        return forwardCell;
                    } else {
                        return sideCell1;
                    }
                } else if (this.game.isEmptyStreet(sideCell2)) {
                    int rand = this.RNG.nextInt(9);
                    if (rand < 8) {
                        return forwardCell;
                    } else {
                        return sideCell2;
                    }
                } else {
                    return forwardCell;
                }
            } else {
                if (this.game.isEmptyStreet(sideCell1) && this.game.isEmptyStreet(sideCell2)) {
                    int rand = this.RNG.nextInt(2);
                    if (rand == 0) {
                        return sideCell1;
                    } else {
                        return sideCell2;
                    }
                } else if (this.game.isEmptyStreet(sideCell1)) {
                    return sideCell1;
                } else if (this.game.isEmptyStreet(sideCell2)) {
                    return sideCell2;
                } else if (this.game.isEmptyStreet(backwardCell)) {
                    return backwardCell;
                } else {
                    return this.currentPosition;
                }
            }
        }
    }
}
