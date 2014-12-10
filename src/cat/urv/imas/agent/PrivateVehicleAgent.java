
package cat.urv.imas.agent;

import cat.urv.imas.map.Cell;
import cat.urv.imas.onthology.GameSettings;
import jade.core.AID;

/**
 *
 * @author dgl3
 */
public class PrivateVehicleAgent extends ImasAgent{
    /**
     * PrivateVehicleAgent position
     */
    private Cell currentPosition;
    
     /**
     * Game settings in use.
     */
    private GameSettings game;
    
    private AID privateVehicleAgent;

    public PrivateVehicleAgent() {
        super(AgentType.PRIVATE_VEHICLE);
    }
    
    @Override
    protected void setup() {
        //TODO
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
    
    
}
