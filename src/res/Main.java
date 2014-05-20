package res;

import res.algebra.*;
import res.backend.*;
import res.frontend.*;
import res.transform.*;

import java.awt.Color;
import java.util.*;
import javax.swing.JOptionPane;

public class Main {
    
    public static void die_if(boolean test, String fail)
    {
        if(test) {
            System.err.println(fail);
            Thread.dumpStack();
            System.err.println("Failing.");
            System.exit(1);
        }
    }

    public static void main(String[] args)
    {
        String s;
        SettingsDialog sd = new SettingsDialog();
        sd.setVisible(true); /* blocks until dialog has completed */

        if(sd.cancelled)
            System.exit(0);

        /* prime */
        Config.P = (Integer) sd.prime.getSelectedItem();
        Config.Q = 2 * (Config.P - 1);
        ResMath.calcInverses();

        /* p=2 mode */
        Config.MICHAEL_MODE = sd.oddrel.isSelected();

        /* T cap */
        Config.T_CAP = (Integer) sd.maxt.getValue();

        /* threads */
        Config.THREADS = (Integer) sd.threads.getValue();

        /* at some point we'll have to think about how to do this process in a properly generic way */

        /* algebra */
        GradedAlgebra<Sq> alg = new SteenrodAlgebra();

        /* backend */
        BrunerBackend<Sq> back;
//        s = sd.back.getSelection().getActionCommand();
        back = new BrunerBackend<Sq>(alg);

        /* module */
        GradedModule<Sq> mod;
        s = (String) sd.modcombo.getSelectedItem();
        if(s == SettingsDialog.MODCOF2)
            mod = new CofibHopf(0,alg);
        else if(s == SettingsDialog.MODCOFETA)
            mod = new CofibHopf(1,alg);
        else if(s == SettingsDialog.MODCOFNU)
            mod = new CofibHopf(2,alg);
        else if(s == SettingsDialog.MODCOFSIGMA)
            mod = new CofibHopf(3,alg);
        else if(s == SettingsDialog.MODEXCESS) {
            int exct = -1;
            while(exct < 0) {
                String excstr = JOptionPane.showInputDialog(null, "Excess less than or equal to what T?");
                try {
                    exct = Integer.parseInt(excstr);
                } catch(NumberFormatException e) {}
            }
            mod = new ExcessModule(exct,alg);
        } else
            mod = new Sphere<Sq>(alg);

        back.setModule(mod);

        /* decorators */
        /* TODO this is currently hard-coded */
        CompoundDecorated<Generator<Sq>,MultigradedAlgebra<Generator<Sq>>> dec = new CompoundDecorated<Generator<Sq>,MultigradedAlgebra<Generator<Sq>>>(back);

        Collection<DifferentialRule> diffrules = new ArrayList<DifferentialRule>();
//        diffrules.add(new DifferentialRule(new int[] {2,1,1}, new int[] {1,1,0}, Color.green));
//        diffrules.add(new DifferentialRule(new int[] {1,0,2}, new int[] {0,0,1}, Color.red));
        dec.add(new DifferentialDecorated<Generator<Sq>,MultigradedAlgebra<Generator<Sq>>>(back, diffrules));

        /* TODO add product decorated */

        /* frontend */
        s = sd.front.getSelection().getActionCommand();
        if(s == SettingsDialog.FRONT3D)
            ResDisplay3D.constructFrontend(dec);
        else
            ResDisplay.constructFrontend(dec);


        /* off we go */
        back.start();
    }

}

