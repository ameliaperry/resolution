package res.transform;

import java.awt.Color;
import java.util.*;

public class DifferentialRule
{
    final int[] initial;
    final int[] step;
    final Color color;

    public DifferentialRule(int[] initial, int[] step, Color color)
    {
        this.initial = initial;
        this.step = step;
        this.color = color;
    }

    public static Collection<DifferentialRule> parse(String s) 
    {
        /* TODO parse differential rules */
        return null;
    }
}

