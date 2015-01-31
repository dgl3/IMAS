/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cat.urv.imas.agent;

import jade.core.AID;


public class AgentAction implements java.io.Serializable {
    
    public AID agentAID;
    
    public int nextPosition[];
    
    public boolean extraActions;
    
    public int actionPosition[];
    public int actionParameter;
    
    public AgentAction(AID agentName, int nextPosition[]) {
        this.agentAID = agentName;
        this.nextPosition = nextPosition;
        this.extraActions = false;
    }
    
    public void setAction(int actionPosition[], int actionParameter) {
        this.extraActions = true;
        this.actionPosition = actionPosition;
        this.actionParameter = actionParameter;
    }
    
    public Boolean hasAction() {
        return this.extraActions;
    }
    
    public AgentType getAgentType() {
        if (this.agentAID.getLocalName().startsWith("fire")) {
            return AgentType.FIREMAN;
        } else {
            return AgentType.AMBULANCE;
        }
    }
}
