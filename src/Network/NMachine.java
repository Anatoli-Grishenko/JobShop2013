/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Network;

import StrongHierarchy.*;
import Agents.AgentJobShop;
import agents.LARVAFirstAgent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jobshop.Machine;
import jobshop.Operations;
import jobshop.Product;
import messaging.ACLMessageTools;
import tools.TimeHandler;

/**
 *
 * @author Anatoli Grishenko <Anatoli.Grishenko@gmail.com>
 */
public class NMachine extends AgentJobShop {

    protected Machine myMachine;
    protected TimeHandler timer;
    protected ACLMessage aclprod;
    protected String swho;

    @Override
    public void setup() {
        super.setup();
        myMachine = new Machine(getLocalName());
        this.DFSetMyServices(new String[]{"Machine"});
        this.setAvailable();
        timer = null;
    }

    @Override
    public void Execute() {
        inbox = this.LARVAblockingReceive(100);
        if (inbox != null) {
            switch (inbox.getPerformative()) {
                case ACLMessage.CANCEL:
                    doExit();
                    break;
                case ACLMessage.CFP:
                    product = new Product(inbox.getContent());
                    Info("Received CFP for " + product.toString());
                    Operations op = product.getCurrentOperation();
                    outbox = inbox.createReply();
                    if (myMachine.isAvailable()) {
                        outbox.setPerformative(ACLMessage.PROPOSE);
                        if (myOpt == Optimizations.CHEAPEST) {
                            outbox.setContent("" + myMachine.processingTime(op) * myMachine.processingCost(op));
                        } else if (myOpt == Optimizations.FASTEST) {
                            outbox.setContent("" + myMachine.processingTime(op));
                        } else {
                            outbox.setContent("" + 0);
                        }
                        Info("Answer to CFP: " + outbox.getContent());
                        LARVAsend(outbox);
                    } else {
                        outbox.setContent("");
                        outbox.setPerformative(ACLMessage.REFUSE);
                    }
                    break;
                case ACLMessage.REJECT_PROPOSAL:
                    product = new Product(inbox.getContent());
                    Info("Propose rejected " + product.toString() + " due to " + myOpt.name());
                    break;
                case ACLMessage.ACCEPT_PROPOSAL:
                    aclprod = inbox;
                    product = new Product(inbox.getContent());
                    product.setCost(product.getCost()+myMachine.processingCost(product.getCurrentOperation()));
                    Info("Propose ACCEPTED " + product.toString() + " due to " + myOpt.name());
                    Info("Processing product " + product.toString());
                    this.setUnAvailable(product);
                    timer = new TimeHandler();
                    break;
                case ACLMessage.INFORM:
                default:
                    Info("Ignoring " + ACLMessageTools.fancyWriteACLM(inbox, true));
            }
        }
        if (timer != null) {
            if (timer.elapsedTimeSecs(new TimeHandler()) >= (int) (myMachine.processingTime(product.getCurrentOperation()))) {
                Info("Done processing product " + product.toString());
                this.setAvailable();
                timer = null;
                product.nextOperation();
                if (product.getCurrentOperation() == Operations.END) {
                    outbox = new ACLMessage(ACLMessage.INFORM);
                    outbox.setSender(getAID());
                    outbox.addReceiver(new AID(aclprod.getConversationId(), AID.ISLOCALNAME));
                    outbox.setContent(product.toString());
                    outbox.setInReplyTo(product.getID());
                    LARVAsend(outbox);
                } else {
                    swho = this.callForProposals(product, aclprod.getConversationId());
                }
            }
        }
    }

    public void setAvailable() {
        myMachine.setAvailable(true);
        if (!this.DFHasService(getLocalName(), "AVAILABLE")) {
            this.DFAddMyServices(new String[]{"AVAILABLE"});
        }
        for (String service : this.DFGetAllServicesProvidedBy(getLocalName())) {
            if (service.startsWith("PROCESS")) {
                this.DFRemoveMyServices(new String[]{service});
            }
        }
    }

    public void setUnAvailable(Product p) {
        myMachine.setAvailable(false);
        if (this.DFHasService(getLocalName(), "AVAILABLE")) {
            this.DFRemoveMyServices(new String[]{"AVAILABLE"});
        }
        this.DFAddMyServices(new String[]{"PROCESS " + p.toString()});
    }

}
