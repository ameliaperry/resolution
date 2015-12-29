package res.backend;

import res.*;
import res.algebratypes.*;
import res.algebras.*;
import res.transform.*;
import res.utils.Utils;
import java.awt.Color;
import java.util.*;
import java.util.concurrent.*;


/* Computes Ext_A^{s,t} (M, F_p) through a minimal resolution of M, following a paper of Bruner.
 * A is a multigraded algebra with non-negative grading, and only the unit in grading 0.
 * M is an A-module. */

public class BrunerBackend<M extends MultigradedElement<M>, T extends MultigradedElement<T>>
    extends MultigradedAlgebraComputation<Generator<T>>
    implements Backend<Generator<T>, MultigradedAlgebraComputation<Generator<T>>>
{
    /* internal states */
    private static final int STATE_KER_COMPUTED = 10;
    private int ngrad;
    private int ngrad_with_extra;

    private final MultigradedAlgebra<T> alg;
    private MultigradedModule<M,T> module;
    private MultigradedModule<Dot<T>,T> freemodule;

    private SortedMap<int[],Generator<T>> gens = new TreeMap<int[],Generator<T>>(Multidegrees.multidegComparator);
    private SortedMap<int[],ModSet<Dot<T>>> kbasis = new TreeMap<int[],ModSet<Dot<T>>>(Multidegrees.multidegComparator);
    private SortedMap<int[],Integer> status = new TreeMap<int[],Integer>(Multidegrees.multidegComparator);

    public BrunerBackend(MultigradedAlgebra<T> alg, MultigradedModule<M,T> module) {
        this.alg = alg;
        this.module = module;
        this.freemodule = new FreeModule<T>(alg);
        this.ngrad = alg.num_gradings() + 1;
        this.ngrad_with_extra = alg.unit().multideg().length + 1;
    }


    /* methods implementing MultigradedAlgebra */

    @Override public int num_gradings() {
        return ngrad;
    }

    private Iterable<Generator<T>> gens_range(int[] i, int[] j) {
        return gens.subMap(Multidegrees.minkey(i,ngrad_with_extra), Multidegrees.maxkey(j,ngrad_with_extra)).values();
    }
    @Override public Iterable<Generator<T>> gens(int[] i) { return gens_range(i,i); }

    @Override public ModSet<Generator<T>> times(Generator<T> a, Generator<T> b)
    {
        /* XXX TODO full product structure. So far we can only do products by Hopf elements */
        if(a.deg[0] != 1 || a.img.size() != 1) {
            if(b.deg[0] == 1 && b.img.size() == 1) return times(b,a);
            else throw new RuntimeException("Multiplication by non-Hopf elements is not yet supported.");
        }
        //Dot<T> op = (Dot<T>) a.img.keySet().iterator().next(); 
        Dot<T> op = (Dot<T>) a.img.keySet().iterator().next(); 
        int coeff = a.img.get(op);

        ModSet<Generator<T>> ret = new ModSet<Generator<T>>();
        int[] retdeg = Multidegrees.sumdeg(a.deg, b.deg);
        for(Generator<T> g : gens(retdeg)) {
            Integer i = g.img.get(op);
            if(i == null) continue;
            ret.add(g, i * coeff);
        }
        return ret;
    }

    @Override public Generator<T> unit()
    {
        Collection<Generator<T>> candidates = Utils.collect(gens(new int[ngrad]));
        if(candidates.size() == 1)
            return candidates.iterator().next();
        else return null; /* TODO don't fail silently */
    }





    /* task management */

    @Override public int getState(int[] i)
    {
        int sum = -i[0];
        for(int j = 1; j < i.length; j++) {
            if(i[j] < 0) return STATE_FORMALLY_VANISHES;
            sum += i[j];
        }
        if(sum < 0) return STATE_FORMALLY_VANISHES;
        /* TODO more sophisticated vanishing line ideas. An algebra should report a set of planes
         * cutting out its support, and we work from there */

        boolean exists_queue = false;
        boolean exists_started = false;
        boolean exists_done = false;
        synchronized(status) {
            Collection<Integer> stat = status.subMap(
                    Multidegrees.minkey(i,ngrad), Multidegrees.maxkey(i,ngrad)).values();
            for(int s : stat) {
                switch(s) {
                    case STATE_QUEUED: exists_queue = true; break;
                    case STATE_STARTED: 
                    case STATE_KER_COMPUTED: exists_started = true; break;
                    case STATE_DONE: exists_done = true; break;
                    default: throw new RuntimeException("Invalid cell state in BrunerBackend: " + s);
                }
            }
        }

        if(exists_done) {
            if(exists_queue || exists_started) return STATE_PARTIAL;
            else return STATE_DONE;
        } else if(exists_started) return STATE_STARTED;
        else if(exists_queue) return STATE_QUEUED;
        else return STATE_NOT_COMPUTED;
    }

    private boolean isKerComputed(int[] i)
    {
        Integer s = status.get(i);
        if(s == null) return false;
        int si = s;
        if(si == STATE_KER_COMPUTED || si == STATE_DONE) return true;
        return false;
    }


    long start;
    BlockingQueue<int[]> tasks = new PriorityBlockingQueue<int[]>(50,Multidegrees.multidegComparator);

    @Override public void start()
    {
        start = System.currentTimeMillis();
        tryQueue(new int[ngrad]); // seed the computation
        for(int i = 0; i < Config.THREADS; i++)
            new BrunerThread(this).start();
    }

    private boolean claim_grid(int[] deg)
    {
        synchronized(status) {
            if(! status.containsKey(deg)) {
                status.put(deg, STATE_QUEUED);
                return true;
            }
        }
        return false;
    }
    
    private void tryQueue(int[] deg)
    {
        if(claim_grid(deg)) {
            while(true) {
                try {
                    tasks.put(deg);
                    return;
                } catch(InterruptedException e) {
                    continue;
                }
            }
        }
    }

    private void tryQueueIfReady(int[] deg)
    {

        /* TODO totally unfinished -- this is just some relevant code:
        for(int g = 1; g < ngrad; g++) {
            int[] reqdeg = Arrays.copyOf(nextdeg);
            reqdeg[g]--;
            if(!isComputed(reqdeg)) {
                proceed = false;
                break;
            }
        }
        if(proceed) tryQueue(nextdeg);
        */
    }


    
    /* the actual computation */

    void compute(int[] deg)
    {
        if(deg[0] == 0) {
            int[] subdeg = Arrays.copyOfRange(deg,1,deg.length);
            ArrayList<ModSet<M>> okbasis = new ArrayList<ModSet<M>>();
            for(M m : module.gens(subdeg))
                okbasis.add(new ModSet<M>(m));
            compute_m(deg, module, okbasis);
        } else compute_m(deg, freemodule, kbasis.subMap(Multidegrees.minkey(deg,ngrad), Multidegrees.maxkey(deg,ngrad)).values());
    }

    private <N extends MultigradedElement<N>> void compute_m(
            int[] deg, MultigradedModule<N,T> mod, Collection<ModSet<N>> okbasis) {

        if(Config.DEBUG) System.out.println(Arrays.toString(deg));

        Map<N,ModSet<Dot<T>>> list_x = new TreeMap<N,ModSet<Dot<T>>>();
        Map<N,ModSet<N>> list_dx = new TreeMap<N,ModSet<N>>();
        ArrayList<ModSet<Dot<T>>> ker = new ArrayList<ModSet<Dot<T>>>();

        /* loop over existing dots in this degree, to see what's already been hit and determine the kernel */
        for(Generator<T> g : gens_range(new int[] {deg[0]}, deg)) { // genenerators that could yield such dots
            // compute the "dot degree" (amount to promote)
            int[] diffdeg = new int[ngrad-1];
            int[] gdeg = g.multideg();
            for(int i = 1; i < ngrad; i++) diffdeg[i-1] = deg[i] - gdeg[i];
            for(T q : alg.gens(diffdeg)) {

                ModSet<Dot<T>> x = new ModSet<Dot<T>>(new Dot<T>(g,q));
                /* compute the image */
                ModSet<N> dx = ((ModSet<N>) g.img).times(q, mod);
                if(Config.DEBUG) System.out.printf("1: %s --> %s", x, dx);

                /* reduce against the existing image */
                while(! dx.isEmpty()) {
                    Map.Entry<N,Integer> high = dx.lastEntry();
                    N d = high.getKey();
                    int coeff = high.getValue();

                    ModSet<Dot<T>> modx = list_x.get(d);
                    if(modx == null)
                        break;

                    x.add(modx, -coeff);
                    dx.add(list_dx.get(d), -coeff);
                    if(Config.DEBUG) System.out.printf(" reduces to %s --> %s", x, dx);
                }
                if(Config.DEBUG) System.out.println();

                if(dx.isEmpty()) { /* dx = 0, add to kernel */
                    if(Config.DEBUG) System.out.printf("Adding %s to kernel\n", x);
                    ker.add(x);
                } else { /* register this as the entry with highest dot <highest> */
                    Map.Entry<N,Integer> high = dx.lastEntry();
                    N d = high.getKey();
                    int coeff = high.getValue();
                    if(Config.DEBUG) System.out.println("highest term "+d);
                    if(Config.DEBUG) Main.die_if(list_x.containsKey(d), "key clash on "+d);
                    list_x.put(d, x.scaled(ResMath.inverse[coeff]));
                    list_dx.put(d, dx.scaled(ResMath.inverse[coeff]));
                }

            }
        }

        if(Config.DEBUG) {
            System.out.println("Dump of image:");
            for(N d : list_x.keySet())
                System.out.printf("%s : %s --> %s\n", d, list_x.get(d), list_dx.get(d));
        }
        
        /* free some memory */
        list_x = null;

        /* save the kernel data */
        int idx = 0;
        for(ModSet<Dot<T>> k : ker) {
            int[] degplus = Arrays.copyOf(deg, ngrad+1);
            degplus[ngrad] = idx++;
            kbasis.put(degplus,k);
        }
        synchronized(status) {
            status.put(deg, STATE_KER_COMPUTED);
        }
        
        /* kick off the first child task (+1 homological degree) -- only depends ker, not gens */
        int[] nextdeg = Arrays.copyOf(deg, ngrad);
        nextdeg[0]++;
        tryQueueIfReady(nextdeg);



        /* now see how we're doing with respect to the old kernel. modifies okbasis elements */
        idx = 0;
        for(ModSet<N> k : okbasis) {
            if(Config.DEBUG) System.out.printf("kernel element %s ", k);
            /* reduce against the image */
            while(! k.isEmpty()) {
                Map.Entry<N,Integer> ent = k.lastEntry();
                N d = ent.getKey();
                Integer coeff = ent.getValue();

                ModSet<N> moddx = list_dx.get(d);
                if(moddx == null)
                    break;
                int ocoeff = moddx.get(d);
                k.add(moddx, -coeff * ResMath.inverse[ocoeff]);
            }
            if(Config.DEBUG) System.out.printf("reduces to %s\n", k);

            if(k.isEmpty()) { /* successfully killed this kernel class */
                if(Config.DEBUG) System.out.println("has been already killed");
                continue;
            }

            if(Config.DEBUG) System.out.printf("adding a generator to kill %s\n", k);
            
            /* haven't yet killed this kernel class -- add a generator */
            
            /* compute the degree of the result:
             * hard degrees are taken as deg,
             * soft degrees are taken by maxing the things that are hit,
             * extra degree for the index */
            int[] gdeg = Arrays.copyOf(deg, ngrad_with_extra + 1);
            for(int g = ngrad; g < ngrad_with_extra; g++)
                for(N o : k.keySet())
                    if(gdeg[g] < o.multideg()[g]) gdeg[g] = o.multideg()[g];
            gdeg[ngrad_with_extra] = idx++;

            Generator<T> gen = new Generator<T>(gdeg, idx);
            gen.img = k;

            /* add this into the existing image */
            list_dx.put(k.lastKey(), k);
            synchronized(gens_lock) {
                gens.put(gdeg, gen);
            }
        }

        /* okbasis is done, free it. Note that okbasis is usually a restricted
         * view of the global kbasis object, and this call removes entries from that. */
        okbasis.clear(); 

        /* save the result -- at this point the computation is considered finished */
        status.put(deg, STATE_DONE);
        ping(deg);

        //if(Config.STDOUT) System.out.printf("%s: %3d gen, %3d ker\n\n", Arrays.toString(deg), dat.gens.size(), dat.kbasis.size());

        if(Config.TIMING && deg[0] == deg[1]) {
            long elapsed = System.currentTimeMillis() - start;
            double log = Math.log(elapsed);
            double score = log / deg[0]; 
            Runtime run = Runtime.getRuntime();
            System.out.printf("t=%d elapsed=%dms log/t=%f mem=%dM/%dM\n",
                deg[0], elapsed, score, (run.maxMemory() - run.freeMemory())>>20, run.maxMemory()>>20);
        }

        /* kick off child tasks (+1 to each algebraic degree) */
        for(int g = 1; g < ngrad; g++) {
            nextdeg = Arrays.copyOf(deg,ngrad);
            nextdeg[g]++;
            tryQueueIfReady(nextdeg);
        }
    }
    

    /* backend implementation */

    public Decorated<Generator<T>, MultigradedAlgebraComputation<Generator<T>>> getDecorated()
    {
        CompoundDecorated<Generator<T>,MultigradedAlgebraComputation<Generator<T>>> dec = new CompoundDecorated<Generator<T>,MultigradedAlgebraComputation<Generator<T>>>(this);

//        Collection<DifferentialRule> diffrules = new ArrayList<DifferentialRule>();
//        diffrules.add(new DifferentialRule(new int[] {2,1,1}, new int[] {1,1,0}, Color.green));
//        diffrules.add(new DifferentialRule(new int[] {1,0,2}, new int[] {0,0,1}, Color.red));
//        dec.add(new DifferentialDecorated<Generator<T>,MultigradedAlgebraComputation<Generator<T>>>(this, diffrules));

        /* // RGB
        Color[] colors = new Color[] {
            new Color(128,0,0),
            new Color(0,128,0),
            new Color(0,0,128)
        }; */
        /* // fading
        Color[] colors = new Color[] {
            new Color(0,0,0),
            new Color(0,0,0),
            new Color(96,96,96),
            new Color(192,192,192)
        };*/
        // black
        Color[] colors = new Color[] {
            new Color(0,0,0),
            new Color(0,0,0),
            new Color(0,0,0),
        };
        List<T> distinguished = alg.distinguished();
        Collection<ProductRule> prodrules = new ArrayList<ProductRule>();
        for(int i = 0; i < colors.length && i < distinguished.size(); i++)
            prodrules.add(new ProductRule("h_"+i, distinguished.get(i), true, false, false, colors[i]));
        dec.add(new ProductDecorated<T,MultigradedAlgebraComputation<Generator<T>>>(this, prodrules));

        return dec;
    }
}

class BrunerThread extends Thread
{
    BrunerBackend<?,?> back;
    int id;
    volatile static int ids;
    BrunerThread(BrunerBackend<?,?> back) {
        setPriority(Thread.MIN_PRIORITY);
        this.back = back;
        id = ids++;
    }

    @Override public void run()
    {
        while(true) {
            if(Config.DEBUG_THREADS) System.out.println(id + ": Waiting for task...");
            int[] task;
            try {
                task = back.tasks.take();
            } catch(InterruptedException e) {
                continue;
            }
            if(Config.DEBUG_THREADS) System.out.println(id + ": got task ...");

            if(task[0] > Config.T_CAP)
                continue;
            back.compute(task);
        }
    }
}

