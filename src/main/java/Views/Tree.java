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
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class Tree extends JFrame {
    private final mxGraph graph;
    private final Object parent;
    private final Map<KnowledgePiece, Object> vertexMap;
    private final Map<String, Object> dmpNodeMap; // Para mapear nodos dMP únicos
    private int dmpCounter = 0; // Contador para crear IDs únicos de nodos dMP
    
    private static final int PADDING = 4;
    private static final Font NODE_FONT = new Font("Arial", Font.PLAIN, 9);
    private static final Font HEADER_FONT = new Font("Arial", Font.BOLD, 9);
    private static final int CELL_WIDTH = 35;
    private static final int CELL_HEIGHT = 18;
    private static final int HEADER_HEIGHT = 30;
    private static final int FIXED_NODE_HEIGHT = 55;

    public Tree(Map<KnowledgePiece, List<Fact>> edges) {
        super("Árbol de Inferencias");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setTitle("LAF");
        
        graph = new mxGraph();
        parent = graph.getDefaultParent();
        vertexMap = new HashMap<>();
        dmpNodeMap = new HashMap<>();
        
        // Configurar el estilo predeterminado del grafo
        graph.setCellsResizable(false);
        graph.setAutoSizeCells(false);
        graph.setHtmlLabels(true);
        
        graph.getModel().beginUpdate();
        try {
            createGraph(edges);
            
            mxHierarchicalLayout layout = new mxHierarchicalLayout(graph);
            layout.setInterRankCellSpacing(70);
            layout.setInterHierarchySpacing(200);
            layout.setIntraCellSpacing(80);
            layout.execute(parent);
        } finally {
            graph.getModel().endUpdate();
        }

        mxGraphComponent graphComponent = new mxGraphComponent(graph);
        getContentPane().add(graphComponent);
        
        setSize(800, 600);
        setLocationRelativeTo(null);
    }

    private void createGraph(Map<KnowledgePiece, List<Fact>> edges) {
        // Crear primero todos los vértices de hechos y reglas
        for (Map.Entry<KnowledgePiece, List<Fact>> entry : edges.entrySet()) {
            createVertex(entry.getKey());
            for (Fact fact : entry.getValue()) {
                createVertex(fact);
            }
        }

        // Identificar qué hechos son inferidos por reglas para evitar conexiones directas
        Set<Fact> factsInferredByRules = new HashSet<>();
        for (Map.Entry<KnowledgePiece, List<Fact>> entry : edges.entrySet()) {
            if (entry.getKey() instanceof Rule) {
                factsInferredByRules.addAll(entry.getValue());
            }
        }

        // Crear las conexiones con nodos dMP para inferencias por reglas
        for (Map.Entry<KnowledgePiece, List<Fact>> entry : edges.entrySet()) {
            KnowledgePiece source = entry.getKey();
            List<Fact> targets = entry.getValue();
            
            if (source instanceof Rule && !targets.isEmpty()) {
                // Es una regla que infiere hechos - usar nodo dMP
                createRuleInferenceWithDMP((Rule) source, targets, edges);
            } else {
                // Conexión directa solo para casos que NO sean hechos inferidos por reglas
                Object sourceVertex = vertexMap.get(source);
                for (Fact target : targets) {
                    // Solo crear conexión directa si el target NO es inferido por una regla
                    if (!factsInferredByRules.contains(target)) {
                        Object targetVertex = vertexMap.get(target);
                        graph.insertEdge(parent, null, "", sourceVertex, targetVertex, 
                            "edgeStyle=orthogonalEdgeStyle;strokeWidth=2;strokeColor=#333333");
                    }
                }
            }
        }
    }

    private void createRuleInferenceWithDMP(Rule rule, List<Fact> inferredFacts, 
                                          Map<KnowledgePiece, List<Fact>> allEdges) {
        for (Fact inferredFact : inferredFacts) {
            // Crear un nodo dMP único para esta inferencia
            String dmpId = "dMP_" + (++dmpCounter);
            Object dmpNode = createDMPNode(dmpId);
            
            // Conectar la regla al nodo dMP
            Object ruleVertex = vertexMap.get(rule);
            graph.insertEdge(parent, null, "", ruleVertex, dmpNode, 
                "edgeStyle=orthogonalEdgeStyle;strokeWidth=2;strokeColor=#333333");
            
            // Encontrar los hechos que activan esta regla y conectarlos al nodo dMP
            // MODIFICACIÓN: Solo conectar hechos agregados (no duplicados)
            List<Fact> activatingFacts = findActivatingFactsAggregated(rule, allEdges);
            for (Fact activatingFact : activatingFacts) {
                Object factVertex = vertexMap.get(activatingFact);
                if (factVertex != null) {
                    graph.insertEdge(parent, null, "", factVertex, dmpNode, 
                        "edgeStyle=orthogonalEdgeStyle;strokeWidth=2;strokeColor=#333333");
                }
            }
            
            // Conectar el nodo dMP al hecho inferido
            Object inferredFactVertex = vertexMap.get(inferredFact);
            graph.insertEdge(parent, null, "", dmpNode, inferredFactVertex, 
                "edgeStyle=orthogonalEdgeStyle;strokeWidth=2;strokeColor=#333333");
        }
    }

    private Object createDMPNode(String dmpId) {
        // Crear un nodo circular para dMP
        String style = "shape=ellipse;fillColor=#FFFFFF;strokeColor=#333333;strokeWidth=2;" +
                      "fontSize=10;fontStyle=1;verticalAlign=middle;align=center";
        
        Object dmpNode = graph.insertVertex(parent, null, "dMP", 
            0, 0, 60, 40, style);
        
        dmpNodeMap.put(dmpId, dmpNode);
        return dmpNode;
    }

    /**
     * MÉTODO MODIFICADO: Encuentra los hechos que activan la regla, 
     * pero solo retorna el hecho "agregado" cuando hay múltiples hechos 
     * con el mismo nombre y argumento.
     */
    private List<Fact> findActivatingFactsAggregated(Rule rule, Map<KnowledgePiece, List<Fact>> allEdges) {
        List<Fact> activatingFacts = new ArrayList<>();
        List<String> ruleBody = rule.getBody();
        
        // Mapa para agrupar hechos por nombre y argumento
        Map<String, List<Fact>> factGroups = new HashMap<>();
        
        // Buscar hechos que coincidan con los predicados del cuerpo de la regla
        // y construir el mapa de dependencias para identificar cuáles son agregados
        for (Map.Entry<KnowledgePiece, List<Fact>> entry : allEdges.entrySet()) {
            KnowledgePiece piece = entry.getKey();
            if (piece instanceof Fact) {
                Fact fact = (Fact) piece;
                // Verificar si este hecho puede activar la regla
                if (ruleBody.contains(fact.getName())) {
                    String key = fact.getName() + "(" + fact.getArgument() + ")";
                    factGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(fact);
                }
            }
        }
        
        // Para cada grupo de hechos con el mismo nombre y argumento,
        // seleccionar solo el hecho "agregado" (el que está más abajo en el árbol)
        for (List<Fact> group : factGroups.values()) {
            if (group.size() == 1) {
                // Si solo hay un hecho, agregarlo directamente
                activatingFacts.add(group.get(0));
            } else {
                // Si hay múltiples hechos, seleccionar el agregado basándose en las dependencias
                Fact aggregatedFact = findBottomMostFact(group, allEdges);
                activatingFacts.add(aggregatedFact);
            }
        }
        
        return activatingFacts;
    }
    
    /**
     * Encuentra el hecho que está más abajo en el árbol de inferencias.
     * El hecho agregado es aquel que tiene más dependencias (más hechos que lo generan).
     */
    private Fact findBottomMostFact(List<Fact> facts, Map<KnowledgePiece, List<Fact>> allEdges) {
        if (facts.size() == 1) {
            return facts.get(0);
        }
        
        // Calcular la profundidad/dependencias de cada hecho
        Map<Fact, Integer> depthMap = new HashMap<>();
        
        for (Fact fact : facts) {
            int depth = calculateDepth(fact, allEdges, new HashSet<>());
            depthMap.put(fact, depth);
        }
        
        // Encontrar el hecho con mayor profundidad (más abajo en el árbol)
        Fact bottomMostFact = facts.get(0);
        int maxDepth = depthMap.get(bottomMostFact);
        
        for (Fact fact : facts) {
            int currentDepth = depthMap.get(fact);
            if (currentDepth > maxDepth) {
                maxDepth = currentDepth;
                bottomMostFact = fact;
            }
        }
        
        return bottomMostFact;
    }
    
    /**
     * Calcula la profundidad de un hecho en el árbol de inferencias.
     * Un hecho agregado tendrá mayor profundidad porque depende de otros hechos.
     */
    private int calculateDepth(Fact targetFact, Map<KnowledgePiece, List<Fact>> allEdges, Set<Fact> visited) {
        if (visited.contains(targetFact)) {
            return 0; // Evitar ciclos
        }
        
        visited.add(targetFact);
        int maxDepth = 0;
        
        // Buscar todos los hechos y reglas que generan este hecho
        for (Map.Entry<KnowledgePiece, List<Fact>> entry : allEdges.entrySet()) {
            List<Fact> inferredFacts = entry.getValue();
            
            if (inferredFacts.contains(targetFact)) {
                KnowledgePiece source = entry.getKey();
                
                if (source instanceof Rule) {
                    // Si es generado por una regla, buscar la profundidad de los hechos que activan la regla
                    Rule rule = (Rule) source;
                    List<Fact> activatingFacts = findDirectActivatingFacts(rule, allEdges);
                    
                    for (Fact activatingFact : activatingFacts) {
                        int depth = calculateDepth(activatingFact, allEdges, new HashSet<>(visited)) + 1;
                        maxDepth = Math.max(maxDepth, depth);
                    }
                } else if (source instanceof Fact) {
                    // Si es generado por otro hecho, calcular su profundidad
                    int depth = calculateDepth((Fact) source, allEdges, new HashSet<>(visited)) + 1;
                    maxDepth = Math.max(maxDepth, depth);
                }
            }
        }
        
        visited.remove(targetFact);
        return maxDepth;
    }
    
    /**
     * Encuentra los hechos que activan una regla sin filtrar por agregación.
     * Método auxiliar para el cálculo de profundidad.
     */
    private List<Fact> findDirectActivatingFacts(Rule rule, Map<KnowledgePiece, List<Fact>> allEdges) {
        List<Fact> activatingFacts = new ArrayList<>();
        List<String> ruleBody = rule.getBody();
        
        for (Map.Entry<KnowledgePiece, List<Fact>> entry : allEdges.entrySet()) {
            KnowledgePiece piece = entry.getKey();
            if (piece instanceof Fact) {
                Fact fact = (Fact) piece;
                if (ruleBody.contains(fact.getName())) {
                    activatingFacts.add(fact);
                }
            }
        }
        
        return activatingFacts;
    }

    private void createVertex(KnowledgePiece piece) {
        if (!vertexMap.containsKey(piece)) {
            String htmlLabel = createNodeLabel(piece);
            Dimension nodeSize = calculateNodeSize(piece);
            
            // Nodo transparente sin borde para que solo se vea la tabla HTML
            String style = "shape=rectangle;fillColor=none;strokeColor=none;fontSize=10;verticalAlign=top";
            
            Object vertex = graph.insertVertex(parent, null, htmlLabel, 
                0, 0, nodeSize.width, nodeSize.height, style);
            vertexMap.put(piece, vertex);
        }
    }

    private String createNodeLabel(KnowledgePiece piece) {
        StringBuilder html = new StringBuilder();
        html.append("<table border='1' cellpadding='2' cellspacing='0' style='border-collapse:collapse;background-color:#FFFFFF;border:2px solid #333333;'>");
        
        // Encabezado con el nombre del nodo
        String nodeName = getNodeName(piece);
        Double[] attributes = piece.getAttributes();
        int numAttributes = (attributes != null) ? attributes.length : 1;
        
        html.append("<tr><td colspan='").append(numAttributes)
            .append("' style='background-color:#FFFFFF;font-weight:bold;text-align:center;padding:3px;font-size:9px;white-space:nowrap;border-bottom:1px solid #333333;'>")
            .append(nodeName)
            .append("</td></tr>");
        
        // Fila con los valores de los atributos
        html.append("<tr>");
        if (attributes != null && attributes.length > 0) {
            for (int i = 0; i < attributes.length; i++) {
                Double attr = attributes[i];
                String borderStyle = (i < attributes.length - 1) ? "border-right:1px solid #333333;" : "";
                html.append("<td style='text-align:center;padding:2px;width:").append(CELL_WIDTH)
                    .append("px;height:").append(CELL_HEIGHT)
                    .append("px;font-size:8px;background-color:#FFFFFF;").append(borderStyle).append("'>")
                    .append(formatAttribute(attr))
                    .append("</td>");
            }
        } else {
            // Si no hay atributos, mostrar una celda vacía
            html.append("<td style='text-align:center;padding:2px;width:").append(CELL_WIDTH)
                .append("px;height:").append(CELL_HEIGHT)
                .append("px;font-size:8px;background-color:#FFFFFF;'>-</td>");
        }
        html.append("</tr>");
        
        html.append("</table>");
        return html.toString();
    }

    private String getNodeName(KnowledgePiece piece) {
        if (piece instanceof Rule) {
            Rule rule = (Rule) piece;
            // Construir el nombre completo de la regla
            StringBuilder ruleName = new StringBuilder();
            ruleName.append(rule.getHead()).append("(X) :- ");
            
            List<String> body = rule.getBody();
            for (int i = 0; i < body.size(); i++) {
                ruleName.append(body.get(i)).append("(X)");
                if (i < body.size() - 1) {
                    ruleName.append(", ");
                }
            }
            return ruleName.toString();
        } else if (piece instanceof Fact) {
            Fact fact = (Fact) piece;
            return fact.getName() + "(" + fact.getArgument() + ")";
        } else {
            // Fallback para otros tipos
            String fullName = piece.toString();
            if (fullName.contains("]. ")) {
                return fullName.substring(0, fullName.indexOf("]. "));
            } else if (fullName.contains("). ")) {
                return fullName.substring(0, fullName.indexOf("). ") + 1);
            }
            return fullName;
        }
    }

    private Dimension calculateNodeSize(KnowledgePiece piece) {
        String nodeName = getNodeName(piece);
        Double[] attributes = piece.getAttributes();
        int numAttributes = (attributes != null) ? attributes.length : 1;
        
        // Calcular ancho basado en el texto del nombre del nodo
        int minWidthForAttributes = numAttributes * CELL_WIDTH + (numAttributes + 1) * 2; // +2 por bordes
        
        // Usar medición real del texto para el ancho del nombre
        Dimension textDim = getTextDimension(nodeName);
        int minWidthForName = textDim.width + 20; // +20 para padding
        
        // El ancho final es el mayor entre el necesario para el nombre y para los atributos
        int width = Math.max(minWidthForAttributes, minWidthForName);
        
        // Altura fija para todos los nodos
        int height = HEADER_HEIGHT + CELL_HEIGHT + 10; // Header + fila de atributos + padding
        
        return new Dimension(width, height);
    }

    private String formatAttribute(Double value) {
        if (value == null) {
            return "-";
        }
        
        // Formatear el número dependiendo de su valor
        if (value == 0.0) {
            return "0";
        } else if (value == 1.0) {
            return "1";
        } else if (Math.abs(value) < 0.01) {
            return String.format("%.3f", value);
        } else if (Math.abs(value) < 0.1) {
            return String.format("%.2f", value);
        } else if (Math.abs(value) < 1) {
            return String.format("%.1f", value);
        } else {
            // Para números enteros o mayores a 1
            if (value % 1 == 0) {
                return String.format("%.0f", value);
            } else {
                return String.format("%.1f", value);
            }
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
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        g.setFont(HEADER_FONT); // Usar la fuente del header para medir
        FontMetrics fm = g.getFontMetrics();
        
        int width = fm.stringWidth(text);
        int height = fm.getHeight();
        
        g.dispose();
        
        return new Dimension(width, height);
    }

    public static void visualizeInferenceTree(Map<KnowledgePiece, List<Fact>> edges) {
        Tree visualizer = new Tree(edges);
        visualizer.setVisible(true);
    }
}