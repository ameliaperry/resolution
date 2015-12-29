package res.transform;

import res.algebratypes.*;
import java.util.*;

public class CompoundDecorated<U extends MultigradedElement<U>, T extends MultigradedComputation<U>> extends Decorated<U,T>
{
    private Collection<Decorated<U,T>> sub;

    public CompoundDecorated(T u) {
        super(u);
        sub = new LinkedList<Decorated<U,T>>();
    }

    public boolean add(Decorated<U,T> d)
    {
        if(d.underlying() != underlying()) {
            System.err.println("Error adding decorated to CompoundDecorated: different underyling algebra");
            return false;
        }
        sub.add(d);
        return true;
    }

    @Override public boolean isVisible(U u)
    {
        for(Decorated<U,T> d : sub)
            if(! d.isVisible(u))
                return false;
        return true;
    }

    @Override public Collection<BasedLineDecoration<U>> getBasedLineDecorations(U u)
    {
        Collection<BasedLineDecoration<U>> ret = new ArrayList<BasedLineDecoration<U>>();
        for(Decorated<U,T> d : sub)
            ret.addAll(d.getBasedLineDecorations(u));
        return ret;
    }

    @Override public Collection<UnbasedLineDecoration<U>> getUnbasedLineDecorations(U u)
    {
        Collection<UnbasedLineDecoration<U>> ret = new ArrayList<UnbasedLineDecoration<U>>();
        for(Decorated<U,T> d : sub)
            ret.addAll(d.getUnbasedLineDecorations(u));
        return ret;
    }
 }
