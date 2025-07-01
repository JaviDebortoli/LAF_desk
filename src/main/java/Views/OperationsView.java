package Views;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.border.LineBorder;

public class OperationsView extends JDialog {
    private final List<JTextField[]> functionFields;
    private final int attributeCount;
    private String[][] result;
    private boolean confirmed;

    public OperationsView(JFrame parent, int initialAttributeCount) {
        super(parent, "Function Input Configuration", true);
        this.attributeCount = initialAttributeCount;
        this.functionFields = new ArrayList<>();
        this.confirmed = false;
        initializeComponents();
    }

    private void initializeComponents() {
        setUndecorated(true);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        
        // Panel principal con scroll para los campos de función
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        this.getRootPane().setBorder(new LineBorder(Color.black, 2, true));
        
        // Panel con scroll para los campos de función
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        
        // Inicializar campos para cada atributo
        for (int i = 0; i < attributeCount; i++) {
            addAttributeFields(fieldsPanel);
        }

        JScrollPane scrollPane = new JScrollPane(fieldsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);

        // Panel de botones
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");

        okButton.addActionListener(e -> {
            if (validateInput()) {
                saveResult();
                confirmed = true;
                dispose();
            }
        });

        buttonPanel.add(okButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Configurar el diálogo
        setSize(500, 400);
        setLocationRelativeTo(getParent());
    }

    private void addAttributeFields(JPanel panel) {
        JPanel attributePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField[] fields = new JTextField[3];
        
        // Etiqueta del atributo
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        attributePanel.add(new JLabel("Attribute " + (functionFields.size() + 1)), gbc);

        // Campos para las tres funciones
        String[] functionNames = {"Support", "Aggregation", "Conflict"};
        for (int i = 0; i < 3; i++) {
            gbc.gridx = 0;
            gbc.gridy = i + 1;
            gbc.gridwidth = 1;
            attributePanel.add(new JLabel(functionNames[i] + ":"), gbc);

            gbc.gridx = 1;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            fields[i] = new JTextField("X " + getDefaultOperator(i) + " Y", 20);
            attributePanel.add(fields[i], gbc);
        }

        panel.add(attributePanel);
        functionFields.add(fields);
        panel.revalidate();
        panel.repaint();
    }

    private String getDefaultOperator(int index) {
        return switch (index) {
            case 0 -> "+";
            case 1 -> "*";
            case 2 -> "-";
            default -> "+";
        };
    }

    private boolean validateInput() {
        for (JTextField[] fields : functionFields) {
            for (JTextField field : fields) {
                String text = field.getText().trim();
                if (text.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                        "All function fields must be filled.",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                // TODO otras validaciones
            }
        }
        return true;
    }

    private void saveResult() {
        result = new String[attributeCount][3];
        for (int i = 0; i < attributeCount; i++) {
            for (int j = 0; j < 3; j++) {
                result[i][j] = functionFields.get(i)[j].getText().trim();
            }
        }
    }

    public String[][] getResult() {
        return result;
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
