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
package cat.urv.imas.onthology;

/**
 * Content messages for inter-agent communication.
 */
public class MessageContent {
    
    /**
     * Message sent from Coordinator agent to Central agent to get the whole
     * city information.
     */
    public static final String GET_MAP = "Get map";
    
    /**
     * Message sent to send the whole game information
     */
    public static final String SEND_GAME = "Send Game";
    
    /**
     * Message sent to send the while game information + petition to take care of a new fire
     */
    public static final String NEW_FIRE_PETITION = "New Fire Petition";
    
    /**
     * Message sent to inform about turns end + new position for all child agents
     */
    public static final String END_TURN = "End Of Turn";
    
    /**
     * Message sent to propose about starting a contract net
     */
    public static final String START_CONTRACTNET = "Start Contract Net";
    
     /**
     * Message sent to reject the proposal of starting a contract net
     */
    public static final String REJECT_CONTRACTNET = "Reject Contract Net";
    
}
