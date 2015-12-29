package res.algebras;

import res.*;
import res.algebratypes.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

public class BrunerNotationModule extends AbstractGradedModule<BrunerNotationElement,Sq>
{
    ArrayList<BrunerNotationElement> dots = new ArrayList<BrunerNotationElement>();
    Map<Pair<BrunerNotationElement,Integer>,ModSet<BrunerNotationElement>> actions = new TreeMap<Pair<BrunerNotationElement,Integer>,ModSet<BrunerNotationElement>>();


    public BrunerNotationModule()
    {
        JFileChooser jfc = new JFileChooser();
        jfc.setDialogTitle("Load module...");
        int ret = jfc.showOpenDialog(null);
        if(ret != JFileChooser.APPROVE_OPTION) {
            System.err.println("User aborted module loading.");
            System.exit(1);
        }

        try {
            load(jfc.getSelectedFile());
        } catch(IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load module.");
            System.exit(1);
        }
    }

    private void load(File f) throws IOException
    {
        BufferedReader reader = new BufferedReader(new FileReader(f));

        /* line 1: total dimension */
        int n = Integer.parseInt(reader.readLine());

        /* line 2: degrees of generators */
        String[] toks = reader.readLine().split(" ");
        if(toks.length != n)
            throw new RuntimeException("Error in bruner-notation module: number of degrees of generators on line 2 is not the given number of generators on line 1");

        for(int i = 0; i < n; i++) {
            int deg = Integer.parseInt(toks[i]);
            dots.add(new BrunerNotationElement(i,deg));
        }


        /* remaining lines: actions */
        for(String str = reader.readLine(); str != null && ! str.trim().equals(""); str = reader.readLine()) {
            if(str.trim().equals("")) continue;

            toks = str.split(" ");
            int g = Integer.parseInt(toks[0]);
            int r = Integer.parseInt(toks[1]);
            int k = Integer.parseInt(toks[2]);

            ModSet<BrunerNotationElement> set = new ModSet<BrunerNotationElement>();

            if(Config.P == 2) {
                if(toks.length != 3 + k)
                    throw new RuntimeException("Bruner notation: invalid action specification for Sq^"+r+" g_"+g);
                for(int i = 3; i < 3+k; i++)
                    set.add(dots.get(Integer.parseInt(toks[i])),1);
            } else {
                if(toks.length != 3 + 2*k)
                    throw new RuntimeException("Bruner notation: invalid action specification for Sq^"+r+" g_"+g);
                for(int i = 0; i < k; i++)
                    set.add(dots.get(Integer.parseInt(toks[3+2*i])), Integer.parseInt(toks[4+2*i]));
            }

            actions.put(new Pair<BrunerNotationElement,Integer>(dots.get(g),r), set);
        }

        reader.close();
    }

    @Override public Iterable<BrunerNotationElement> gens(int deg)
    {
        ArrayList<BrunerNotationElement> ret = new ArrayList<BrunerNotationElement>();
        for(BrunerNotationElement el : dots)
            if(el.deg == deg) ret.add(el);
        return ret;
    }

    static ModSet<BrunerNotationElement> zero = new ModSet<BrunerNotationElement>();
    @Override public ModSet<BrunerNotationElement> times(BrunerNotationElement o, Sq sq)
    {
        if(sq.q.length == 0)
            return new ModSet<BrunerNotationElement>(o);
        else if(sq.q.length == 1) {
            ModSet<BrunerNotationElement> ret = actions.get(new Pair<BrunerNotationElement,Integer>(o,sq.q[0]));
            if(ret != null) return ret;
            return zero;
        } else {
            int[] sqcopy = new int[sq.q.length-1];
            for(int i = 0; i < sq.q.length-1; i++) sqcopy[i] = sq.q[i];
            Sq next = new Sq(sqcopy);
            Sq curr = new Sq(sq.q[sq.q.length-1]);
            return times(o, curr).times(next,this);
        }
    }

}

