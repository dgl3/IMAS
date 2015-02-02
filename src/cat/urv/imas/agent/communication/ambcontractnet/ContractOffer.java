package cat.urv.imas.agent.communication.ambcontractnet;

import cat.urv.imas.agent.communication.util.MessageCreator;
import cat.urv.imas.map.Cell;
import cat.urv.imas.onthology.MessageContent;
import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

/**
 * Created by Philipp Oliver on 29/1/15.
 */
public class ContractOffer implements java.io.Serializable {
    private AID contractor;
    private int contractNetID;
    private Cell cell;
    private String kindMessage;

    public ContractOffer(AID contractor, int contractNetID, Cell cell, String kindMessage) {
        this.contractNetID = contractNetID;
        this.contractor = contractor;
        this.cell = cell;
        this.kindMessage = kindMessage;
    }

    public Cell getCell() {
        return cell;
    }

    public AID getContractor() {
        return contractor;
    }

    public void reply(Agent sender, int bidValue){
        ContractBid bid = new ContractBid(contractNetID, bidValue);
        ACLMessage bidMsg = MessageCreator.createPropose(contractor, kindMessage, bid);
        sender.send(bidMsg);
    }

    public int getContractNetID() {
        return contractNetID;
    }
}
