package res.algebra;

import java.util.Collection;

public interface MultigradedVectorSpace<T>
{
    public int num_gradings();
    public Collection<T> gens(int[] i);
}

