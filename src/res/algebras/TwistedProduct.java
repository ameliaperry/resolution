package res.algebras;

import res.*;
import res.algebratypes.*;
import java.util.*;

public class TwistedProduct<T1 extends GradedElement<T1>, T2 extends GradedElement<T2>, A1 extends GradedAlgebraWithAction<T1,T2>, A2 extends GradedBialgebra<T2>> extends AbstractGradedAlgebra<GradedPair<T1,T2>>
{
    A1 alg1;
    A2 alg2;

    public TwistedProduct(A1 alg1, A2 alg2) {
        this.alg1 = alg1;
        this.alg2 = alg2;
    }

    @Override public Iterable<GradedPair<T1,T2>> gens(int deg)
    {
        ArrayList<GradedPair<T1,T2>> ret = new ArrayList<GradedPair<T1,T2>>();
        for(int d1 = 0; d1 <= deg; d1++)
            for(T2 t2 : alg2.gens(deg-d1))
                for(T1 t1 : alg1.gens(d1))
                    ret.add(new GradedPair<T1,T2>(t1,t2));
        return ret;
    }

    @Override public ModSet<GradedPair<T1,T2>> times(GradedPair<T1,T2> p1, GradedPair<T1,T2> p2)
    {
        ModSet<GradedPair<T1,T2>> ret = new ModSet<GradedPair<T1,T2>>();

        ModSet<Pair<T2,T2>> diag = alg2.diagonal(p1.b);
        for(Map.Entry<Pair<T2,T2>,Integer> e : diag.entrySet()) {
            int parity = (e.getKey().b.deg() * p2.a.deg()) % 2;

            ModSet<T2> prodR = alg2.times(e.getKey().b, p2.b);
            ModSet<T1> prodL1 = alg1.times_r(p2.a, e.getKey().a);
            for(Map.Entry<T1,Integer> e2 : prodL1.entrySet()) {
                ModSet<T1> prodL2 = alg1.times(p1.a, e2.getKey());
                for(Map.Entry<T1,Integer> e3 : prodL2.entrySet()) {
                    for(Map.Entry<T2,Integer> e4 : prodR.entrySet()) {

                        int coeff = e.getValue() * e2.getValue() * e3.getValue() * e4.getValue();
                        if(parity == 1) coeff = -coeff;
                        ret.add(new GradedPair<T1,T2>(e3.getKey(), e4.getKey()), coeff);
                    }
                }
            }
        }
        return ret;
    }

    @Override public GradedPair<T1,T2> unit()
    {
        return new GradedPair<T1,T2>(alg1.unit(), alg2.unit());
    }

    @Override public int num_gradings() {
        return 1;
    }

}

