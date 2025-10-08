package Views;

import InferenceEngine.ArgumentativeGraph;
import InferenceEngine.Pair;
import KnowledgePieces.*;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.ext.JGraphXAdapter;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxGraphView;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
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

    private static final double CA_NODE_SIZE = 50.0;
    private static final double CA_NODE_HALF_SIZE = CA_NODE_SIZE / 2.0;
    private static final double POSITION_STEP = 80.0;
    private static final int MAX_SEARCH_RADIUS = 10;
    private static final double EDGE_PADDING = 6.0;
    
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
        setExtendedState(MAXIMIZED_BOTH);
        
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

        Map<mxCell, mxRectangle> nodeBounds = collectNodeBounds();
        List<mxRectangle> edgeBounds = collectEdgeBounds();

        mxGraph.getModel().beginUpdate();

        try {
            // Crear mapa de Facts a GraphNodes para búsqueda rápida
            Map<Fact, GraphNode> factToNodeMap = new HashMap<>();
            for (GraphNode node : graph.vertexSet()) {
                if (node.getKnowledgePiece() instanceof Fact fact) {
                    factToNodeMap.put(fact, node);
                }
            }

            for (Pair conflictivePair : conflictiveNodes) {
                GraphNode firstNode = factToNodeMap.get(conflictivePair.first());
                GraphNode secondNode = factToNodeMap.get(conflictivePair.second());

                if (firstNode != null && secondNode != null) {
                    GraphNode caNode = new GraphNode("CA");

                    mxCell firstCell = (mxCell) graphAdapter.getVertexToCellMap().get(firstNode);
                    mxCell secondCell = (mxCell) graphAdapter.getVertexToCellMap().get(secondNode);

                    if (firstCell != null && secondCell != null) {
                        Point2D position = findValidCANodePosition(firstCell, secondCell, nodeBounds, edgeBounds);
                        double caCenterX = position.getX();
                        double caCenterY = position.getY();

                        mxCell caCell = (mxCell) mxGraph.insertVertex(
                            mxGraph.getDefaultParent(),
                            null,
                            caNode.getTextRepresentation(),
                            caCenterX - CA_NODE_HALF_SIZE,
                            caCenterY - CA_NODE_HALF_SIZE,
                            CA_NODE_SIZE,
                            CA_NODE_SIZE
                        );

                        caCell.setStyle("CA_NODE");

                        graph.addVertex(caNode);
                        graphAdapter.getVertexToCellMap().put(caNode, caCell);
                        graphAdapter.getCellToVertexMap().put(caCell, caNode);

                        nodeBounds.put(caCell, new mxRectangle(
                            caCenterX - CA_NODE_HALF_SIZE,
                            caCenterY - CA_NODE_HALF_SIZE,
                            CA_NODE_SIZE,
                            CA_NODE_SIZE
                        ));

                        double firstCenterX = firstCell.getGeometry().getCenterX();
                        double firstCenterY = firstCell.getGeometry().getCenterY();
                        double secondCenterX = secondCell.getGeometry().getCenterX();
                        double secondCenterY = secondCell.getGeometry().getCenterY();

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

                        edgeBounds.add(createEdgeBoundingBox(firstCenterX, firstCenterY, caCenterX, caCenterY));
                        edgeBounds.add(createEdgeBoundingBox(secondCenterX, secondCenterY, caCenterX, caCenterY));
                    }
                }
            }
        } finally {
            mxGraph.getModel().endUpdate();
        }

        graphComponent.refresh();
    }

    private Map<mxCell, mxRectangle> collectNodeBounds() {
        Map<mxCell, mxRectangle> bounds = new HashMap<>();
        for (mxCell cell : graphAdapter.getVertexToCellMap().values()) {
            mxGeometry geometry = cell.getGeometry();
            if (geometry != null) {
                bounds.put(cell, new mxRectangle(
                    geometry.getX(),
                    geometry.getY(),
                    geometry.getWidth(),
                    geometry.getHeight()
                ));
            }
        }
        return bounds;
    }

    private List<mxRectangle> collectEdgeBounds() {
        List<mxRectangle> bounds = new ArrayList<>();
        mxGraphView view = graphComponent.getGraph().getView();
        view.validate();
        for (mxCell edgeCell : graphAdapter.getEdgeToCellMap().values()) {
            mxCellState state = view.getState(edgeCell);
            if (state != null && state.getBoundingBox() != null) {
                bounds.add(expandRectangle(state.getBoundingBox(), EDGE_PADDING));
            }
        }
        return bounds;
    }

    private Point2D findValidCANodePosition(mxCell firstCell, mxCell secondCell,
                                            Map<mxCell, mxRectangle> nodeBounds,
                                            List<mxRectangle> edgeBounds) {
        mxGeometry firstGeometry = firstCell.getGeometry();
        mxGeometry secondGeometry = secondCell.getGeometry();

        double firstCenterX = firstGeometry.getCenterX();
        double firstCenterY = firstGeometry.getCenterY();
        double secondCenterX = secondGeometry.getCenterX();
        double secondCenterY = secondGeometry.getCenterY();

        double midX = (firstCenterX + secondCenterX) / 2.0;
        double midY = (firstCenterY + secondCenterY) / 2.0;

        double dx = secondCenterX - firstCenterX;
        double dy = secondCenterY - firstCenterY;

        List<Point2D.Double> directions = createSearchDirections(dx, dy);
        Set<mxCell> excludedForSegments = new HashSet<>();
        excludedForSegments.add(firstCell);
        excludedForSegments.add(secondCell);

        for (int radius = 0; radius <= MAX_SEARCH_RADIUS; radius++) {
            double distance = radius * POSITION_STEP;
            for (Point2D.Double direction : directions) {
                if (radius == 0 && direction.distance(0, 0) > 0) {
                    continue;
                }

                double candidateCenterX = midX + direction.getX() * distance;
                double candidateCenterY = midY + direction.getY() * distance;

                mxRectangle candidateBounds = new mxRectangle(
                    candidateCenterX - CA_NODE_HALF_SIZE,
                    candidateCenterY - CA_NODE_HALF_SIZE,
                    CA_NODE_SIZE,
                    CA_NODE_SIZE
                );

                if (isValidCANodePosition(candidateBounds, candidateCenterX, candidateCenterY,
                    firstCenterX, firstCenterY, secondCenterX, secondCenterY,
                    nodeBounds, edgeBounds, excludedForSegments)) {
                    return new Point2D.Double(candidateCenterX, candidateCenterY);
                }
            }
        }

        // Búsqueda adicional hacia abajo en caso de no encontrar posición libre en el radio inicial
        for (int extra = 1; extra <= MAX_SEARCH_RADIUS * 2; extra++) {
            double candidateCenterX = midX;
            double candidateCenterY = midY + extra * POSITION_STEP;
            mxRectangle candidateBounds = new mxRectangle(
                candidateCenterX - CA_NODE_HALF_SIZE,
                candidateCenterY - CA_NODE_HALF_SIZE,
                CA_NODE_SIZE,
                CA_NODE_SIZE
            );
            if (isValidCANodePosition(candidateBounds, candidateCenterX, candidateCenterY,
                firstCenterX, firstCenterY, secondCenterX, secondCenterY,
                nodeBounds, edgeBounds, excludedForSegments)) {
                return new Point2D.Double(candidateCenterX, candidateCenterY);
            }
        }

        return new Point2D.Double(midX, midY + (MAX_SEARCH_RADIUS + 1) * POSITION_STEP);
    }

    private List<Point2D.Double> createSearchDirections(double dx, double dy) {
        List<Point2D.Double> directions = new ArrayList<>();
        directions.add(new Point2D.Double(0, 0));
        addDirection(directions, -dy, dx);
        addDirection(directions, dy, -dx);
        addDirection(directions, dx, dy);
        addDirection(directions, -dx, -dy);
        addDirection(directions, 0, 1);
        addDirection(directions, 0, -1);
        addDirection(directions, 1, 0);
        addDirection(directions, -1, 0);
        addDirection(directions, 1, 1);
        addDirection(directions, -1, 1);
        addDirection(directions, 1, -1);
        addDirection(directions, -1, -1);
        return directions;
    }

    private void addDirection(List<Point2D.Double> directions, double x, double y) {
        double length = Math.hypot(x, y);
        if (length > 0) {
            directions.add(new Point2D.Double(x / length, y / length));
        }
    }

    private boolean isValidCANodePosition(mxRectangle candidateBounds,
                                           double candidateCenterX,
                                           double candidateCenterY,
                                           double firstCenterX,
                                           double firstCenterY,
                                           double secondCenterX,
                                           double secondCenterY,
                                           Map<mxCell, mxRectangle> nodeBounds,
                                           List<mxRectangle> edgeBounds,
                                           Set<mxCell> excludedNodes) {
        if (overlapsNodes(candidateBounds, nodeBounds.values())) {
            return false;
        }

        if (overlapsEdges(candidateBounds, edgeBounds)) {
            return false;
        }

        if (segmentIntersectsNodes(firstCenterX, firstCenterY, candidateCenterX, candidateCenterY, nodeBounds, excludedNodes)) {
            return false;
        }

        if (segmentIntersectsNodes(secondCenterX, secondCenterY, candidateCenterX, candidateCenterY, nodeBounds, excludedNodes)) {
            return false;
        }

        return true;
    }

    private boolean overlapsNodes(mxRectangle candidateBounds, Collection<mxRectangle> nodes) {
        for (mxRectangle rect : nodes) {
            if (rect != null && rect.intersects(candidateBounds)) {
                return true;
            }
        }
        return false;
    }

    private boolean overlapsEdges(mxRectangle candidateBounds, Collection<mxRectangle> edges) {
        for (mxRectangle rect : edges) {
            if (rect != null && rect.intersects(candidateBounds)) {
                return true;
            }
        }
        return false;
    }

    private boolean segmentIntersectsNodes(double x1, double y1, double x2, double y2,
                                           Map<mxCell, mxRectangle> nodeBounds,
                                           Set<mxCell> excludedNodes) {
        Line2D segment = new Line2D.Double(x1, y1, x2, y2);
        for (Map.Entry<mxCell, mxRectangle> entry : nodeBounds.entrySet()) {
            if (excludedNodes.contains(entry.getKey())) {
                continue;
            }
            mxRectangle rect = entry.getValue();
            if (rect != null && segment.intersects(rect)) {
                return true;
            }
        }
        return false;
    }

    private mxRectangle createEdgeBoundingBox(double x1, double y1, double x2, double y2) {
        double minX = Math.min(x1, x2);
        double minY = Math.min(y1, y2);
        double width = Math.max(Math.abs(x1 - x2), 1);
        double height = Math.max(Math.abs(y1 - y2), 1);
        return expandRectangle(new mxRectangle(minX, minY, width, height), EDGE_PADDING);
    }

    private mxRectangle expandRectangle(mxRectangle rectangle, double padding) {
        return new mxRectangle(
            rectangle.getX() - padding,
            rectangle.getY() - padding,
            rectangle.getWidth() + padding * 2,
            rectangle.getHeight() + padding * 2
        );
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
        caEdgeStyle.put(mxConstants.STYLE_STROKECOLOR, "#ff0000");
        caEdgeStyle.put(mxConstants.STYLE_STROKEWIDTH, 1);
        caEdgeStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC);
        caEdgeStyle.put(mxConstants.STYLE_STARTARROW, mxConstants.ARROW_CLASSIC);
        caEdgeStyle.put(mxConstants.STYLE_ENDSIZE, 8);
        caEdgeStyle.put(mxConstants.STYLE_STARTSIZE, 8);
        caEdgeStyle.put(mxConstants.STYLE_NOLABEL, true);
        caEdgeStyle.put(mxConstants.STYLE_NOEDGESTYLE, "1");
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