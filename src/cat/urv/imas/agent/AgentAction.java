/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cat.urv.imas.agent;

import java.util.Map;


public class AgentAction implements java.io.Serializable {
    
    public String agentName;
    
    public int nextPosition[];
    
    public boolean extraActions;
    
    public int actionPosition[];
    
    public AgentAction(String agentName, int nextPosition[]) {
        this.agentName = agentName;
        this.nextPosition = nextPosition;
        this.extraActions = false;
    }
    
    public void setAction(int actionPosition[]) {
        this.extraActions = true;
        this.actionPosition = actionPosition;
    }
    
    public Boolean hasAction() {
        return this.extraActions;
    }
    
    public AgentType getAgentType() {
        if (this.agentName.startsWith("fire")) {
            return AgentType.FIREMAN;
        } else {
            return AgentType.AMBULANCE;
        }
    }
}
