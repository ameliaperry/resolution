package res;

import res.algebra.*;
import res.backend.*;
import res.frontend.*;

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
        else
            mod = new Sphere<Sq>(alg);

        back.setModule(mod);

        /* frontend */
        s = sd.front.getSelection().getActionCommand();
        if(s == SettingsDialog.FRONT3D)
            ResDisplay3D.constructFrontend(back);
        else
            ResDisplay.constructFrontend(back);


        /* off we go */
        back.start();
    }

}
