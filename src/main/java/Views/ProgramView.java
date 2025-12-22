package Views;

import KnowledgePieces.Rule;
import KnowledgePieces.Fact;
import InferenceEngine.InferenceEngine;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;

/**
 * Vista para cargar hechos y reglas.
 * Versión modificada para permitir redimensionar/maximizar sin romper el orden
 * (scroll al centro y botón abajo a la derecha).
 */
public class ProgramView extends javax.swing.JFrame {

    private final List<Fact> facts;
    private final List<Rule> rules;
    String[][] functions;

    public ProgramView() {
        facts = new ArrayList<>();
        rules = new ArrayList<>();
        initComponents();
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        programPanel = new javax.swing.JPanel();
        nextButton = new javax.swing.JButton();
        programScrollPanel = new javax.swing.JScrollPane();
        programTextArea = new javax.swing.JTextArea();

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("LAF");
        setResizable(true);

        programPanel.setBackground(new java.awt.Color(255, 255, 255));

        nextButton.setFont(new java.awt.Font("Segoe UI", 0, 16)); // NOI18N
        nextButton.setText("SIGUIENTE");
        nextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextButtonActionPerformed(evt);
            }
        });

        programTextArea.setColumns(20);
        programTextArea.setRows(5);
        programTextArea.setVerifyInputWhenFocusTarget(false);
        programTextArea.setFont(new java.awt.Font("Segoe UI", 0, 20));
        programScrollPanel.setViewportView(programTextArea);

        programPanel.setLayout(new java.awt.BorderLayout(12, 12));
        programPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Centro: el scroll crece/encoge con la ventana
        programPanel.add(programScrollPanel, java.awt.BorderLayout.CENTER);

        // Sur: botón alineado a la derecha
        javax.swing.JPanel southPanel = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0)
        );
        southPanel.setOpaque(false); // mantener el fondo blanco del programPanel
        southPanel.add(nextButton);
        programPanel.add(southPanel, java.awt.BorderLayout.SOUTH);

        // ContentPane: también BorderLayout para ocupar todo el frame
        getContentPane().setLayout(new java.awt.BorderLayout());
        getContentPane().add(programPanel, java.awt.BorderLayout.CENTER);

        // Tamaños sugeridos (opcionales, pero ayudan a una UX estable)
        setMinimumSize(new java.awt.Dimension(600, 450));

        pack();
        setLocationRelativeTo(null);
    }

    private void nextButtonActionPerformed(java.awt.event.ActionEvent evt) {
        // Limpiar hechos y reglas
        facts.clear();
        rules.clear();

        String program = programTextArea.getText();
        String[] lines = program.split("\n");

        for (String line : lines) {

            line = line.trim();

            if (line.isEmpty()) continue;

            if (line.contains(":-")) {
                processRule(line);
            } else {
                processFact(line);
            }

        }

        setFunctions();

        InferenceEngine laf = new InferenceEngine(facts, rules, functions);
        GraphView graphView = new GraphView(laf.buildTree());
        graphView.setVisible(true);
    }

    // Procesar las reglas
    private void processRule(String line) {

        // Separar la regla de los atributos
        String[] mainParts = line.split("\\. ");

        String ruleData = mainParts[0];
        String attributesStr = mainParts[1].replaceAll("[{}\\s]", "");

        // Separar cabeza y cuerpo
        String[] ruleParts = ruleData.split(":-");

        // Procesar la cabeza
        String headPart = ruleParts[0].trim();
        int openParenIndex = headPart.indexOf('(');

        String head = headPart.substring(0, openParenIndex).trim();

        // Procesar el cuerpo
        String bodyPart = ruleParts[1].trim();
        ArrayList<String> body = new ArrayList<>();

        // Dividir el cuerpo en predicados individuales
        String[] predicates = bodyPart.split(",");
        for (String predicate : predicates) {
            predicate = predicate.trim();
            if (!predicate.isEmpty()) {
                body.add(predicate.replaceAll("\\(.*?\\)", ""));
            }
        }

        // Procesar los atributos
        String[] attributeStrings = attributesStr.split(",");
        Double[] attributes = new Double[attributeStrings.length];

        try {

            for (int i = 0; i < attributeStrings.length; i++) {
                attributes[i] = Double.valueOf(attributeStrings[i]);
            }

            // Crear nueva instancia de Rule
            rules.add(new Rule(head, body, attributes));

        } catch (NumberFormatException e) {

            // Manejar el error si los atributos no son números válidos
            System.err.println("Error parsing attributes in line: " + line);

        }

    }

    // Procesar los hechos
    private void processFact(String line) {

        // Separar el nombre y argumento de los atributos
        String[] mainParts = line.split("\\. ");

        String factData = mainParts[0];
        String attributesStr = mainParts[1].replaceAll("[{}\\s]", "");

        // Extraer nombre y argumento
        int openParenIndex = factData.indexOf('(');
        int closeParenIndex = factData.indexOf(')');

        String name = factData.substring(0, openParenIndex).trim();
        String argument = factData.substring(openParenIndex + 1, closeParenIndex).trim();

        // Procesar los atributos
        String[] attributeStrings = attributesStr.split(",");
        Double[] attributes = new Double[attributeStrings.length];

        try {

            for (int i = 0; i < attributeStrings.length; i++) {
                attributes[i] = Double.valueOf(attributeStrings[i]);
            }

            // Crear nueva instancia de Fact
            facts.add(new Fact(name, argument, attributes));

        } catch (NumberFormatException e) {

            // Manejar el error si los atributos no son números válidos
            System.err.println("Error parsing attributes in line: " + line);

        }

    }

    // Procesar las funciones
    private void setFunctions() {

        int attributes = facts.getFirst().getAttributes().length;

        OperationsView dialog = new OperationsView(this, attributes);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            String[][] userFunctions = dialog.getResult();
            functions = new String[attributes][3];

            for (int i = 0; i < attributes; i++) {
                System.arraycopy(userFunctions[i], 0, functions[i], 0, 3);
            }
        } else {
            // Si el usuario cancela, usar las funciones por defecto
            functions = new String[attributes][3];
            for (int i = 0; i < attributes; i++) {
                functions[i][0] = "X + Y";
                functions[i][1] = "X * Y";
                functions[i][2] = "X - Y";
            }
        }

    }

    // Variables declaration
    private javax.swing.JButton nextButton;
    private javax.swing.JPanel programPanel;
    private javax.swing.JScrollPane programScrollPanel;
    private javax.swing.JTextArea programTextArea;
}