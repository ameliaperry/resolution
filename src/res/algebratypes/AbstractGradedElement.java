package res.algebratypes;

public abstract class AbstractGradedElement<T> implements GradedElement<T>
{
    /* implementations should override equals(Object) */
    @Override public abstract int compareTo(T t) { }
    @Override public abstract int deg();
    @Override public String extraInfo() { }
    @Override public int[] deg() {
        return new int[] { deg() };
    }
}

