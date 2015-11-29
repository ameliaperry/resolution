package res.algebras;

import res.algebratypes.*;
import java.util.*;

public class AnModuleWrapper extends GradedModule<Dot<AnElement>>
{
    GradedModule<Dot<Sq>> base;

    Map<Dot<Sq>,Dot<AnElement>> dmap = new TreeMap<Dot<Sq>,Dot<AnElement>>();
    Map<Dot<AnElement>,Dot<Sq>> rmap = new TreeMap<Dot<AnElement>,Dot<Sq>>();

    public AnModuleWrapper(GradedModule<Sq> _base)
    {
        base = _base;
    }

    @Override public Iterable<Dot<AnElement>> basis(int deg)
    {
        List<Dot<AnElement>> ret = new ArrayList<Dot<AnElement>>();
        for(Dot<Sq> old : base.basis(deg))
            ret.add(wrap(old));
        return ret;
    }

    @Override public ModSet<Dot<AnElement>> act(Dot<AnElement> o, AnElement elt)
    {
        Dot<Sq> under = rmap.get(o);
        ModSet<Dot<AnElement>> ret = new ModSet<Dot<AnElement>>();

        for(Map.Entry<Sq,Integer> sqe : elt.modset.entrySet()) {

            ModSet<Dot<Sq>> prod = base.act(under, sqe.getKey());
            for(Map.Entry<Dot<Sq>,Integer> de : prod.entrySet()) {

                Dot<AnElement> w = wrap(de.getKey());
                ret.add(w, sqe.getValue() * de.getValue());
            }
        }
        
        return ret;
    }

    static int gencount = 0;
    private Dot<AnElement> wrap(Dot<Sq> in) {
        Dot<AnElement> ret = dmap.get(in);
        if(ret != null) return ret;

        Generator<AnElement> gen = new Generator<AnElement>(in.deg, gencount++);
        ret = new Dot<AnElement>(gen, AnElement.UNIT);
        dmap.put(in,ret);
        rmap.put(ret,in);
        return ret;
    }
}

