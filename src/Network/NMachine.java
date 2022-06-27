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

    protected enum Status {
        AVAILABLE, BUSY, FORWARDPRODUCT, CANCEL, EXIT
    }
    protected Status myStatus;
    protected Machine myMachine;
    protected TimeHandler timer;
    protected String nextMachine, conversationID;

    @Override
    public void setup() {
        super.setup();
        myMachine = new Machine(getLocalName());
        this.DFSetMyServices(new String[]{"Machine"});
        this.setAvailable();
        timer = null;
        myStatus = Status.AVAILABLE;
    }

    @Override
    public void Execute() {
//        Info("Status: " + myStatus.name());
        switch (myStatus) {
            case AVAILABLE:
                myStatus = myAvailable();
                break;
            case BUSY:
                myStatus = myBusy();
                break;
            case FORWARDPRODUCT:
                myStatus = myForwardProduct();
                break;
            case CANCEL:
            case EXIT:
                doExit();
                break;
        }

    }

    public Status myAvailable() {
        inbox = this.LARVAblockingReceive(SHORTWAIT);
        if (inbox != null) {
            switch (inbox.getPerformative()) {
                case ACLMessage.CANCEL:
                    return Status.CANCEL;
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
                    fromWho.put("SENDER", inbox);
                    product = new Product(inbox.getContent());
                    conversationID = inbox.getConversationId();
                    Info("Propose ACCEPTED " + product.toString() + " due to " + myOpt.name());
                    Info("Processing product " + product.toString());
                    product.setCost(product.getCost() + myMachine.processingCost(product.getCurrentOperation()) * myMachine.processingTime(product.getCurrentOperation()));
                    product.setStart(TimeHandler.Now());
                    this.setUnAvailable(product);
                    timer = new TimeHandler();
                    break;
                default:
                    Info("Ignoring " + ACLMessageTools.fancyWriteACLM(inbox, true));
            }
        }
        if (timer != null) {
            return Status.BUSY;
        } else {
            return Status.AVAILABLE;
        }
    }

    public Status myBusy() {
        if (timer != null) {
            if (timer.elapsedTimeSecs(new TimeHandler()) >= (int) (myMachine.processingTime(product.getCurrentOperation()))) {
                Info("Done processing product " + product.toString());
                this.setAvailable();
                timer = null;
                product.nextOperation();
                if (product.getCurrentOperation() == Operations.END) {
                    outbox = new ACLMessage(ACLMessage.INFORM);
                    outbox.setSender(getAID());
                    outbox.addReceiver(new AID(fromWho.get("SENDER").getConversationId(), AID.ISLOCALNAME));
                    outbox.setContent(product.toString());
                    outbox.setInReplyTo(product.getID());
                    LARVAsend(outbox);
                } else {
                    return Status.FORWARDPRODUCT;
                }
            }
        }
        return Status.AVAILABLE;
    }

    public Status myForwardProduct() {
        nextMachine = this.callForProposals(product, conversationID);
        if (nextMachine != null) {
            Info("Accept propose from " + nextMachine + " due to " + myOpt.name());
            outbox = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            outbox.setSender(this.getAID());
            outbox.setContent(product.toString());
            outbox.setConversationId(conversationID);
            outbox.addReceiver(new AID(nextMachine, AID.ISLOCALNAME));
            LARVAsend(outbox);
        } else {
            Info("Waiting for a machine to be AVAILABLE for op " + product.getCurrentOperation().name());
        }
        return Status.AVAILABLE;
    }

    public void setAvailable() {
        myMachine.setAvailable(true);
        myStatus = Status.AVAILABLE;
        if (!this.DFHasService(getLocalName(), "AVAILABLE")) {
            this.DFAddMyServices(new String[]{"AVAILABLE"});
        }
        for (String service : this.DFGetAllServicesProvidedBy(getLocalName())) {
            if (service.startsWith(Product.HEAD)) {
                this.DFRemoveMyServices(new String[]{service});
            }
        }
    }

    public void setUnAvailable(Product p) {
        myMachine.setAvailable(false);
        myStatus = Status.BUSY;
        if (this.DFHasService(getLocalName(), "AVAILABLE")) {
            this.DFRemoveMyServices(new String[]{"AVAILABLE"});
        }
        this.DFAddMyServices(new String[]{p.toProcess()});
    }
}

//    protected Machine myMachine;
//    protected TimeHandler timer;
//    protected ACLMessage aclprod;
//    protected String swho;
//
//    @Override
//    public void setup() {
//        super.setup();
//        myMachine = new Machine(getLocalName());
//        this.DFSetMyServices(new String[]{"Machine"});
//        this.setAvailable();
//        timer = null;
//    }
//
//        protected enum Status {
//        AVAILABLE, BUSY, CANCEL, EXIT
//    }
//    @Override
//    public void Execute() {
//        inbox = this.LARVAblockingReceive(100);
//        if (inbox != null) {
//            switch (inbox.getPerformative()) {
//                case ACLMessage.CANCEL:
//                    doExit();
//                    break;
//                case ACLMessage.CFP:
//                    product = new Product(inbox.getContent());
//                    Info("Received CFP for " + product.toString());
//                    Operations op = product.getCurrentOperation();
//                    outbox = inbox.createReply();
//                    if (myMachine.isAvailable()) {
//                        outbox.setPerformative(ACLMessage.PROPOSE);
//                        if (myOpt == Optimizations.CHEAPEST) {
//                            outbox.setContent("" + myMachine.processingTime(op) * myMachine.processingCost(op));
//                        } else if (myOpt == Optimizations.FASTEST) {
//                            outbox.setContent("" + myMachine.processingTime(op));
//                        } else {
//                            outbox.setContent("" + 0);
//                        }
//                        Info("Answer to CFP: " + outbox.getContent());
//                        LARVAsend(outbox);
//                    } else {
//                        outbox.setContent("");
//                        outbox.setPerformative(ACLMessage.REFUSE);
//                    }
//                    break;
//                case ACLMessage.REJECT_PROPOSAL:
//                    product = new Product(inbox.getContent());
//                    Info("Propose rejected " + product.toString() + " due to " + myOpt.name());
//                    break;
//                case ACLMessage.ACCEPT_PROPOSAL:
//                    aclprod = inbox;
//                    product = new Product(inbox.getContent());
//                    product.setCost(product.getCost()+myMachine.processingCost(product.getCurrentOperation()));
//                    Info("Propose ACCEPTED " + product.toString() + " due to " + myOpt.name());
//                    Info("Processing product " + product.toString());
//                    this.setUnAvailable(product);
//                    timer = new TimeHandler();
//                    break;
//                case ACLMessage.INFORM:
//                default:
//                    Info("Ignoring " + ACLMessageTools.fancyWriteACLM(inbox, true));
//            }
//        }
//        if (timer != null) {
//            if (timer.elapsedTimeSecs(new TimeHandler()) >= (int) (myMachine.processingTime(product.getCurrentOperation()))) {
//                Info("Done processing product " + product.toString());
//                this.setAvailable();
//                timer = null;
//                product.nextOperation();
//                if (product.getCurrentOperation() == Operations.END) {
//                    outbox = new ACLMessage(ACLMessage.INFORM);
//                    outbox.setSender(getAID());
//                    outbox.addReceiver(new AID(aclprod.getConversationId(), AID.ISLOCALNAME));
//                    outbox.setContent(product.toString());
//                    outbox.setInReplyTo(product.getID());
//                    LARVAsend(outbox);
//                } else {
//                    swho = this.callForProposals(product, aclprod.getConversationId());
//                }
//            }
//        }
//    }
//
//    public void setAvailable() {
//        myMachine.setAvailable(true);
//        if (!this.DFHasService(getLocalName(), "AVAILABLE")) {
//            this.DFAddMyServices(new String[]{"AVAILABLE"});
//        }
//        for (String service : this.DFGetAllServicesProvidedBy(getLocalName())) {
//            if (service.startsWith("PROCESS")) {
//                this.DFRemoveMyServices(new String[]{service});
//            }
//        }
//    }
//
//    public void setUnAvailable(Product p) {
//        myMachine.setAvailable(false);
//        if (this.DFHasService(getLocalName(), "AVAILABLE")) {
//            this.DFRemoveMyServices(new String[]{"AVAILABLE"});
//        }
//        this.DFAddMyServices(new String[]{"PROCESS " + p.toString()});
//    }
//
//}
