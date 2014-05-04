
class Main {
    
    static void die_if(boolean test, String fail)
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

        /* backend */
        ResBackend back;
        s = sd.back.getSelection().getActionCommand();
        if(s == SettingsDialog.BACKOLD)
            back = new ResParallelizedBackend();
        else
            back = new BrunerBackend();

        /* module */
        AMod mod;
        s = (String) sd.modcombo.getSelectedItem();
        if(s == SettingsDialog.MODCOF2)
            mod = new CofibHopf(0);
        else if(s == SettingsDialog.MODCOFETA)
            mod = new CofibHopf(1);
        else if(s == SettingsDialog.MODCOFNU)
            mod = new CofibHopf(2);
        else if(s == SettingsDialog.MODCOFSIGMA)
            mod = new CofibHopf(3);
        else
            mod = new Sphere();

        /* ugly way to set a module. TODO change interface */
        if(back instanceof BrunerBackend)
            ((BrunerBackend) back).setModule(mod);

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
