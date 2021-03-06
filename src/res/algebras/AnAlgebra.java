package res.algebras;

import res.*;
import res.algebratypes.*;
import java.util.*;

/* The subalgebra of the Steenrod algebra generated by Sq^1, Sq^2, Sq^4, ..., Sq^{2^n},
 * or for odd primes, B, P^1, P^p, etc. */
public class AnAlgebra extends AbstractGradedAlgebra<AnElement>
{
    int N;
    public AnAlgebra(int _N) {
        N = _N;

        // precompute hopf elements
        hopf.add(new AnElement(new ModSet<Sq>(Sq.HOPF[0]),1));
        int pow = Config.Q;
        for(int i = 1; i <= N; i++) {
            Sq sq = new Sq(pow);
            AnElement elt = new AnElement(new ModSet<Sq>(sq), pow);
            hopf.add(elt);
            pow *= Config.P;
        }
    }

    ArrayList<AnElement> hopf = new ArrayList<AnElement>();

    // cache everything
    Map<Integer, Map<Sq,AnElement>> basis = new TreeMap<Integer, Map<Sq,AnElement>>();
    Map<AnElement,Map<AnElement,ModSet<AnElement>>> mult = new TreeMap<AnElement,Map<AnElement,ModSet<AnElement>>>();

    @Override public Iterable<AnElement> gens(int n)
    {
        Map<Sq,AnElement> ret = basis.get(n);
        if(ret != null) return ret.values();

        ret = new TreeMap<Sq,AnElement>();
        if(n == 0) {
            ret.put(Sq.UNIT, AnElement.UNIT);
        } else {
            // add a new hopf element as appropriate
            for(AnElement elt : hopf) if(elt.deg == n) {
                Sq sq = new Sq(new int[] {n});
                ret.put(sq, elt);
            }

            // multiply everything from lower degrees
            for(int d = 1; d < n; d++) {
                for(AnElement a : gens(d)) {
                    if(! mult.containsKey(a))
                        mult.put(a, new TreeMap<AnElement,ModSet<AnElement>>());

                    for(AnElement b : gens(n-d)) {
                        // multiply
                        ModSet<Sq> prod = new ModSet<Sq>();
                        for(Map.Entry<Sq,Integer> e1 : a.modset.entrySet()) {
                            for(Map.Entry<Sq,Integer> e2 : b.modset.entrySet()) {
                                ModSet<Sq> sqprod = e1.getKey().times(e2.getKey());
                                int valprod = e1.getValue() * e2.getValue();
                                prod.add(sqprod, valprod);
                            }
                        }

                        // reduce by Gaussian elimination
                        Sq highKey = null;
                        int highVal = -1;
                        ModSet<AnElement> eltprod = new ModSet<AnElement>();
                        while(! prod.isEmpty()) {
                            Map.Entry<Sq,Integer> high = prod.lastEntry();
                            highKey = high.getKey();
                            highVal = high.getValue();

                            AnElement toCancel = ret.get(high.getKey());
                            if(toCancel == null) {
                                break; // should add this to the basis
                            } else {
                                eltprod.add(toCancel, highVal);
                                prod.add(toCancel.modset, -highVal);
                            }
                        }

                        // add to basis if appropriate
                        if(! prod.isEmpty()) {
                            ModSet<Sq> scaled = prod.scaled(ResMath.inverse[highVal]);
                            AnElement elt = new AnElement(scaled,n);
                            ret.put(highKey, elt);
                            eltprod.add(elt,highVal);
//                            System.out.println("Obtained "+elt+" from "+a+" * "+b);
                        }

                        // register the product
                        mult.get(a).put(b, eltprod);
                    }
                }
            }
        }

        basis.put(n,ret);
        return ret.values();
    }

    @Override public ModSet<AnElement> times(AnElement a, AnElement b)
    {
        if(a == AnElement.UNIT) return new ModSet<AnElement>(b);
        if(b == AnElement.UNIT) return new ModSet<AnElement>(a);
        return mult.get(a).get(b);
    }

    @Override public AnElement unit()
    {
        return AnElement.UNIT;
    }

    @Override public List<AnElement> distinguished() {
        return hopf;
    }
}


