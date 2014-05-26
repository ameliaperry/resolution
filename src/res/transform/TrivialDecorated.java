package res.transform;

import res.algebra.*;
import java.util.*;

public class TrivialDecorated<U extends MultigradedElement<U>, T extends MultigradedVectorSpace<U>> extends Decorated<U,T>
{
    public TrivialDecorated(T t) {
        super(t);
    }

    public Collection<BasedLineDecoration<U>> getBasedLineDecorations()
    {
        return Collections.emptyList();
    }

    public Collection<UnbasedLineDecoration<U>> getUnbasedLineDecorations()
    {
        return Collections.emptyList();
    }
}

