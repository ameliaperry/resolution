import java.util.*;


/* Computes Ext_A^{s,t} (M, Z/2) through a minimal resolution of M. */
/* This seems to work effectively through about t=75, and then becomes prohibitively slow. */

class ResDefaultBackend implements ResBackend
{
    PingListener listener = null;

    HashMap<String,CellData> output = new HashMap<String,CellData>();
    static String keystr(int s, int t) {
        return s+","+t;
    }

    /* convenience methods for cell data lookup */
    int ngens(int s, int t) {
        CellData dat = output.get(keystr(s,t));
        if(dat == null) return -1;
        return dat.gens.length;
    }
    DModSet[] kbasis(int s, int t) {
        CellData dat = output.get(keystr(s,t));
        if(Config.DEBUG) Main.die_if(dat == null, "Data null in ("+s+","+t+")");
        return dat.kbasis;
    }
    @Override public Dot[] gens(int s, int t) {
        CellData dat = output.get(keystr(s,t));
        if(dat == null) return null;
        return dat.gens;
    }
    @Override public boolean isComputed(int s, int t) {
        return output.containsKey(keystr(s,t));
    }



    /* Main minimal-resolution procedure. */

    @Override public void start()
    {
        long start;
        if(Config.TIMING) start = System.currentTimeMillis();

        Dot bottom_dot = new Dot(0,0,0);

        for(int t = 0; t <= Config.T_CAP; t++) {

            /* first handle the s=0 case: kludge a start */
            CellData dat0 = new CellData();
            if(t == 0) {
                dat0.gens = new Dot[] { bottom_dot };
                dat0.kbasis = new DModSet[] {};
            } else {
                dat0.gens = new Dot[] {};
                List<DModSet> kbasis0 = new ArrayList<DModSet>();
                for(Sq q : Sq.steenrod(t)) {
                    DModSet ms = new DModSet();
                    ms.add(new Dot(bottom_dot, q), 1);
                    kbasis0.add(ms);
                }
                dat0.kbasis = kbasis0.toArray(new DModSet[] {});
            }
            if(Config.STDOUT) System.out.printf("(%2d,%2d): %2d gen, %2d ker\n", 0, t, 0, dat0.kbasis.length);
            output.put(keystr(0,t), dat0);


            /* now the typical s>0 case */

            for(int s = 1; s <= t; s++) {

                /* compute the basis for this resolution bidegree */
                ArrayList<Dot> basis_l = new ArrayList<Dot>();
                for(int gt = s; gt < t; gt++)
                    for(Dot d : gens(s,gt))
                        for(Sq q : Sq.steenrod(t - gt))
                            basis_l.add(new Dot(d,q));

                /* get the old kernel basis */
                DModSet[] okbasis = kbasis(s-1,t);

                /* compute what the map does in this basis. this takes maybe 10% of the running time */
                DotMatrix mat = new DotMatrix();
                if(Config.DEBUG) System.out.printf("(%d,%d) Map:\n",s,t);

                Collection<DModSet> easy_ker = new ArrayList<DModSet>();
                for(Dot dot : basis_l) {
                    /* compute the image of this basis vector */
                    dot.img = dot.base.img.times(dot.sq);
                    if(Config.DEBUG) System.out.println("Image of "+dot+" is "+dot.img);

                    if(dot.img.isEmpty()) {
                        easy_ker.add(new DModSet(dot));
                    } else { /* have to do actual linear algebra ... */
                        mat.put(dot, dot.img);
                    }
                }

                /* OK, do all the heavy lifting = linear algebra. timesuck */
                CellData dat = calc_gens(mat, okbasis,s,t);
                
                /* add in the "easy" elements */
                DModSet[] newkbasis = Arrays.copyOf(dat.kbasis, dat.kbasis.length + easy_ker.size());
                int i = 0; 
                for(DModSet k : easy_ker)
                    newkbasis[dat.kbasis.length + (i++)] = k;
                dat.kbasis = newkbasis;

                /* compute the novikov filtration on new generators */
                if(Config.MICHAEL_MODE) {
                    for(Dot d : dat.gens) {
                        d.nov = -1;
                        for(Dot o : d.img.keySet()) 
                            if(d.nov == -1 || o.nov < d.nov)
                                d.nov = o.nov;
                        if(Config.STDOUT) System.out.println("generator has extra grading "+d.nov);
                    }
                }
                
                /* list generators */
                if(dat.gens.length > 0 && Config.STDOUT) {
                    System.out.println("Generators:");
                    for(Dot g : dat.gens) System.out.println(g.img);
                }

                output.put(keystr(s,t), dat);

                print_result(t);
                if(Config.STDOUT) System.out.printf("(%2d,%2d): %2d gen, %2d ker\n\n", s, t, dat.gens.length, dat.kbasis.length);
                if(listener != null)
                    listener.ping();
            }

            if(Config.TIMING && t >= 1) {
                long elapsed = System.currentTimeMillis() - start;
                double log = Math.log(elapsed);
                double score = log / t; 
                System.out.printf("t=%d elapsed=%dms log/t=%f\n", t, elapsed, score);
            }
        }
    }
    
    
    /* Computes a basis complement to the image of mat inside the span of okbasis */

    private static CellData calc_gens(DotMatrix mat, DModSet[] okbasis, int s, int t)
    {
        /* sketch idea:
         * do RREF on okbasis.
         * apply the same row ops (matrix mult) to mat.
         * (should find that the zero lines of okbasis are zero in mat)
         * finish RREFing mat from this form
         * find the overall complement of mat, excluding these zero lines
         * transform back
         *
         * To do this more efficiently, we basically rref a huge augmentation: bokbasis | mat | id.
         *
         * The code for computing the kernel of mat was merged into this function (since
         * it was basically doing all the same computations), so this function now does
         * all of the interesting linear algebra.
         *
         * Probably the last step of inverting a matrix via RREF could be sped up by keeping better
         * track of row operations used in earlier steps...
         */
        CellData ret = new CellData();

        /* choose an ordering on all keys and values */
        Dot[] keys = mat.keySet().toArray(new Dot[] {});
        DModSet val_set = new DModSet();
        for(DModSet ms : okbasis)
            val_set.union(ms);
        if(Config.DEBUG) System.out.println("val_set: "+val_set);
        Dot[] values = val_set.toArray(); 
        if(Config.STDOUT) System.out.printf("o:%d k:%d v:%d\n", okbasis.length, keys.length, values.length);

        /* construct our huge augmented behemoth */
        int[][] aug = new int[values.length][okbasis.length + keys.length + values.length];
        for(int i = 0; i < values.length; i++) {
            for(int j = 0; j < okbasis.length; j++)
                aug[i][j] = ResMath.dmod(okbasis[j].getsafe(values[i]));
            for(int j = 0; j < keys.length; j++)
                aug[i][j + okbasis.length] = ResMath.dmod(mat.get(keys[j]).getsafe(values[i]));
            for(int j = 0; j < values.length; j++)
                aug[i][j + okbasis.length + keys.length] = (i == j ? 1 : 0);
        }
        Matrices.printMatrix("aug", aug);

        /* rref it */
        int l1 = Matrices.rref(aug, keys.length + values.length).length;
        Matrices.printMatrix("rref(aug)", aug);

        /* extract mat | id */
        int[][] bmatrr = new int[values.length][keys.length + values.length];
        for(int i = 0; i < values.length; i++)
            for(int j = 0; j < keys.length + values.length; j++)
                bmatrr[i][j] = aug[i][j + okbasis.length];
        Matrices.printMatrix("bmatrr", bmatrr);

        /* rref it some more */
        int[] bmatrr_leads = Matrices.rref(bmatrr, values.length);
        int l2 = bmatrr_leads.length;
        Matrices.printMatrix("rref(bmatrr)", bmatrr);
        if(Config.DEBUG) System.out.printf("l1: %2d   l2: %2d\n", l1, l2);

        /* read out the kernel */
        List<DModSet> ker = new ArrayList<DModSet>();

        int idx = 0;
        for(int j = 0; j < keys.length; j++) {

            /* keep an eye out for leading ones and skip them */
            if(idx < bmatrr_leads.length && bmatrr_leads[idx] == j) {
                idx++;
                continue;
            }

            /* not a leading column, so we obtain a kernel element */
            DModSet ms = new DModSet();
            ms.add(keys[j], 1);
            for(int i = 0; i < values.length; i++) {
                if(bmatrr[i][j] != 0) {
                    if(Config.DEBUG) Main.die_if(i >= bmatrr_leads.length, "bad rref: no leading one");
                    ms.add(keys[bmatrr_leads[i]], -bmatrr[i][j]);
                }
            }

            ker.add(ms);
        }
        ret.kbasis = ker.toArray(new DModSet[]{});

        if(Config.DEBUG && ker.size() != 0) {
            System.out.println("Kernel:");
            for(DModSet dm : ret.kbasis)
                System.out.println(dm);
        }


        /* extract and invert (via rref, tracking only the relevant part) the row transform matrix */
        int[][] transf = new int[values.length][2 * values.length];
        for(int i = 0; i < values.length; i++) {
            for(int j = 0; j < values.length; j++)
                transf[i][j] = bmatrr[i][j + keys.length];
            for(int j = 0; j < l1 - l2; j++)
                transf[i][j + values.length] = (i == j + l2 ? 1 : 0);
        }
        Matrices.printMatrix("transf", transf);
        Matrices.rref(transf, values.length);
        Matrices.printMatrix("rref(transf)", transf); 

        /* read off the output */
        ret.gens = new Dot[l1 - l2];
        for(int j = 0; j < l1 - l2; j++) {
            ret.gens[j] = new Dot(s,t,j);
            for(int i = 0; i < values.length; i++)
                ret.gens[j].img.add(values[i], transf[i][j + values.length]);
        }

        return ret;
    }


    private void print_result(int s_max)
    {
        if(!Config.STDOUT) return;
        for(int s = s_max; s >= 0; s--) {
            for(int t = s; ; t++) {
                int n = ngens(s,t);
                if(n < 0) break;
                if(n > 0)
                    System.out.printf("%2d ", n);
                else
                    System.out.print("   ");
            }
            System.out.println("###");
        }
    }


    public void register_listener(PingListener p)
    {
        listener = p;
    }
}


class CellData
{
    Dot[] gens;
    DModSet[] kbasis; /* kernel basis dot-sums in bidegree s,t */

    CellData() { }
    CellData(Dot[] g, DModSet[] k) {
        gens = g;
        kbasis = k;
    }
}


class DotMatrix extends HashMap<Dot,DModSet>
{
}

