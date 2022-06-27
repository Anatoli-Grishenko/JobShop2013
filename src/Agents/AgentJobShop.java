/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Agents;

import agents.LARVAFirstAgent;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import data.Transform;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.ArrayList;
import java.util.HashMap;
import jobshop.Layout;
import jobshop.Machine;
import jobshop.Operations;
import jobshop.Product;

/**
 *
 * @author Anatoli Grishenko <Anatoli.Grishenko@gmail.com>
 */
public class AgentJobShop extends LARVAFirstAgent {
    protected final int SHORTWAIT=100, LONGWAIT=1000;
    protected enum Optimizations {
        FASTEST, CHEAPEST, ANY
    };
    protected Layout layout;
    protected Product product;
    protected Optimizations myOpt;
    protected ArrayList<String> Machines;
    protected ArrayList<Product> inProductionProducts, doneProducts, queuedProducts;
    protected HashMap<String, Product> indexedQueuedProducts;
    protected HashMap <String, String> who;
    protected HashMap <String, ACLMessage> fromWho;
        
    @Override
    public void setup() {
        super.setup();
        logger.onTabular();
        myOpt = Optimizations.FASTEST;
        inProductionProducts = new ArrayList();
        doneProducts = new ArrayList();
        queuedProducts = new ArrayList();
        indexedQueuedProducts = new HashMap();
        who = new HashMap();
        fromWho = new HashMap();
        
//        Info("Setup "+Class.class.getName());
    }

    @Override
    public void takeDown() {
        Info("Taking down");
        super.takeDown();
    }

    public void getLayout() {
        Operations op;
        Machine m;
        layout = new Layout();
        Machines = this.DFGetAllProvidersOf("Machine");                
        for (String smachine : Machines) {
            m = new Machine(smachine);
            layout.Machines.put(smachine, m);
            for (String soperation : Transform.getAllNames(Operations.class)) {
                op = Operations.valueOf(soperation);
                if (this.DFHasService(smachine, soperation)) {
                    if (layout.Capabilities2Machine.get(op) == null) {
                        layout.Capabilities2Machine.put(op, new ArrayList());
                    }
                    if (layout.Machine2Capabilities.get(smachine) == null) {
                        layout.Machine2Capabilities.put(smachine, new ArrayList());
                    }
                    layout.Capabilities2Machine.get(op).add(smachine);
                    layout.Machine2Capabilities.get(smachine).add(op);
                }
            }
            if (this.DFHasService(smachine, "AVAILABLE")) {
                m.setAvailable(true);
            } else {
                m.setAvailable(false);
                for (String service : this.DFGetAllServicesProvidedBy(smachine)) {
                    if (service.startsWith(Product.HEAD)) {
                        m.setProcessing(new Product(service.split(Product.SEP)[1]));
                    }
                }

            }
        }
//        System.out.println("\n\n"+layout.toString()+"\n\n");
    }

    @Override
    public void Execute() {
        inbox = this.LARVAblockingReceive();

    }

    public String outOf(ArrayList<String> al) {
        return al.get((int) (Math.random() * al.size()));
    }

    public String callForProposals(Product prod, String conversationID) {
        int ncandidates;
        ArrayList<String> candidates, proposes;
        String tag, sbest, swho;
        double dbest, propose;
        getLayout();

//        do {
            Info("Negotiation to " + prod.toString());
            
            if (this.DFGetAllProvidersOf(prod.getCurrentOperation().name()).isEmpty()) {
                this.Error("Operation " + prod.getCurrentOperation().name() + " not supported");
                sbest = null;
            } else {
                if (!this.getAllAvailableMachines(prod.getCurrentOperation()).isEmpty()) {
                    candidates = this.getAllAvailableMachines(prod.getCurrentOperation());
                    ncandidates = candidates.size();
                    outbox = new ACLMessage(ACLMessage.CFP);
                    outbox.setSender(this.getAID());
                    outbox.setContent(prod.toString());
                    outbox.setConversationId(conversationID);
                    tag = "CFP " + prod.getID() + " " + prod.getCurrentOperation().name();
                    outbox.setReplyWith(tag);
                    for (String smachine : candidates) {
                        outbox.addReceiver(new AID(smachine, AID.ISLOCALNAME));
                    }
                    Info("Negotiating  " + prod.getID() + " to machines " + candidates);
                    this.LARVAsend(outbox);
                    dbest = Integer.MAX_VALUE;
                    sbest = "";
                    proposes = new ArrayList();
                    while (!candidates.isEmpty()) {
                        inbox = LARVAblockingReceive(MessageTemplate.MatchInReplyTo(tag));
                        swho = inbox.getSender().getLocalName();
                        candidates.remove(swho);
                        switch (inbox.getPerformative()) {
                            case ACLMessage.PROPOSE:
                            case ACLMessage.AGREE:
                                proposes.add(swho);
                                try {
                                    propose = Double.parseDouble(inbox.getContent());
                                } catch (Exception ex) {
                                    propose = Integer.MAX_VALUE;
                                }
                                if (myOpt != Optimizations.ANY) {
                                    if (propose < dbest) {
                                        dbest = propose;
                                        sbest = swho;
                                    }
                                } else {
                                    if (sbest == null) {
                                        sbest = swho;
                                    }
                                }
                            default:
                            case ACLMessage.REFUSE:
                                break;
                        }
                    }
                    Info("Potential answers: " + proposes);
                    if (proposes.contains(sbest)) {
                        proposes.remove(sbest);
                    }
                    if (!proposes.isEmpty()) {
                        Info("Reject proposes to " + proposes+ " due to "+myOpt.name());
                        outbox = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                        outbox.setSender(this.getAID());
                        outbox.setContent(prod.toString());
                        for (String sprop : proposes) {
                            outbox.addReceiver(new AID(sprop, AID.ISLOCALNAME));
                        }
                        LARVAsend(outbox);
                    }
//                    if (sbest != null) {
//                        Info("Accept propose from " + sbest+ " due to "+myOpt.name());
//                        outbox = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
//                        outbox.setSender(this.getAID());
//                        outbox.setContent(product.toString());
//                        outbox.setConversationId(conversationID);
//                        outbox.addReceiver(new AID(sbest, AID.ISLOCALNAME));
//                        LARVAsend(outbox);
//                    }
                } else {
                    Info("Unable to continue with product " + prod.getID() + ": all machines are busy. Delaying ... ");
                    sbest = null;
                    this.LARVAwait(2000);
                }
            }

//        } while (sbest == null);
        return sbest;
    }

    public ArrayList<String> getAllAvailableMachines(Operations op) {
        ArrayList<String> res = new ArrayList();
        for (String s : this.DFGetAllProvidersOf(op.name())) {
            if (layout.Machines.get(s).isAvailable()) {
                res.add(s);
            }
        }
        return res;
    }

    public String getCheapestAvailableMachine(Operations op) {
        String sbest="";
        double dbest=Integer.MAX_VALUE;
        for (String s : this.DFGetAllProvidersOf(op.name())) {
            if (layout.Machines.get(s).isAvailable()) {
                if (layout.Machines.get(s).processingCost(op)<dbest) {
                    dbest = layout.Machines.get(s).processingCost(op);
                    sbest=s;
                }
            }
        }
        return sbest;
    }

    public String getFastestAvailableMachine(Operations op) {
        String sbest="";
        double dbest=Integer.MAX_VALUE;
        for (String s : this.DFGetAllProvidersOf(op.name())) {
            if (layout.Machines.get(s).isAvailable()) {
                if (layout.Machines.get(s).processingTime(op)<dbest) {
                    dbest = layout.Machines.get(s).processingTime(op);
                    sbest=s;
                }
            }
        }
        return sbest;
    }

    
}
