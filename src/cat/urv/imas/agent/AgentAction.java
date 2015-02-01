/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cat.urv.imas.agent;

import cat.urv.imas.map.Cell;
import jade.core.AID;


public class AgentAction implements java.io.Serializable {
    
    public AID agentAID;

    public int nextPosition[]; // 0=Row, 1=Col
    
    public boolean extraActions;
    
    public int actionPosition[];
    public int actionParameter;
    
    public AgentAction(AID agentName, Cell nextPosition) {
        this.agentAID = agentName;
        this.nextPosition = new int[2];
        this.nextPosition[0] = nextPosition.getRow();
        this.nextPosition[1] = nextPosition.getCol();

        this.extraActions = false;
    }

    public void changeNextPosition(Cell nextPosition) {
        this.nextPosition = new int[2];
        this.nextPosition[0] = nextPosition.getRow();
        this.nextPosition[1] = nextPosition.getCol();
    }
    
    public void setAction(Cell actionPosition, int actionParameter) {
        this.extraActions = true;
        this.actionParameter = actionParameter;
        this.actionPosition = new int[2];
        this.actionPosition[0] = actionPosition.getRow();
        this.actionPosition[1] = actionPosition.getCol();
    }
    
    public Boolean hasAction() {
        return this.extraActions;
    }
    
    public void setPosition(Cell nextPosition){
        this.nextPosition[0] = nextPosition.getRow();
        this.nextPosition[1] = nextPosition.getCol();
    }
    
    public AgentType getAgentType() {
        if (this.agentAID.getLocalName().startsWith("fire")) {
            return AgentType.FIREMAN;
        } else {
            return AgentType.AMBULANCE;
        }
    }
}
