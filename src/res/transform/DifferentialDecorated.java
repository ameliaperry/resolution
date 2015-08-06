package res.transform;

import res.algebra.*;
import java.util.*;

/* TODO be clever and use the Leibniz rule */
/* TODO use user information about known differentials */
public class DifferentialDecorated<U extends MultigradedElement<U>, T extends MultigradedVectorSpace<U>> extends Decorated<U,T>
{
    Collection<DifferentialRule> rules;

    public DifferentialDecorated(T t, Collection<DifferentialRule> rules) {
        super(t);
        this.rules = rules;
    }
    public DifferentialDecorated(T t, String rules) {
        this(t, DifferentialRule.parse(rules));
    }

    @Override public Collection<BasedLineDecoration<U>> getBasedLineDecorations(U u)
    {
        Collection<BasedLineDecoration<U>> ret = new ArrayList<BasedLineDecoration<U>>();

        T und = underlying();

        for(DifferentialRule rule : rules) {
            int[] i = Arrays.copyOf(rule.initial, rule.initial.length);
            for(int j = 0; j < i.length && j < u.deg().length; j++)
                i[j] += u.deg()[j];
            while(und.getState(i) >= MultigradedVectorSpace.STATE_OK_TO_QUERY) {
                if(und.gens(i) == null) {
                    System.out.print("null gens at i: ");
                    for(int k : i)
                        System.out.print(k + ",");
                    System.out.println();
                }
                for(U o : und.gens(i)) {
                    ret.add(new BasedLineDecoration<U>(u, o, rule.color)); 
                }

                for(int j = 0; j < i.length; j++)
                    i[j] += rule.step[j];
            }
        }
        return ret;
    }

}

