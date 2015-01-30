/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.statistics;

import cat.urv.imas.map.Cell;

public class FireStatistics {
    
    /**
     * Building of the fire
     */
    private Cell building;
    
    /**
     * Turn in which the fire was started
     */
    private int startingTurn;
    
    /**
     * Boolean to check if the fire was burnt down or not
     */
    private Boolean isBurned;
    
    /**
     * Turn in which the fire was put off or it burned the building
     */
    private int finishingTurn = -1;
    
    /**
     * Casualties of the fire is case it burned down
     */
    private int casualties;
    
    /**
     * Maximum burned ratio of the fire
     */
    private int burnPercentage;
    
    void FireStatistics(Cell building, int startingTurn) {
        this.building = building;
        this.startingTurn = startingTurn;
    }
    
    public Boolean isActive() {
        return (this.finishingTurn == -1);
    }
    
}
