/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cat.urv.imas.agent.communication.contractnet;

/**
 *
 * @author Ferran
 */
public class ContractNetInfo {
    
    /**
     * Agent is available (not working on any task).
     */
    private Boolean available;
    
    /**
     * Agent bid sent (if positive is the distance to building in fire, if negative means not bidding).
     */
    private int bid;

    public ContractNetInfo(Boolean available, int bid) {
        this.available = available;
        this.bid = bid;
    }

    public Boolean getAvailable() {
        return available;
    }

    public int getBid() {
        return bid;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public void setBid(int bid) {
        this.bid = bid;
    }
    
    
    
}
