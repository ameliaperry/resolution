package res.transform;

import res.algebratypes.*;
import res.algebras.*;
import java.awt.Color;
import java.util.*;

public class ProductDecorated<U extends MultigradedElement<U>, T extends MultigradedAlgebraComputation<Generator<U>>> extends Decorated<Generator<U>,T>
{

    Collection<ProductRule> rules;

    public ProductDecorated(T und, Collection<ProductRule> rules)
    {
        super(und);
        this.rules = rules;
    }

    @Override public Collection<BasedLineDecoration<Generator<U>>> getBasedLineDecorations(Generator<U> g)
    {
        ArrayList<BasedLineDecoration<Generator<U>>> ret = new ArrayList<BasedLineDecoration<Generator<U>>>();
        if(g.deg[0] == 0) return ret;

        /* Currently this is ugly and only makes sense for the Bruner backend. Instead, query products abstractly
         * from the backend. This probably requires the Bruner backend to pre-compute products for efficiency. */
        for(ProductRule rule : rules) if(!rule.hide) {
            for(Object dot : g.img.keySet()) {
                Dot<U> casted = (Dot<U>) dot;
                if(casted.sq.equals(rule.trigger))
                    ret.add(new BasedLineDecoration<Generator<U>>(g, casted.base, rule.color));
            }
        }

        return ret;
    }
}

