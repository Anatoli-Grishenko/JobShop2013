/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Agents;

import agents.LARVAFirstAgent;
import jade.lang.acl.ACLMessage;
import jobshop.Machine;
import jobshop.Operations;
import jobshop.Product;
import messaging.ACLMessageTools;

/**
 *
 * @author Anatoli Grishenko <Anatoli.Grishenko@gmail.com>
 */
public class AgentMachine extends AgentJobShop {

    protected Machine myMachine;

    @Override
    public void setup() {
        super.setup();
        myMachine = new Machine(getLocalName());
        this.DFSetMyServices(new String[]{"Machine"});
        this.setUnAvailable();
    }

    @Override
    public void Execute() {
        inbox = this.LARVAblockingReceive();
        switch (inbox.getPerformative()) {
            case ACLMessage.REQUEST:
                prod = new Product(inbox.getContent());
                Info("Processing product " + prod.toString());
                this.setUnAvailable();
                this.LARVAwait(1000 * (int) (myMachine.processingTime(prod.getCurrentOperation())));
                this.setAvailable();
                prod.nextOperation();
                Info("Done processing product " + prod.toString());
                outbox = inbox.createReply();
                outbox.setPerformative(ACLMessage.INFORM);
                outbox.setContent(prod.toString());
                this.LARVAsend(outbox);
                break;
            default:
                Info("Ignoring " + ACLMessageTools.fancyWriteACLM(inbox, true));
        }
    }

    public void setAvailable() {
        myMachine.setAvailable(true);
        if (!this.DFHasService(getLocalName(), "AVAILABLE")) {
            this.DFAddMyServices(new String[]{"AVAILABLE"});
        }
    }

    public void setUnAvailable() {
        myMachine.setAvailable(false);
        if (this.DFHasService(getLocalName(), "AVAILABLE")) {
            this.DFRemoveMyServices(new String[]{"AVAILABLE"});
        }
    }
}
