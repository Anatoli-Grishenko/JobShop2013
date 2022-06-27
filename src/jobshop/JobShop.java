package jobshop;

import Network.NMCutter1;
import appboot.LARVABoot;

public class JobShop {

    static LARVABoot boot;

    public static void main(String[] args) {
        boot = new LARVABoot();
        boot.Boot("localhost", 1099);
        StrongHierarchy();
//        Network();
        boot.WaitToShutDown();

    }
    
    public static void StrongHierarchy() {
        boot.launchAgent("hcutter1", StrongHierarchy.HMCutter1.class);
        boot.launchAgent("hcutter+", StrongHierarchy.HMCutterPRO.class);
        boot.launchAgent("hdriller", StrongHierarchy.HMDrillB.class);
        boot.launchAgent("hdriller+", StrongHierarchy.HMDrillA.class);
        boot.launchAgent("hpolish1", StrongHierarchy.HMPolisher.class);
//        boot.launchAgent("polish2", StrongHierarchy.HMPolisher.class);
        boot.launchAgent("hcontroller", StrongHierarchy.HController.class);
        boot.launchAgent("producer", Agents.AgentProducer.class);        
    }
    public static void Network() {
        boot.launchAgent("ncutter1", Network.NMCutter1.class);
        boot.launchAgent("ncutter+", Network.NMCutterPRO.class);
        boot.launchAgent("ndriller", Network.NMDrillB.class);
        boot.launchAgent("ndriller+", Network.NMDrillA.class);
        boot.launchAgent("npolish1", Network.NMPolisher.class);
        boot.launchAgent("ncontroller", Network.NController.class);
        boot.launchAgent("producer", Agents.AgentProducer.class);        
    }

}
