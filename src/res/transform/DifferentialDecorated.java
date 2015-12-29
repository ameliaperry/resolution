package res.transform;

import res.algebratypes.*;
import java.util.*;

/* TODO be clever and use the Leibniz rule. Also use user information about known differentials.
 * Can we encode these sorts of constraints as a SAT instance, and feed it into a solver?
 * Are the constraints linear over F_p? -- can we solve this problem via Gaussian elimination? */
public class DifferentialDecorated<U extends MultigradedElement<U>, T extends MultigradedComputation<U>> extends Decorated<U,T>
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
            for(int j = 0; j < i.length && j < u.multideg().length; j++)
                i[j] += u.multideg()[j];
            while(und.getState(i) >= MultigradedComputation.STATE_OK_TO_QUERY) {
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

