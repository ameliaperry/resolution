package res.algebras;

public class BorelSteenrodAlgebra extends TwistedProduct<PEMonomial, Sq, BCp, SteenrodAlgebra>
{
    public class BorelSteenrodAlgebra() {
        super(BCp.create(), new SteenrodAlgebra());
    }
}

