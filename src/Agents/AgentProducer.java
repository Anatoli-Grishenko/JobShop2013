/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Agents;

import appboot.XUITTY;
import static appboot.XUITTY.HTMLColor.Gray;
import static appboot.XUITTY.HTMLColor.Green;
import static appboot.XUITTY.HTMLColor.Red;
import static appboot.XUITTY.HTMLColor.White;
import com.eclipsesource.json.WriterConfig;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import jobshop.Machine;
import jobshop.Operations;
import jobshop.Product;

/**
 *
 * @author Anatoli Grishenko <Anatoli.Grishenko@gmail.com>
 */
public class AgentProducer extends AgentJobShop {

    enum Status {
        WAITANSWERS, PRODUCE, CANCEL, EXIT
    };
    Status myStatus;
    double total;

    @Override
    public void setup() {
        super.setup();
        Info("Setup ");
        who.put("MYCONTROLLER", this.DFGetAllProvidersOf("Controller").get(0));
        this.openXUITTY();
        productionSet2();
        total = 0;
        myStatus = Status.PRODUCE;
    }

    @Override
    public void Execute() {
        Info("Ready. Status:" + myStatus.name() + ", " + inProductionProducts.size() + " in production)");
        showSummary();
        this.saveSequenceDiagram("jobshop.seqd");
        switch (myStatus) {
            case PRODUCE:
                myStatus = myProduce();
                break;
            case WAITANSWERS:
                myStatus = myWait();
                break;
            case CANCEL:
                myStatus = myCancel();
                break;
            case EXIT:
                doExit();
                break;
        }
    }

    @Override
    public void takeDown() {
        super.takeDown();
    }

    public Status myProduce() {
        if (queuedProducts.size() > 0) {
            product = queuedProducts.get(0);
            queuedProducts.remove(0);
            Info("Product arrival:\n" + product.toJson().toString(WriterConfig.PRETTY_PRINT));
            outbox = new ACLMessage(ACLMessage.REQUEST);
            outbox.setSender(getAID());
            outbox.addReceiver(new AID(who.get("MYCONTROLLER"), AID.ISLOCALNAME));
            outbox.setContent(product.toString());
            outbox.setConversationId(getLocalName());
            outbox.setReplyWith(product.getID());
            this.LARVAsend(outbox);
        }
        return Status.WAITANSWERS;
    }

    public Status myWait() {
        inbox = this.LARVAblockingReceive(1000);
        if (inProductionProducts.size() > doneProducts.size()) {
            if (inbox != null) {
                if (inbox.getPerformative() == ACLMessage.INFORM) {
                    product = new Product(inbox.getContent());
                    doneProducts.add(product);
                    if (myOpt == Optimizations.FASTEST) {
                        total = Math.max(total, product.getCost());
                    } else {
                        total = total + product.getCost();
                    }
                }
            }
            return Status.PRODUCE;
        } else {
            return Status.CANCEL;
        }

    }

    public Status myCancel() {
        outbox = new ACLMessage(ACLMessage.CANCEL);
        outbox.setSender(getAID());
        outbox.addReceiver(new AID(who.get("MYCONTROLLER"), AID.ISLOCALNAME));
        outbox.setContent("");
        this.LARVAsend(outbox);
        return Status.EXIT;
    }

    public void productionSet1() {
        Product p;
        inProductionProducts.clear();
        queuedProducts.clear();
        p = new Product();
        product.setID("000");
        product.addOperation(Operations.BEGIN)
                .addOperation(Operations.SLOWCUT)
                .addOperation(Operations.END);
        inProductionProducts.add(p);
        queuedProducts.add(p);
    }

    public void productionSet1b() {
        Product p;
        inProductionProducts.clear();
        queuedProducts.clear();
        for (int i = 0; i < 5; i++) {
            p = new Product();
            product.setID(String.format("%03d", i));
            product.addOperation(Operations.BEGIN)
                    .addOperation(Operations.SLOWCUT)
                    .addOperation(Operations.END);
            inProductionProducts.add(p);
            queuedProducts.add(p);
        }
    }

    public void productionSet2() {
        Product p;
        inProductionProducts.clear();
        queuedProducts.clear();
        for (int i = 0; i < 5; i++) {
            p = new Product();
            product.setID(String.format("%03d", i));
            product.addOperation(Operations.BEGIN)
                    .addOperation(Operations.SLOWCUT)
                    .addOperation(Operations.DRILL)
                    .addOperation(Operations.POLISH)
                    .addOperation(Operations.END);
            inProductionProducts.add(p);
            queuedProducts.add(p);
        }
    }

    public void productionSet3() {
        Product p;
        inProductionProducts.clear();
        queuedProducts.clear();
        for (int i = 0; i < 5; i++) {
            p = this.genProduct((int) (Math.random() * 5), i);
            inProductionProducts.add(p);
            queuedProducts.add(p);
        }
    }

    public Product genProduct(int type, int n) {
        Product p;
        p = new Product();
//        product.setID(""+this.getNCycles()); 
        product.setID(String.format("%03d", n));
//        p = new Product(Keygen.getWordo(8));
        product.addOperation(Operations.BEGIN);
        switch (type) {
            case 0:
            default:
                product.addOperation(Operations.SLOWCUT);
                break;
            case 1:
                product.addOperation(Operations.SLOWCUT);
                product.addOperation(Operations.DRILL);
                product.addOperation(Operations.POLISH);
                break;
            case 2:
                product.addOperation(Operations.DRILL);
                product.addOperation(Operations.FASTCUT);
                product.addOperation(Operations.POLISH);
                product.addOperation(Operations.DRILL);
                break;
            case 3:
                product.addOperation(Operations.POLISH);
                break;
        }
        product.addOperation(Operations.END);
        return p;
    }

    public void showSummary() {
        int x, y;
        getLayout();
        ArrayList<String> machines = new ArrayList(layout.Machines.keySet());

        xuitty.textColor(White);
        xuitty.doFrameTitle("JOBSHOP", 1, 1, 130, 35);
        x = 5;
        y = 3;
        xuitty.textColor(White);
        xuitty.doFrameTitle("PRODUCTION", x - 1, y - 1, 120, 12);
        xuitty.textColor(White);
        for (int i = 0; i < machines.size(); i++) {
            xuitty.setCursorXY(x, y++);
            xuitty.print(machines.get(i) + ": ");
            xuitty.setCursorXY(x + 10 + ((int) this.getNCycles() * 4) % (xuitty.getXUIWidth() - 30), 3 + i);
            if (layout.Machines.get(machines.get(i)).getProcessing() == null) { //.isAvailable()) {
                xuitty.print("|   " + "    ");
            } else {
                xuitty.print("|");
                printProduct(layout.Machines.get(machines.get(i)).getProcessing());
                xuitty.print("    ");
            }
        }
        y = 15;
        x = 5;
        xuitty.doFrameTitle("IN PRODUCTION", x - 1, y - 1, 26, 20);
//        xuitty.setCursorXY(x, y++);
//        xuitty.textColor(Green);
//        xuitty.print("IN PRODUCTION");
        xuitty.textColor(White);
        for (Product p : inProductionProducts) {
            xuitty.setCursorXY(x, y++);
            printProduct(p);
            xuitty.print(">");
            for (Operations op : product.getSequence()) {
                xuitty.print(op.name().substring(0, 1));
            }
//            xuitty.print(p.toString());
        }
        y = 15;
        x = 35;
        xuitty.textColor(White);
        xuitty.doFrameTitle("FINISHED (total= " + total + ")", x - 1, y - 1, 26, 20);
//        xuitty.setCursorXY(x, y++);
//        xuitty.textColor(Red);
//        xuitty.print("FINISHED");
        xuitty.textColor(White);
        xuitty.textColor(White);
        for (Product p : doneProducts) {
            xuitty.setCursorXY(x, y++);
            printProduct(p);
            xuitty.print(" Cost " + product.getCost());
        }
        y = 15;
        x = 65;
        xuitty.textColor(White);
        xuitty.doFrameTitle("DF INFO", x - 1, y - 1, 30, 20);
//        xuitty.setCursorXY(x, y++);
//        xuitty.textColor(Blue);
//        xuitty.print("DF");

        Machine m;
        for (String smachine : layout.Machines.keySet()) {
            m = layout.Machines.get(smachine);
            xuitty.setCursorXY(x, y++);
            xuitty.textColor(White).print(m.getID() + "  ");
            if (m.isAvailable()) {
                xuitty.textColor(Green).print("    ").textColor(White);
            } else {
                xuitty.textColor(Red).print("N/A").textColor(White);
            }
            xuitty.setCursorXY(x + 3, y++).textColor(Gray);
            for (Operations op : layout.Machine2Capabilities.get(smachine)) {
                xuitty.print("[" + op.name() + "] ");
            }
            product = m.getProcessing();
            xuitty.setCursorXY(x + 3, y++);
            if (product != null) {
                printProduct(product);
                xuitty.print(": " + product.getCost());
            } else {
                xuitty.print("                        ");
            }
        }

        xuitty.render();
    }

    public void printProduct(Product p) {
        int color;
        try {
            color = Integer.parseInt(p.getID());
        } catch (Exception ex) {
            color = 0;
        }
        color = color % XUITTY.HTMLColor.values().length + 1;
//        xuitty.print(p.getID());
        xuitty.textColor(XUITTY.HTMLColor.values()[color]).print(p.getID()).textColor(White);

    }
}
