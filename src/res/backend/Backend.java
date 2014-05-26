package res.backend;

import res.algebra.*;
import res.transform.*;

public interface Backend<T extends MultigradedElement<T>, U extends MultigradedVectorSpace<T>> {
    Decorated<T,U> getDecorated();
    void start();
}

