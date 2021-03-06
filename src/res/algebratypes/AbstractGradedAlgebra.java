package res.algebratypes;

import java.util.*;

public abstract class AbstractGradedAlgebra<T> extends AbstractGradedModule<T,T> implements GradedAlgebra<T>
{
    @Override public abstract T unit();
    @Override public List<T> distinguished() {
        return Collections.emptyList();
    }
}

