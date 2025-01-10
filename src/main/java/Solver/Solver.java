package Solver;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class Solver {
    
    // Expresión de la función (dinámica)
    private final String functionExpression;
    
    // Mapa para almacenar las funciones asociadas a las variables
    private final Map<String, Function<Double, Double>> variableFunctions;

    public Solver(String functionExpression) {
        this.functionExpression = functionExpression;
        this.variableFunctions = new HashMap<>();
    }

    // Asignar una función a una variable
    public void assignFunctionToVariable(String variable, Function<Double, Double> function) {
        variableFunctions.put(variable, function);
    }

    // Evaluar la función con valores dinámicos para las variables
    public double evaluate(Map<String, Double> variableValues) {
        ExpressionBuilder builder = new ExpressionBuilder(functionExpression);

        // Agregar las variables de la expresión
        for (String var : variableValues.keySet()) {
            builder.variable(var);
        }

        Expression expression = builder.build();

        // Reemplazar variables con sus funciones aplicadas a los valores ingresados
        for (Map.Entry<String, Double> entry : variableValues.entrySet()) {
            String varName = entry.getKey();
            Double varValue = entry.getValue();

            // Si hay una función asignada a esta variable, la aplicamos
            if (variableFunctions.containsKey(varName)) {
                varValue = variableFunctions.get(varName).apply(varValue);
            }
            expression.setVariable(varName, varValue);
        }

        // Evaluar la expresión
        return expression.evaluate();
    }
    
}
