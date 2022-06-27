/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package StrongHierarchy;

import jobshop.Operations;

/**
 *
 * @author Anatoli Grishenko <Anatoli.Grishenko@gmail.com>
 */
public class HMDrillB extends HMachine{
    
    @Override
    public void setup(){
        super.setup();
        myMachine.addCapability(Operations.DRILL, 6, 4);
        this.DFAddMyServices(new String []{Operations.DRILL.name()});
        Info("Setup "+this.getClass().getName());
    }

}
