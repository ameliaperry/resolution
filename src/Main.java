
class Main {
    
    static void die_if(boolean test, String fail)
    {
        if(test) {
            System.err.println(fail);
            Thread.dumpStack();
            System.err.println("Failing.");
            System.exit(1);
        }
    }

    static void init()
    {
        ResMath.calcInverses();
    }

    public static void main(String[] args)
    {
        init();

        ResBackend back = new ResParallelizedBackend();
        ResDisplay.constructFrontend(back);
        
        back.start();
    }

}
