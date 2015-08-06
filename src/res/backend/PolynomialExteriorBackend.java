package res.backend;

import res.*;
import res.algebra.*;
import res.transform.*;
import java.awt.Color;
import java.util.*;
import javax.swing.*;


public class PolynomialExteriorBackend
    extends MultigradedAlgebra<PEMonomial>
    implements Backend<PEMonomial, MultigradedAlgebra<PEMonomial>>
{

    private int num_gradings = -1;
    private PEGenerator[] gens;
    private Map<int[],Set<PEMonomial>> monsByMultidegree = new TreeMap<int[],Set<PEMonomial>>(Multidegrees.multidegComparator);
    private Map<int[],Set<PEMonomial>> monsByBidegree    = new TreeMap<int[],Set<PEMonomial>>(Multidegrees.multidegComparator);

    private PriorityQueue<PEQueueElt> queue = new PriorityQueue<PEQueueElt>();
    private int curr = 0;


    public PolynomialExteriorBackend() {
        Box box = new Box(BoxLayout.Y_AXIS);
        JTextArea ta = new JTextArea(50,7);
        JScrollPane sp = new JScrollPane(ta);
        sp.getViewport().setPreferredSize(new java.awt.Dimension(470,200));;
        box.add(new JLabel("Syntax: name [E or P] grading1 grading2 ..."));
        box.add(new JLabel("Example: h10 E 1 8"));
        box.add(new JLabel("Enter multiple generators on separate lines."));
        box.add(sp);
        if(JOptionPane.showConfirmDialog(null,box,"Specify generators",JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
            System.exit(0);

        ArrayList<PEGenerator> genlist = new ArrayList<PEGenerator>();
        for(String l : ta.getText().split("\n")) {
            l = l.trim();
            if(l.equals("")) continue;
            String[] toks = l.split(" ");
            PEGenerator g = new PEGenerator();
            g.name = toks[0];
            g.exterior = toks[1].toLowerCase().charAt(0) == 'e';
            if(num_gradings == -1)
                num_gradings = toks.length - 2;
            else Main.die_if(num_gradings != toks.length-2, "Bad number of gradings.");
            g.deg = new int[num_gradings];
            for(int i = 0; i < num_gradings; i++) {
                g.deg[i] = Integer.parseInt(toks[i+2]);
                g.totaldeg += g.deg[i];
            }
            genlist.add(g);
        }
        gens = genlist.toArray(new PEGenerator[] {});
    }


    /* methods implementing MultigradedAlgebra */

    @Override public int num_gradings() {
        Main.die_if(num_gradings < 0, "PolynomialExteriorBackend has not yet been initialized -- number of gradings is unknown.");
        return num_gradings;
    }

    @Override public Collection<PEMonomial> gens(int[] i)
    {
        /* XXX hack */
        i[1] -= i[0];

        Collection<PEMonomial> ret = null;
        if(i.length == 2) {
            ret = monsByBidegree.get(i);
        } else ret = monsByMultidegree.get(i);
        if(ret == null) return Collections.emptyList();
        return ret;
    }

    @Override public int getState(int[] i)
    {
        if(i.length < 2) return STATE_VANISHES;

        /* XXX hack */
        i[1] -= i[0];

        int tot = 0;
        for(int j : i) {
            if(j < 0) return STATE_VANISHES;
            tot += j;
        }
        if(tot < curr) return STATE_DONE;
        if(tot == curr && tot < Config.T_CAP) return STATE_STARTED;
        return STATE_NOT_COMPUTED;
    }

    @Override public ModSet<PEMonomial> times(PEMonomial a, PEMonomial b)
    {
        int[] ret = new int[gens.length];
        for(int i = 0; i < gens.length; i++)
            ret[i] = a.exponents[i] + b.exponents[i];
        return new ModSet<PEMonomial>(makeMonom(ret));
    }

    @Override public void start()
    {
        PEQueueElt initelt = new PEQueueElt();
        initelt.deg = 0;
        initelt.exp = new int[gens.length];
        initelt.min = 0;
        queue.offer(initelt);

        while(true) {
            PEQueueElt e = queue.poll();
            if(e == null) { 
                System.out.println("Computation ended: ran out of work.");
                curr = 1<<30;
                break; 
            }
            curr = e.deg;
            if(e.deg > Config.T_CAP) {
                System.out.println("Computation ended: hit max grading.");
                break;
            }

            /* add dot to picture */
            PEMonomial mon = makeMonom(e.exp);
            int[] bideg = bideg(mon.deg);
            insert(monsByMultidegree, mon.deg, mon);
            insert(monsByBidegree, bideg(mon.deg), mon);
            ping(new int[] { bideg[1], bideg[0]+bideg[1] });

            /* spawn children */
            for(int i = e.min; i < gens.length; i++) {
                PEGenerator g = gens[i];
                PEQueueElt next = new PEQueueElt();
                next.deg = e.deg + g.totaldeg;
                next.exp = Arrays.copyOf(e.exp, e.exp.length);
                next.exp[i]++;
                next.min = (g.exterior) ? i + 1 : i;
                queue.offer(next);
            }
        }
    }


    public Decorated<PEMonomial, MultigradedAlgebra<PEMonomial>> getDecorated()
    {
        /* TODO ? */
        return new TrivialDecorated<PEMonomial,MultigradedAlgebra<PEMonomial>>(this);

        /*

        Collection<ProductRule> prodrules = new ArrayList<ProductRule>();
        Color[] colors = new Color[] { new Color(128,0,0), new Color(0,128,0), new Color(0,0,128) };
        List<T> distinguished = alg.distinguished();
        for(int i = 0; i < colors.length && i < distinguished.size(); i++)
            prodrules.add(new ProductRule("h_"+i, distinguished.get(i), true, false, false, colors[i]));
        return new ProductDecorated<T,MultigradedAlgebra<Generator<T>>>(this, prodrules);
        */
    }


    private PEMonomial makeMonom(int[] exponents) {
        PEMonomial ret = new PEMonomial();
        ret.exponents = exponents;
        ret.deg = new int[num_gradings];
        ret.name = "";
        for(int i = 0; i < exponents.length; i++) if(exponents[i] > 0) {
            for(int j = 0; j < num_gradings; j++)
                ret.deg[j] += exponents[i] * gens[i].deg[j];
            ret.name += gens[i].name + "^" + exponents[i] + " ";
        }
        if(ret.name.equals("")) ret.name = "1";
        return ret;
    }

    private int[] bideg(int[] deg) {
        return new int[] { (deg.length > 0 ? deg[0] : 0), (deg.length > 1 ? deg[1] : 0) };
    }

    private void insert(Map<int[],Set<PEMonomial>> map, int[] key, PEMonomial val)
    {
        Set<PEMonomial> set = map.get(key);
        if(set == null) {
            set = new TreeSet<PEMonomial>();
            map.put(key,set);
        }
        set.add(val);
    }
}

class PEMonomial implements MultigradedElement<PEMonomial>
{
    int[] deg;
    int[] exponents;
    String name;

    @Override public int[] deg() {
        return deg;
    }

    @Override public String toString() {
        return name;
    }

    @Override public String extraInfo() {
        return "";
    }

    @Override public int compareTo(PEMonomial o)
    {
        for(int i = 0; i < exponents.length; i++) {
            int d = exponents[i] - o.exponents[i];
            if(d != 0) return d;
        }
        return 0;
    }
}


class PEGenerator
{
    int[] deg;
    int totaldeg;
    String name;
    boolean exterior;
}

class PEQueueElt implements Comparable<PEQueueElt>
{
    int deg;
    int[] exp;
    int min;

    @Override public int compareTo(PEQueueElt o) {
        return deg - o.deg;
    }
}


