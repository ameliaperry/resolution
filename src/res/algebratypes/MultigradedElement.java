package res.algebratypes;

public interface MultigradedElement<T> extends Comparable<T>
{
    int[] multideg();
    String extraInfo();
}

