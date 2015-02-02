/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.statistics;

import cat.urv.imas.map.Cell;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameStatistics {
    
    /**
     * List of fire statistics
     */
    private List<FireStatistics> fires;
    
    /**
     * List of hospital occupancy
     */
    private List<List<Integer>> occupancy;
    
    /**
     * 
     */
    private float peopleInRisk;
    
    public GameStatistics() {
        this.fires = new ArrayList<>();
        this.occupancy = new ArrayList<>();
    }
    
    public void newFire(Cell building, int startingTurn) {
        this.fires.add(new FireStatistics(building, startingTurn));
    }
    
    public void setNewTurnHospitalOccupancy(List<Integer> occupancy) {
        this.occupancy.add(occupancy);
    }
    
    public List<FireStatistics> getActiveFires() {
        List<FireStatistics> activeFires = new ArrayList<>();
        
        for (FireStatistics f : this.fires) {
            if (f.isActive()) {
                activeFires.add(f);
            }
        }
        return activeFires;
    }
    
    public List<FireStatistics> getInactiveFires() {
        List<FireStatistics> inactiveFires = new ArrayList<>();
        
        for (FireStatistics f : this.fires) {
            if (!f.isActive()) {
                inactiveFires.add(f);
            }
        }
        return inactiveFires;
    }
    
    public FireStatistics getActiveFireStatistics(Cell c) {
        for (FireStatistics fs : this.fires) {
            if (fs.isActive() && fs.getFireCell().getRow() == c.getRow() 
                    && fs.getFireCell().getCol() == c.getCol()) {
                return fs;
            }
        }
        return null;
    }
    
    public void updatePeopleInRisk(float pir) {
        this.peopleInRisk = pir;
    }
    
    public String getCurrentStatistics() {
        int totalFires = this.fires.size();
        float avarageMaxBurnRatio = 0;
        int numberActiveFires = 0;
        int putDownFires = 0;
        int burntBuildings = 0;
        int casualties = 0;
        for (FireStatistics fs : this.fires) {
            if (fs.isActive()) {
                numberActiveFires += 1;
            } else {
                if (fs.getIsBurned()) {
                    burntBuildings += 1;
                    casualties += fs.getCasualties();
                } else {
                    putDownFires += 1;
                    avarageMaxBurnRatio += fs.getBurnPercentage();
                }
            }
        }
        String hospitalOccupancy = "";
        
        if (this.occupancy.size() > 0) {
            
            int nHospitals = this.occupancy.get(0).size();
            
            int ho[] = new int[nHospitals];
            Arrays.fill(ho,0);
            for (List<Integer> li : this.occupancy) {
                for (int i=0; i<nHospitals; i++) {
                    ho[i] += li.get(i);
                }
            }
            
            for (int i=0; i<nHospitals; i++) {
                hospitalOccupancy = hospitalOccupancy.concat("Avarage Ocuppancy of hospital " + i + ": " + ho[i]/this.occupancy.size() + "\n");
            }
        }
        
        if (putDownFires != 0) {
            avarageMaxBurnRatio = avarageMaxBurnRatio / putDownFires;
        } else {
            avarageMaxBurnRatio = 0;
        }
        
        String currentStatistics = "Total Fires: " + totalFires + "\n"
                + "Fires Put Down: " + putDownFires + "\n"
                + "Burned Buildings: " + burntBuildings + "\n"
                + "Avarage Burned Ration when Fireman arrived: " + avarageMaxBurnRatio + "\n"
                + "Active Fires: " + numberActiveFires + "\n"
                + "Number of Casualties: " + casualties + "\n"
                + "People in risk: " + this.peopleInRisk + "\n"
                + hospitalOccupancy;
        
        return currentStatistics;
    }
}
