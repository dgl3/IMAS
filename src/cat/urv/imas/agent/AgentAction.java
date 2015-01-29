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
    //public Map<String,int[]> extraActionsPosition;
    
    public AgentAction(String agentName, int nextPosition[]) {
        this.agentName = agentName;
        this.nextPosition = nextPosition;
        this.extraActions = false;
    }
    
    /*public void setExtraActions(String actionName, int actionPosition[]) {
        this.extraActions = true;
        this.extraActionsPosition.put(actionName, actionPosition);
    }*/
}
