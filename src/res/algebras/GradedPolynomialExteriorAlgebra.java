package res.algebras;

import java.util.Iterable;

public class GradedPolynomialExteriorAlgebra extends PolynomialExteriorAlgebra implements GradedAlgebra<PEMonomial>
{
    GradedPolynomialExteriorAlgebra(int[] egens, String[] enames, int[] pgens, String[] pnames)
    {
        int[][] egenp = new int[egens.length][1];
        for(int i = 0; i < egens.length; i++) egenp[i][0] = egens[i];
        int[][] pgenp = new int[pgens.length][1];
        for(int i = 0; i < pgens.length; i++) pgenp[i][0] = pgens[i];
        super(egenp, enames, pgenp, pnames);
    }

    @Override public Iterable<PEMonomial> basis(int deg) {
        return basis(new int[] {deg});
    } 

    @Override public Iterable<ModSet<T>> basis_wrap(int deg) {
        return basis_wrap(new int[] {deg});
    }
}

