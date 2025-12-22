package KnowledgePieces;

public abstract class KnowledgePiece {

    Double[] attributes;
    
    @Override
    public abstract String toString();
    public Double[] getAttributes() { return attributes; }
    
}