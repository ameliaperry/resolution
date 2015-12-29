package res.utils;

import java.util.*;

public class Utils
{
    public static <T> Collection<T> collect(Iterable<T> it)
    {
        if(it instanceof Collection)
            return (Collection<T>) it;
        ArrayList<T> ret = new ArrayList<T>();
        for(T t : it) ret.add(t);
        return ret;
    }

}

