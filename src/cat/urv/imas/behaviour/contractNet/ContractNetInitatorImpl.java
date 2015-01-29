/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cat.urv.imas.behaviour.contractNet;

import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;

/**
 *
 * @author dgl3
 * Implementation of the ContractNet behaviour
 */
public class ContractNetInitatorImpl extends ContractNetInitiator{

    public ContractNetInitatorImpl(Agent a, ACLMessage cfp) {
        super(a, cfp);
    }
    
}
