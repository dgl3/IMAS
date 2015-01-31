/**
 *  IMAS base code for the practical work.
 *  Copyright (C) 2014 DEIM - URV
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cat.urv.imas.agent;

import cat.urv.imas.agent.communication.util.KeyValue;
import cat.urv.imas.agent.communication.util.MessageCreator;
import cat.urv.imas.onthology.MessageContent;
import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Agent abstraction used in this practical work.
 * It gathers common attributes and functionality from all agents.
 */
public class ImasAgent extends Agent {
    
    /**
     * Type of this agent.
     */
    protected AgentType type;
    
    /**
     * Agents' owner.
     */
    public static final String OWNER = "urv";
    /**
     * Language used for communication.
     */
    public static final String LANGUAGE = "serialized-object";
    /**
     * Onthology used in the communication.
     */
    public static final String ONTOLOGY = "serialized-object";
    
    /**
     * Creates the agent.
     * @param type type of agent to set.
     */
    public ImasAgent(AgentType type) {
        super();
        this.type = type;
    }
    
    /**
     * Informs the type of agent.
     * @return the type of agent.
     */
    public AgentType getType() {
        return this.type;
    }
    
    /**
     * Add a new message to the log.
     *
     * @param str message to show
     */
    public void log(String str) {
        System.out.println(getLocalName() + ": " + str);
    }
    
    /**
     * Add a new message to the error log.
     *
     * @param str message to show
     */
    public void errorLog(String str) {
        System.err.println(getLocalName() + ": " + str);
    }

    /**
     * Helper to extract the content from a ACLMessage.
     * @param msg
     * @return
     */
    protected <O> KeyValue<String, O> getMessageContent(ACLMessage msg){
        KeyValue<String, O> keyValue = null;

        try {
            Map<String,Object> contentObject = (Map<String,Object>) msg.getContentObject();
            String content = contentObject.keySet().iterator().next();

            keyValue = new KeyValue<>(content, (O)contentObject.get(content));
        } catch (UnreadableException ex) {
            Logger.getLogger(FiremenCoordinatorAgent.class.getName()).log(Level.SEVERE, null, ex);
        }

        return keyValue;
    }

    /**
     * Confirms to the parent agent that the game update has been performed successfully.
     */
    protected void sendGameUpdateConfirmation(AID parent) {
        ACLMessage gameUpdateConfirmationMsg = MessageCreator.createConfirm(parent, MessageContent.SEND_GAME, null);
        send(gameUpdateConfirmationMsg);
    }
}
