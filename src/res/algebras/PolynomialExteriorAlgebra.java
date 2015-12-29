package res.algebras;

import res.*;
import res.algebratypes.*;
import res.transform.*;
import java.awt.Color;
import java.util.*;

public class PolynomialExteriorAlgebra extends AbstractMultigradedAlgebra<PEMonomial>
{
    private int num_gradings = 0;
    private PEGenerator[] gens;
    private Map<int[],Set<PEMonomial>> cache = new TreeMap<int[],Set<PEMonomial>>(Multidegrees.multidegComparator);

    public PolynomialExteriorAlgebra(int[][] egens, String[] enames, int[][] pgens, String[] pnames)
    {
        gens = new PEGenerator[egens.length + pgens.length];
        int idx = 0;
        for(int i = 0; i < egens.length; i++) {
            PEGenerator g = new PEGenerator();
            g.deg = egens[i];
            for(int j : g.deg) g.totaldeg += j;
            g.name = enames[i];
            g.exterior = true;
            gens[idx++] = g;
            if(egens[i].length > num_gradings) num_gradings = egens[i].length;
        }
        for(int i = 0; i < pgens.length; i++) {
            PEGenerator g = new PEGenerator();
            g.deg = pgens[i];
            for(int j : g.deg) g.totaldeg += j;
            g.name = pnames[i];
            g.exterior = false;
            gens[idx++] = g;
            if(pgens[i].length > num_gradings) num_gradings = pgens[i].length;
        }
    }

    public PolynomialExteriorAlgebra(PEGenerator[] gens) {
        this.gens = gens;
    }


    /* methods implementing MultigradedAlgebra */
    @Override public int num_gradings() {
        return num_gradings;
    }

    @Override public PEMonomial unit() {
        return makeMonom(new int[gens.length]);
    }

    @Override public synchronized Iterable<PEMonomial> gens(int[] deg)
    {
        Set<PEMonomial> ret = cache.get(deg);
        if(ret != null) return ret;
        ret = new TreeSet<PEMonomial>();

gen_loop:
        for(int g = 0; g < gens.length; g++) {
            int[] odeg = new int[deg.length];
            for(int i = 0; i < deg.length; i++) {
                odeg[i] = deg[i] - gens[g].deg[i];
                if(odeg[i] < 0) continue gen_loop;
            }

            for(PEMonomial omon : gens(odeg)) {
                int[] exp = omon.exponents.clone();
                exp[g]++;
                if(exp[g] > 1 && gens[g].exterior) continue;
                ret.add(makeMonom(exp));
            }
        }

        cache.put(deg,ret);
        return ret;
    }

    @Override public ModSet<PEMonomial> times(PEMonomial a, PEMonomial b)
    {
        int[] ret = new int[gens.length];
        for(int i = 0; i < gens.length; i++) {
            ret[i] = a.exponents[i] + b.exponents[i];
            if(ret[i] > 1 && gens[i].exterior) return new ModSet<PEMonomial>();
        }
        return new ModSet<PEMonomial>(makeMonom(ret));
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
}

