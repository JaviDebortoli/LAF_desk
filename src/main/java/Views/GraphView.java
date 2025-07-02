package Views;

import InferenceEngine.ArgumentativeGraph;
import InferenceEngine.Pair;
import KnowledgePieces.*;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.ext.JGraphXAdapter;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.util.mxConstants;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;


/**
 * GraphView class para visualizar el grafo argumentativo usando JGraphT y JGraphX
 * Representa I-nodos con sus atributos en formato de tabla
 */
public class GraphView extends JFrame {
    
    private final Map<KnowledgePiece, List<Fact>> edgeStructure;
    private final List<Pair> conflictiveNodes;
    private Graph<GraphNode, DefaultEdge> graph;
    private JGraphXAdapter<GraphNode, DefaultEdge> graphAdapter;
    private mxGraphComponent graphComponent;
    
    /**
     * Clase interna para representar nodos del grafo con información adicional
     */
    public static class GraphNode {
        private final KnowledgePiece knowledgePiece;
        private final String displayName;
        private final Double[] attributes;
        private final Double[] deltaAttributes; // Nuevo campo para deltaAttributes
        private final boolean isCANode; // Indica si es un nodo CA
        
        public GraphNode(KnowledgePiece knowledgePiece) {
            this.knowledgePiece = knowledgePiece;
            this.attributes = knowledgePiece.getAttributes();
            this.isCANode = false;
            
            // Obtener deltaAttributes si es un Fact
            if (knowledgePiece instanceof Fact fact) {
                this.deltaAttributes = fact.getDeltaAttributes();
            } else {
                this.deltaAttributes = null;
            }
            
            switch (knowledgePiece) {
                case Fact fact -> this.displayName = fact.getName() + "(" + fact.getArgument() + ")";
                case Rule rule -> this.displayName = rule.getHead() + "(X) :- " + rule.getBody() + "(X)";
                default -> this.displayName = knowledgePiece.toString();
            }
        }
        
        // Constructor para nodos CA
        public GraphNode(String caNodeName) {
            this.knowledgePiece = null;
            this.attributes = null;
            this.deltaAttributes = null;
            this.displayName = caNodeName;
            this.isCANode = true;
        }
        
        public KnowledgePiece getKnowledgePiece() {
            return knowledgePiece;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public Double[] getAttributes() {
            return attributes;
        }
        
        public Double[] getDeltaAttributes() {
            return deltaAttributes;
        }
        
        public boolean isCANode() {
            return isCANode;
        }
        
        /**
         * Genera representación de texto en formato tabla para el nodo
         * @return 
         */
        public String getTextRepresentation() {
            if (isCANode) {
                return displayName;
            }
            
            StringBuilder text = new StringBuilder();
            text.append(displayName).append("\n");
            
            if (attributes != null && attributes.length > 0) {
                text.append("─".repeat(Math.max(20, displayName.length()))).append("\n");
                
                // Crear tabla con columnas
                if (knowledgePiece instanceof Fact && deltaAttributes != null) {
                    int maxLength = Math.max(attributes.length, deltaAttributes.length);
                    for (int i = 0; i < maxLength; i++) {
                        String attrValue = i < attributes.length ? 
                            String.format("%4.1f", attributes[i]) : "    ";
                        String deltaValue = i < deltaAttributes.length ? 
                            String.format("%5.1f", deltaAttributes[i]) : "     ";
                        
                        text.append(attrValue).append("  | ").append(deltaValue).append("\n");
                    }
                } else {
                    for (Double attribute : attributes) {
                        String attrValue = String.format("%4.1f", attribute);
                        text.append(attrValue).append("  | ").append(attrValue).append("\n");
                    }
                }
            } 
            return text.toString();
        }
        
        @Override
        public String toString() {
            return displayName;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            GraphNode graphNode = (GraphNode) obj;
            return Objects.equals(displayName, graphNode.displayName) && 
                   Objects.equals(knowledgePiece, graphNode.knowledgePiece) &&
                   isCANode == graphNode.isCANode;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(knowledgePiece, displayName, isCANode);
        }
    }
    
    /**
     * Constructor principal
     * 
     * @param graph Estructura que contiene todas las aristas y
     * nodos en conflicto de un grafo argumentativo
     */
    public GraphView(ArgumentativeGraph graph) {
        this.edgeStructure = graph.edges();
        this.conflictiveNodes = graph.conflictiveNodes();
        initializeGraph();
        setupUI();
    }
    
    /**
     * Inicializa el grafo a partir de la estructura de aristas
     */
    private void initializeGraph() {
        graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        
        // Crear mapa de KnowledgePiece a GraphNode para evitar duplicados
        Map<KnowledgePiece, GraphNode> nodeMap = new HashMap<>();
        
        // Agregar todos los nodos
        for (Map.Entry<KnowledgePiece, List<Fact>> entry : edgeStructure.entrySet()) {
            KnowledgePiece source = entry.getKey();
            List<Fact> targets = entry.getValue();
            
            // Agregar nodo fuente
            GraphNode sourceNode = nodeMap.computeIfAbsent(source, GraphNode::new);
            graph.addVertex(sourceNode);
            
            // Agregar nodos destino y aristas
            for (Fact target : targets) {
                GraphNode targetNode = nodeMap.computeIfAbsent(target, GraphNode::new);
                graph.addVertex(targetNode);
                graph.addEdge(sourceNode, targetNode);
            }
        }
        
        // Crear el adaptador JGraphX
        graphAdapter = new JGraphXAdapter<>(graph);
    }
    
    /**
     * Configura la interfaz de usuario
     */
    private void setupUI() {
        setTitle("Graph View - Argumentative Framework");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Configurar el componente del grafo
        setupGraphComponent();
        
        // Panel de controles
        JPanel controlPanel = createControlPanel();
        
        add(controlPanel, BorderLayout.NORTH);
        add(graphComponent, BorderLayout.CENTER);
        
        // Aplicar layout inicial
        applyHierarchicalLayout();
        
        // Agregar nodos CA después del layout inicial
        addCANodes();
        
        // Configurar ventana
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }
    
    /**
     * Agrega nodos CA entre los pares conflictivos sin alterar el layout existente
     */
    private void addCANodes() {
        mxGraph mxGraph = graphAdapter;
        mxGraph.getModel().beginUpdate();
        
        try {
            // Crear mapa de Facts a GraphNodes para búsqueda rápida
            Map<Fact, GraphNode> factToNodeMap = new HashMap<>();
            for (GraphNode node : graph.vertexSet()) {
                if (node.getKnowledgePiece() instanceof Fact fact) {
                    factToNodeMap.put(fact, node);
                }
            }
            
            int caCounter = 1;
            for (Pair conflictivePair : conflictiveNodes) {
                GraphNode firstNode = factToNodeMap.get(conflictivePair.first());
                GraphNode secondNode = factToNodeMap.get(conflictivePair.second());
                
                if (firstNode != null && secondNode != null) {
                    // Crear nodo CA
                    GraphNode caNode = new GraphNode("CA");
                    
                    // Obtener las celdas correspondientes a los nodos conflictivos
                    mxCell firstCell = (mxCell) graphAdapter.getVertexToCellMap().get(firstNode);
                    mxCell secondCell = (mxCell) graphAdapter.getVertexToCellMap().get(secondNode);
                    
                    if (firstCell != null && secondCell != null) {
                        // Calcular posición intermedia entre los dos nodos
                        double x1 = firstCell.getGeometry().getCenterX();
                        double y1 = firstCell.getGeometry().getCenterY();
                        double x2 = secondCell.getGeometry().getCenterX();
                        double y2 = secondCell.getGeometry().getCenterY();
                        
                        double caX = (x1 + x2) / 2;
                        double caY = (y1 + y2) / 2;
                        
                        // Crear celda para el nodo CA
                        mxCell caCell = (mxCell) mxGraph.insertVertex(
                            mxGraph.getDefaultParent(),
                            null,
                            caNode.getTextRepresentation(),
                            caX - 25, // Centrar el nodo (ancho/2)
                            caY - 25, // Centrar el nodo (alto/2)
                            50,       // Ancho del nodo CA
                            50        // Alto del nodo CA
                        );
                        
                        // Aplicar estilo específico para nodos CA
                        caCell.setStyle("CA_NODE");
                        
                        // Agregar el nodo al grafo interno para mantener consistencia
                        graph.addVertex(caNode);
                        graphAdapter.getVertexToCellMap().put(caNode, caCell);
                        graphAdapter.getCellToVertexMap().put(caCell, caNode);
                        
                        // Crear aristas desde los nodos conflictivos hacia el nodo CA
                        mxCell edge1 = (mxCell) mxGraph.insertEdge(
                            mxGraph.getDefaultParent(),
                            null,
                            "",
                            firstCell,
                            caCell
                        );
                        edge1.setStyle("CA_EDGE");
                        
                        mxCell edge2 = (mxCell) mxGraph.insertEdge(
                            mxGraph.getDefaultParent(),
                            null,
                            "",
                            secondCell,
                            caCell
                        );
                        edge2.setStyle("CA_EDGE");
                    }
                    
                    caCounter++;
                }
            }
        } finally {
            mxGraph.getModel().endUpdate();
        }
        
        graphComponent.refresh();
    }
    
    /**
     * Configura el componente gráfico
     */
    private void setupGraphComponent() {
        graphComponent = new mxGraphComponent(graphAdapter);
        graphComponent.setConnectable(false);
        graphComponent.getGraph().setAllowDanglingEdges(false);
        graphComponent.getGraph().setEdgeLabelsMovable(false);
        graphComponent.getGraph().setVertexLabelsMovable(false);
        graphComponent.getGraph().setCellsEditable(false);
        graphComponent.setDragEnabled(false);
        graphComponent.getGraph().setAllowDanglingEdges(false);
        graphComponent.getGraph().setCellsMovable(false);
        graphComponent.getGraph().setCellsResizable(false);
        graphComponent.getGraph().setCellsSelectable(true); // Permitir selección para visualización
        graphComponent.getGraph().setCellsBendable(false);
        graphComponent.getGraph().setCellsCloneable(false);
        graphComponent.getGraph().setCellsDisconnectable(false);
        graphComponent.getGraph().setDropEnabled(false);
        graphComponent.getGraph().setSplitEnabled(false);
        
        // Configurar estilos
        configureStyles();
        
        // Configurar celdas con representación de texto
        configureCellRenderer();
    }
    
    /**
     * Configura los estilos del grafo
     */
    private void configureStyles() {
        mxGraph mxGraph = graphAdapter;
        
        // Estilo para nodos - rectangular con bordes
        Map<String, Object> nodeStyle = new HashMap<>();
        nodeStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
        nodeStyle.put(mxConstants.STYLE_FILLCOLOR, "#ffffff");
        nodeStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");
        nodeStyle.put(mxConstants.STYLE_STROKEWIDTH, 1);
        nodeStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        nodeStyle.put(mxConstants.STYLE_FONTSIZE, 10);
        nodeStyle.put(mxConstants.STYLE_FONTSTYLE, 0);
        nodeStyle.put(mxConstants.STYLE_FONTFAMILY, "Courier New");
        nodeStyle.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_MIDDLE);
        nodeStyle.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_CENTER);
        nodeStyle.put(mxConstants.STYLE_WHITE_SPACE, "wrap");
        mxGraph.getStylesheet().putCellStyle("NODE", nodeStyle);
        
        // Estilo para nodos CA - diamante
        Map<String, Object> caNodeStyle = new HashMap<>();
        caNodeStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RHOMBUS);
        caNodeStyle.put(mxConstants.STYLE_FILLCOLOR, "#ffffff");
        caNodeStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");
        caNodeStyle.put(mxConstants.STYLE_STROKEWIDTH, 2);
        caNodeStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        caNodeStyle.put(mxConstants.STYLE_FONTSIZE, 12);
        caNodeStyle.put(mxConstants.STYLE_FONTSTYLE, 1); // Bold
        caNodeStyle.put(mxConstants.STYLE_FONTFAMILY, "Arial");
        caNodeStyle.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_MIDDLE);
        caNodeStyle.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_CENTER);
        mxGraph.getStylesheet().putCellStyle("CA_NODE", caNodeStyle);
        
        // Estilo para aristas
        Map<String, Object> edgeStyle = new HashMap<>();
        edgeStyle.put(mxConstants.STYLE_EDGE, mxConstants.EDGESTYLE_ORTHOGONAL);
        edgeStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");
        edgeStyle.put(mxConstants.STYLE_STROKEWIDTH, 1);
        edgeStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC);
        edgeStyle.put(mxConstants.STYLE_ENDSIZE, 8);
        edgeStyle.put(mxConstants.STYLE_NOLABEL, true); // Sin etiquetas en aristas
        mxGraph.getStylesheet().putCellStyle("EDGE", edgeStyle);
        
        // Estilo para aristas CA
        Map<String, Object> caEdgeStyle = new HashMap<>();
        caEdgeStyle.put(mxConstants.STYLE_EDGE, mxConstants.EDGESTYLE_ORTHOGONAL);
        caEdgeStyle.put(mxConstants.STYLE_STROKECOLOR, "#ff0000");
        caEdgeStyle.put(mxConstants.STYLE_STROKEWIDTH, 1);
        caEdgeStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC);
        caEdgeStyle.put(mxConstants.STYLE_STARTARROW, mxConstants.ARROW_CLASSIC);
        caEdgeStyle.put(mxConstants.STYLE_ENDSIZE, 8);
        caEdgeStyle.put(mxConstants.STYLE_STARTSIZE, 8);
        caEdgeStyle.put(mxConstants.STYLE_NOLABEL, true);
        mxGraph.getStylesheet().putCellStyle("CA_EDGE", caEdgeStyle);
    }
    
    /**
     * Configura el renderizado personalizado de celdas
     */
    private void configureCellRenderer() {
        mxGraph mxGraph = graphAdapter;
        
        // Configurar las celdas con texto simple
        mxGraph.getModel().beginUpdate();
        try {
            for (GraphNode node : graph.vertexSet()) {
                mxCell cell = (mxCell) graphAdapter.getVertexToCellMap().get(node);
                if (cell != null) {
                    // Usar representación de texto en formato tabla
                    cell.setValue(node.getTextRepresentation());
                    cell.setStyle("NODE");
                    
                    // Calcular y establecer tamaño apropiado
                    Dimension size = calculateNodeSize(node);
                    mxGeometry geometry = new mxGeometry(
                        cell.getGeometry().getX(), 
                        cell.getGeometry().getY(), 
                        size.width, 
                        size.height
                    );
                    mxGraph.getModel().setGeometry(cell, geometry);
                }
            }
            
            // Configurar aristas
            for (DefaultEdge edge : graph.edgeSet()) {
                mxCell cell = (mxCell) graphAdapter.getEdgeToCellMap().get(edge);
                if (cell != null) {
                    cell.setStyle("EDGE");
                }
            }
        } finally {
            mxGraph.getModel().endUpdate();
        }
    }
    
    /**
     * Calcula el tamaño apropiado para un nodo basado en su contenido
     */
    private Dimension calculateNodeSize(GraphNode node) {
        if (node.isCANode()) {
            return new Dimension(50, 50);
        }
        
        // Calcular ancho basado en el formato de tabla
        int nameWidth = node.getDisplayName().length() * 8;
        int tableWidth = 140; // Ancho fijo para la tabla (dos columnas)
        
        int width = Math.max(nameWidth, tableWidth);
        width = Math.max(width, 150); // Ancho mínimo
        width = Math.min(width, 400); // Ancho máximo
        
        // Calcular altura basada en el número de filas
        int height = 40; // Altura base para el nombre
        height += 20; // Espacio para el separador
        height += 20; // Espacio para el encabezado de la tabla
        
        if (node.getAttributes() != null && node.getAttributes().length > 0) {
            // Calcular número de filas necesarias
            int rows = node.getAttributes().length;
            if (node.getKnowledgePiece() instanceof Fact && node.getDeltaAttributes() != null) {
                rows = Math.max(rows, node.getDeltaAttributes().length);
            }
            height += rows * 18; // 18 píxeles por fila
        }
        
        height = Math.max(height, 80); // Altura mínima
        
        return new Dimension(width, height);
    }
    
    /**
     * Crea el panel de controles
     */
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        
        JButton zoomInButton = new JButton("Zoom +");
        zoomInButton.addActionListener(e -> graphComponent.zoomIn());
        
        JButton zoomOutButton = new JButton("Zoom -");
        zoomOutButton.addActionListener(e -> graphComponent.zoomOut());
        
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(zoomInButton);
        panel.add(zoomOutButton);
        
        return panel;
    }
    
    /**
     * Aplica un layout jerárquico al grafo
     */
    private void applyHierarchicalLayout() {
        mxHierarchicalLayout layout = new mxHierarchicalLayout(graphAdapter);
        layout.setOrientation(SwingConstants.NORTH);
        layout.setIntraCellSpacing(60);
        layout.setInterRankCellSpacing(100);
        layout.setInterHierarchySpacing(80);
        layout.setParallelEdgeSpacing(40);
        layout.execute(graphAdapter.getDefaultParent());
        
        graphComponent.refresh();
    }
    
    /**
     * Método para obtener todos los nodos del grafo
     * @return 
     */
    public Set<GraphNode> getAllNodes() {
        return new HashSet<>(graph.vertexSet());
    }
    
    /**
     * Método para obtener las aristas del grafo
     * @return 
     */
    public Set<DefaultEdge> getAllEdges() {
        return new HashSet<>(graph.edgeSet());
    }
}