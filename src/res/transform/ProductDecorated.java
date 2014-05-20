package res.transform;

import res.algebra.*;
import java.util.*;

public class ProductDecorated<U extends MultigradedElement<U>, T extends MultigradedAlgebra<U>> extends Decorated<U,T>
{
    Collection<ProductRule> rules;

    public ProductDecorated(T und, Collection<ProductRule> rules)
    {
        super(und);
        this.rules = rules;
    }

    @Override public boolean isVisible(U u)
    {
        /* TODO */
        return true;
    }

    @Override public Collection<BasedLineDecoration<U>> getBasedLineDecorations(U u)
    {
        /* TODO */
        return null;
    }

    @Override public Collection<UnbasedLineDecoration<U>> getUnbasedLineDecorations(U u)
    {
        /* TODO */
        return null;
    }
}

