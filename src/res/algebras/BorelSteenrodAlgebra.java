package res.algebras;

import res.algebratypes.*;

public class BorelSteenrodAlgebra extends TwistedProduct<PEMonomial, Sq, BCp, SteenrodAlgebra>
{
    public BorelSteenrodAlgebra() {
        super(BCp.create(), new SteenrodAlgebra());
    }
}

