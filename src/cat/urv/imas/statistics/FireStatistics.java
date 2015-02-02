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
    private Boolean isBurned = false;
    
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
    
    public FireStatistics(Cell building, int startingTurn) {
        this.building = building;
        this.startingTurn = startingTurn;
    }
    
    public Boolean isActive() {
        return (this.finishingTurn == -1);
    }
    
    public void fireExtinguished(int turn) {
        this.finishingTurn = turn;
    }
    
    public Cell getFireCell() {
        return this.building;
    }
    
    public void updateBurnedRatio(int burnedRatio, int turn, int peopleInBuilding) {
        if (this.burnPercentage < burnedRatio) {
            this.burnPercentage = burnedRatio;
        }
        if (burnedRatio >= 100) {
            this.isBurned = true;
            this.finishingTurn = turn;
            this.casualties = peopleInBuilding;
        } else if (burnedRatio <= 0) {
            this.finishingTurn = turn;
        }
    }
    
    public int getBurnPercentage() {
        return this.burnPercentage;
    }
    
    public Boolean getIsBurned() {
        return this.isBurned;
    }
    
    public int getCasualties() {
        return this.casualties;
    }
}
