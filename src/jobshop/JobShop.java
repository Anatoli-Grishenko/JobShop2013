package jobshop;

import appboot.LARVABoot;

public class JobShop {

    static LARVABoot boot;

    public static void main(String[] args) {
        boot = new LARVABoot();
        boot.Boot("localhost", 1099);
        StrongHierarchy();
        boot.WaitToShutDown();

    }
    
    public static void StrongHierarchy() {
        boot.launchAgent("cutter1", StrongHierarchy.HMCutter1.class);
        boot.launchAgent("cutter+", StrongHierarchy.HMCutterPRO.class);
        boot.launchAgent("driller", StrongHierarchy.HMDrillB.class);
        boot.launchAgent("driller+", StrongHierarchy.HMDrillA.class);
        boot.launchAgent("polish1", StrongHierarchy.HMPolisher.class);
//        boot.launchAgent("polish2", StrongHierarchy.HMPolisher.class);
        boot.launchAgent("controller", StrongHierarchy.HController.class);
        boot.launchAgent("producer", Agents.AgentProducer.class);        
    }

}
