package res.transform;

import java.awt.Color;

public class UnbasedLineDecoration<T> {
    public T src;
    public int[] dest;
    public Color color;

    public UnbasedLineDecoration(T src, int[] dest, Color color) {
        this.src = src;
        this.dest = dest;
        this.color = color;
    }
}
