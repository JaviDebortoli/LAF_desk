package KnowledgePieces;

import java.text.DecimalFormat;
import java.util.Arrays;

public class Fact extends KnowledgePiece{
    
    private final String name;
    private final String argument;

    public Fact(String name, String argument, Double[] attributes) {
        this.name = name;
        this.argument = argument;
        this.attributes = attributes;
    }

    public String getName() {
        return name;
    }

    public String getArgument() {
        return argument;
    }
    
    public void setAttributes(Double[] attributes) { 
        this.attributes = attributes; 
    }
    
    @Override
    public String toString() {
        return name + '(' + argument + "). " + Arrays.toString(attributes);
    }
    
}