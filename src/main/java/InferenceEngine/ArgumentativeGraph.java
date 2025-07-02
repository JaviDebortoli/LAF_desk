package InferenceEngine;

import KnowledgePieces.Fact;
import KnowledgePieces.KnowledgePiece;
import java.util.List;
import java.util.Map;

public record ArgumentativeGraph (Map<KnowledgePiece, List<Fact>> edges, List<Pair> conflictiveNodes) {}
