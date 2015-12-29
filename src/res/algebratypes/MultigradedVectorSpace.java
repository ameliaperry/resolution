package res.algebratypes;

import java.util.Collection;

public interface MultigradedVectorSpace<T>
{
    public int num_gradings();
    public Iterable<T> gens(int[] i);
}

