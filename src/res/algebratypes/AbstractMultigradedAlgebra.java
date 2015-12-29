package res.algebratypes;

import java.util.*;

public abstract class AbstractMultigradedAlgebra<T> extends AbstractMultigradedModule<T,T> implements MultigradedAlgebra<T>
{
    @Override public abstract T unit();
    @Override public List<T> distinguished() {
        return Collections.emptyList();
    }
}

