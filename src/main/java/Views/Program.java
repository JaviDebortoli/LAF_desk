package Views;

import KnowledgePieces.Rule;
import KnowledgePieces.Fact;
import Main.InferenceEngine;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Program extends javax.swing.JFrame {

    private final Set<Fact> facts;
    private final Set<Rule> rules;
    String[][] functions;
    
    public Program() {
        
        facts = new HashSet<>();
        rules = new HashSet<>();
        initComponents();
        
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        programPanel = new javax.swing.JPanel();
        nextButton = new javax.swing.JButton();
        programScrollPanel = new javax.swing.JScrollPane();
        programTextArea = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("LAF");
        setResizable(false);

        programPanel.setBackground(new java.awt.Color(255, 255, 255));

        nextButton.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        nextButton.setText("»");
        nextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextButtonActionPerformed(evt);
            }
        });

        programTextArea.setColumns(20);
        programTextArea.setRows(5);
        programTextArea.setVerifyInputWhenFocusTarget(false);
        programScrollPanel.setViewportView(programTextArea);

        javax.swing.GroupLayout programPanelLayout = new javax.swing.GroupLayout(programPanel);
        programPanel.setLayout(programPanelLayout);
        programPanelLayout.setHorizontalGroup(
            programPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(programPanelLayout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addGroup(programPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(nextButton, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(programScrollPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 740, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(30, Short.MAX_VALUE))
        );
        programPanelLayout.setVerticalGroup(
            programPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(programPanelLayout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(programScrollPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 483, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(nextButton, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(30, 30, 30))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(programPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(programPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void nextButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextButtonActionPerformed
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

        // Ahora buildTree() retorna Map<KnowledgePiece, Set<Fact>> en lugar de Map<KnowledgePiece, Fact>
        Tree.visualizeInferenceTree(laf.buildTree());
    }//GEN-LAST:event_nextButtonActionPerformed
    
    // Procesar las reglas
    private void processRule (String line) {
        
        // Separar la regla de los atributos
        String[] mainParts = line.split("\\. ");
        
        String ruleData = mainParts[0];
        String attributesStr = mainParts[1].replaceAll("[{}\\s]", "");
        
        // Separar cabeza y cuerpo
        String[] ruleParts = ruleData.split(":-");
        
        // Procesar la cabeza
        String headPart = ruleParts[0].trim();
        int openParenIndex = headPart.indexOf('(');
        int closeParenIndex = headPart.indexOf(')');
        
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
    private void processFact (String line) {
        
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
        
        int attributes = facts.iterator().next().getAttributes().length;

        FunctionInputDialog dialog = new FunctionInputDialog(this, attributes);
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
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton nextButton;
    private javax.swing.JPanel programPanel;
    private javax.swing.JScrollPane programScrollPanel;
    private javax.swing.JTextArea programTextArea;
    // End of variables declaration//GEN-END:variables
}
