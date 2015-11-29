package res.algebratypes;

public interface Module<T,U>
{
    public ModSet<T> times(T o, U sq);
}

