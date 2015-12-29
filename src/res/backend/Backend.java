package res.backend;

import res.algebratypes.*;
import res.transform.*;

public interface Backend<T extends MultigradedElement<T>, U extends MultigradedComputation<T>> {
    Decorated<T,U> getDecorated();
    void start();
}

