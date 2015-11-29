package res.algebras;

import res.*;
import java.util.*;

public class TwistedProduct<T1 extends GradedElement<T1>, T2 extends GradedElement<T2>, A1 extends GradedAlgebra<T1>,GradedModule<T1,T2>, A2 extends GradedAlgebra<T2>,GradedCoalgebra<T2>> implements MultigradedAlgebra<Pair<T1,T2>>
{
    A1 alg1;
    A2 alg2;

    public TwistedProduct(A1 alg1, A2 alg2) {
        this.alg1 = alg1;
        this.alg2 = alg2;
    }

    @Override public Iterable<Pair<T1,T2>> basis(int[] deg)
    {
        ArrayList<Pair<T1,T2>> ret = new ArrayList<Pair<T1,T2>>();

        for(T2 t2 : alg2.basis(deg[1])) {
            for(T1 t1 : alg1.basis(deg[0])) {
                ret.add(new Pair<T1,T2>(t1,t2));
            }
        }
        return ret;
    }

    @Override public ModSet<Pair<T1,T2>> times(Pair<T1,T2> p1, Pair<T1,T2> p2)
    {
        ModSet<Pair<T1,T2>> ret = new ModSet<Pair<T1,T2>>();

        ModSet<Pair<T2,T2>> diag = alg2.diagonal(p1.b);
        for(Map.Entry<Pair<T2,T2>,Integer> e : diag) {
            parity = (e.getKey().b.deg() * p2.a.deg()) % 2;

            ModSet<T2> prodR = alg2.times(e.getKey().b, p2.b);
            prodL1 = alg1.times(p2.a, e.getKey().a);
            for(Map.Entry<T1,Integer> e2 : prodL1) {
                prodL2 = alg1.times(p1.a, e2.getKey());
                for(Map.Entry<T1,Integer> e3 : prodL2) {
                    for(Map.Entry<T2,Integer> e4 : prodR) {
                        coeff = e1.getValue() * e2.getValue() * e3.getValue() * e4.getValue();
                        if(parity == 1) coeff = -coeff;
                        ret.add(new Pair<T1,T2>(e3.getKey(), e4.getKey()), coeff);
                    }
                }
            }
        }
        return ret;
    }

    @Override public Sq unit()
    {
        return new Pair<T1,T2>(alg1.unit(), alg2.unit());
    }

    @Override public List<Sq> distinguished()
    {
        ArrayList<Sq> ret = new ArrayList<Sq>();
        /* TODO */
        return ret;
    }

}
