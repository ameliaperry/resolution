package res.algebras;

import res.algebratypes.*;
import java.util.*;

public class FreeModule<T extends MultigradedElement<T>> extends AbstractMultigradedModule<Dot<T>,T>
{
    private MultigradedAlgebra<T> alg;
    private Iterable<Generator<T>> gens;

    public FreeModule(MultigradedAlgebra<T> alg, Iterable<Generator<T>> gens) {
        this.alg = alg;
        this.gens = gens;
    }
    public FreeModule(MultigradedAlgebra<T> alg) {
        this.alg = alg;
        this.gens = Collections.emptyList();
    }

    @Override public ModSet<Dot<T>> times(Dot<T> o, T sq) {
        ModSet<Dot<T>> ret = new ModSet<Dot<T>>();
        ModSet<T> pd = alg.times(o.sq, sq);
        for(Map.Entry<T,Integer> ent : pd.entrySet()) {
            Dot<T> dot = new Dot<T>(o.base, ent.getKey());
            ret.add(dot, ent.getValue());
        }
        return ret;
    }

    @Override public int num_gradings() {
        return alg.num_gradings();
    }

    @Override public Collection<Dot<T>> gens(int[] deg) {
        /* TODO */
        return null;
    }
}

