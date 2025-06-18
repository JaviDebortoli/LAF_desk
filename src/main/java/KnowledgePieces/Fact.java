package KnowledgePieces;

import java.util.Arrays;

public class Fact extends KnowledgePiece{
    
    private final String name;
    private final String argument;
    private Double[] deltaAttributes;
    
    public Fact(String name, String argument, Double[] attributes) {
        this.name = name;
        this.argument = argument;
        this.attributes = attributes;
        this.deltaAttributes = attributes;
    }

    public String getName() {
        return name;
    }

    public String getArgument() {
        return argument;
    }
    
    public void setAttributes(Double[] attributes) { 
        this.attributes = attributes;
        this.deltaAttributes = attributes; 
    }
    
    public void setDeltaAttributes(Double[] attributes) { 
        this.deltaAttributes = attributes; 
    }
    
    @Override
    public String toString() {
        return name + '(' 
                + argument + "). " 
                + Arrays.toString(attributes) 
                +" "
                + Arrays.toString(deltaAttributes);
    }
    
}