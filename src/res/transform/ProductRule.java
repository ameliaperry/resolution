package res.transform;

import java.awt.Color;

public class ProductRule // "should" be ProductRule<U extends GradedElement<U>>
{
    String name;
    Object trigger; // "should" be type U
    boolean lines;
    boolean hide;
    boolean towers;
    Color color;

    public ProductRule(String name, Object trigger, boolean lines, boolean hide, boolean towers, Color color) {
        this.name = name;
        this.trigger = trigger;
        this.lines = lines;
        this.hide = hide;
        this.towers = towers;
        this.color = color;
    }
}

