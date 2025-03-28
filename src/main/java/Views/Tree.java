package Views;

import KnowledgePieces.*;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import javax.swing.JFrame;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Tree extends JFrame {
    private final mxGraph graph;
    private final Object parent;
    private final Map<KnowledgePiece, Object> vertexMap;
    private static final int PADDING = 20; // Padding interno para los nodos
    private static final Font NODE_FONT = new Font("Arial", Font.PLAIN, 12);

    // Modificar el constructor para aceptar la nueva estructura de datos
    public Tree(Map<KnowledgePiece, Set<Fact>> edges) {
        super("Árbol de Inferencias");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setTitle("LAF");
        
        graph = new mxGraph();
        parent = graph.getDefaultParent();
        vertexMap = new HashMap<>();
        
        // Configurar el estilo predeterminado del grafo
        graph.setCellsResizable(false);
        graph.setAutoSizeCells(true);
        
        graph.getModel().beginUpdate();
        try {
            createGraph(edges);
            
            mxHierarchicalLayout layout = new mxHierarchicalLayout(graph);
            layout.setInterRankCellSpacing(50);
            layout.setInterHierarchySpacing(50);
            layout.execute(parent);
        } finally {
            graph.getModel().endUpdate();
        }

        mxGraphComponent graphComponent = new mxGraphComponent(graph);
        getContentPane().add(graphComponent);
        
        setSize(800, 600);
        setLocationRelativeTo(null);
    }

    // Modificar el método createGraph para trabajar con la nueva estructura
    private void createGraph(Map<KnowledgePiece, Set<Fact>> edges) {
        // Crear primero todos los vértices
        for (Map.Entry<KnowledgePiece, Set<Fact>> entry : edges.entrySet()) {
            createVertex(entry.getKey());
            for (Fact fact : entry.getValue()) {
                createVertex(fact);
            }
        }

        // Luego crear todas las aristas
        for (Map.Entry<KnowledgePiece, Set<Fact>> entry : edges.entrySet()) {
            Object source = vertexMap.get(entry.getKey());
            for (Fact fact : entry.getValue()) {
                Object target = vertexMap.get(fact);
                graph.insertEdge(parent, null, "", source, target, "edgeStyle=orthogonalEdgeStyle");
            }
        }
    }

    private void createVertex(KnowledgePiece piece) {
        if (!vertexMap.containsKey(piece)) {
            String label = piece.toString();
            Dimension textSize = getTextDimension(label);
            
            String style = piece instanceof Rule ? 
                "shape=rectangle;fillColor=#FFD700;fontSize=12" : 
                "shape=ellipse;fillColor=#98FB98;fontSize=12";
            
            Object vertex = graph.insertVertex(parent, null, label, 
                0, 0, textSize.width + PADDING, textSize.height + PADDING, style);
            vertexMap.put(piece, vertex);
        }
    }

    private static class Dimension {
        int width;
        int height;
        
        Dimension(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    private Dimension getTextDimension(String text) {
        // Crear un objeto Graphics temporal para medir el texto
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        g.setFont(NODE_FONT);
        FontMetrics fm = g.getFontMetrics();
        
        // Medir el texto
        int width = fm.stringWidth(text);
        int height = fm.getHeight();
        
        g.dispose();
        
        return new Dimension(width, height);
    }

    // Modificar el método estático para aceptar la nueva estructura
    public static void visualizeInferenceTree(Map<KnowledgePiece, Set<Fact>> edges) {
        Tree visualizer = new Tree(edges);
        visualizer.setVisible(true);
    }
}