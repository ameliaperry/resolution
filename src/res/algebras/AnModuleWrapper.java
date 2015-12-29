package res.algebras;

import res.algebratypes.*;
import res.utils.*;
import java.util.*;

public class AnModuleWrapper<U extends GradedElement<U>> extends AbstractGradedModule<U,AnElement>
{
    GradedModule<U,Sq> base;

    public AnModuleWrapper(GradedModule<U,Sq> _base) {
        base = _base;
    }

    @Override public Iterable<U> gens(int deg)
    {
        return base.gens(deg);
    }

    @Override public ModSet<U> times(U o, AnElement elt)
    {
        ModSet<U> ret = new ModSet<U>();
        for(Map.Entry<Sq,Integer> sqe : elt.modset.entrySet()) 
            ret.add(base.times(o, sqe.getKey()), sqe.getValue());
        return ret;
    }
}

