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
package cat.urv.imas.behaviour.FiremenCoordinator;

import cat.urv.imas.behaviour.hospitalCoordinator.*;
import cat.urv.imas.behaviour.coordinator.*;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import cat.urv.imas.agent.CoordinatorAgent;
import cat.urv.imas.agent.FiremenCoordinatorAgent;
import cat.urv.imas.agent.HospitalCoordinatorAgent;
import cat.urv.imas.onthology.GameSettings;

/**
 * Behaviour for the HospitalCoordinatorAgent agent to deal with INFORM messages.
 * The HospitalCoordinatorAgent Agent sends a REQUEST for the
 * information of the game settings. The Central Agent sends an AGREE and 
 * then it informs of this information which is stored by the HospitalCoordinatorAgent Agent. 
 * 
 * NOTE: The game is processed by another behaviour that we add after the 
 * INFORM has been processed.
 */
public class InformBehaviour extends AchieveREInitiator {

    public InformBehaviour(FiremenCoordinatorAgent agent, ACLMessage requestMsg) {
        super(agent, requestMsg);
        agent.log("Started inform behaviour");
    }

    /**
     * Handle AGREE messages
     *
     * @param msg Message to handle
     */
    @Override
    protected void handleAgree(ACLMessage msg) {
        FiremenCoordinatorAgent agent = (FiremenCoordinatorAgent) this.getAgent();
        agent.log("AGREE informed from " + ((AID) msg.getSender()).getLocalName());
    }

    /**
     * Handle INFORM messages
     *
     * @param msg Message
     */
    @Override
    protected void handleInform(ACLMessage msg) {
        FiremenCoordinatorAgent agent = (FiremenCoordinatorAgent) this.getAgent();
        agent.log("INFORM received from " + ((AID) msg.getSender()).getLocalName());
    }

    /**
     * Handle NOT-UNDERSTOOD messages
     *
     * @param msg Message
     */
    @Override
    protected void handleNotUnderstood(ACLMessage msg) {
        FiremenCoordinatorAgent agent = (FiremenCoordinatorAgent) this.getAgent();
        agent.log("This message NOT UNDERSTOOD.");
    }

    /**
     * Handle FAILURE messages
     *
     * @param msg Message
     */
    @Override
    protected void handleFailure(ACLMessage msg) {
        FiremenCoordinatorAgent agent = (FiremenCoordinatorAgent) this.getAgent();
        agent.log("The action has failed.");

    } //End of handleFailure

    /**
     * Handle REFUSE messages
     *
     * @param msg Message
     */
    @Override
    protected void handleRefuse(ACLMessage msg) {
        FiremenCoordinatorAgent agent = (FiremenCoordinatorAgent) this.getAgent();
        agent.log("Action refused.");
    }

}
