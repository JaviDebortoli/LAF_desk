package Main;

import KnowledgePieces.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;


public class InferenceEngine {
    
    private final Map<KnowledgePiece, Set<Fact>> edges;
    private final Set<Fact> facts;
    private final Set<Rule> rules;
    private final String[][] functions;

    public InferenceEngine(Set<Fact> facts, Set<Rule> rules, String[][] functions) {
        this.edges = new HashMap<>();
        this.facts = facts;
        this.rules = rules;
        this.functions = functions;
    }
    
    // Inferencias
    public Map<KnowledgePiece, Set<Fact>> buildTree() {
        
        Set<Fact> potentialFacts = new HashSet<>();
        boolean anyNewFact;
        Fact newFact = null;
        int bodyPartsVerified;
        String argument;
        
        do {
            anyNewFact = false; // Indica si se modifico el grafo y hay que repetir el ciclo
            
            for (Rule rule : rules) { // Ciclo de reglas
                
                potentialFacts.clear();
                bodyPartsVerified = 0;

                for (String bodypart : rule.getBody()) { // Ciclo del cuerpo de cada una de las reglas
                    argument = null;
                    
                    for (Fact fact : facts) { // Ciclo de hechos                       
                        if ( bodypart.equals(fact.getName()) && argument == null ) {    
                            argument = fact.getArgument();
                            // Nuevo hecho
                            newFact = new Fact(rule.getHead(), fact.getArgument(), null);
                            // Se cuenta el hecho dentro de los antecedentes de la regla
                            potentialFacts.add(fact);
                            bodyPartsVerified++;
                        } else if (bodypart.equals(fact.getName()) && fact.getArgument().equals(argument)) {
                            // Nuevo hecho
                            newFact = new Fact(rule.getHead(), fact.getArgument(), null);
                            // Se cuenta el hecho dentro de los antecedentes de la regla
                            potentialFacts.add(fact);
                            bodyPartsVerified++;
                        }
                    }
                }
                
                if ( bodyPartsVerified == rule.getBody().size() 
                        && !alreadyExists(newFact, rule) 
                        && !anyAggregation(newFact) ) {
                    
                    addFact(potentialFacts, newFact, rule); // Añade un nuevo hecho 
                    anyNewFact = true; // Indica que hay que repetir el ciclo
                    
                } else if ( bodyPartsVerified == rule.getBody().size() 
                        && !alreadyExists(newFact, rule) 
                        && anyAggregation(newFact) ){
                    
                    doAggregation( potentialFacts, newFact, rule ); // Añade un hecho con agregación 
                    anyNewFact = true; // Indica que hay que repetir el ciclo

                }
            }
        } while (anyNewFact);
        
        conflict(); // Se resulven los conflictos entre hechos
        
        return edges;
    }
    
    // Añade un hecho nuevo
    private void addFact (Set<Fact> potentialFacts, Fact newFact, Rule rule) {
        // Calcular los valores las etiquetas del nuevo hecho 
        newFact.setAttributes( support (potentialFacts, rule) );
        
        // Añadir el nuevo hecho a la lista de hechos
        facts.add(newFact); 
        
        // Añadir la arista desde la regla al nuevo hecho
        if (!edges.containsKey(rule)) {
            edges.put(rule, new HashSet<>());
        }
        edges.get(rule).add(newFact);
        
        // Añadir aristas desde los hechos que permitieron inferir el nuevo hecho
        for (Fact potentialFact : potentialFacts) {
            if (!edges.containsKey(potentialFact)) {
                edges.put(potentialFact, new HashSet<>());
            }
            edges.get(potentialFact).add(newFact);
        }
    }
    
    // Calcula el valor para los atributos de un nuevo hecho inferido
    private Double[] support (Set<Fact> potentialFacts, Rule rule) {
        
        Double[] atributtes = new Double[ potentialFacts.iterator().next().getAttributes().length ];
        Expression expression;
        
        for (int i = 0; i < atributtes.length ; i++) {
            
            atributtes[i] = 0.0;
            
            // Reemplazar los valores de X y Y, y evaluar la funcion para cada uno de los antecedentes
            for (Fact fact : potentialFacts) {
                
                expression = new ExpressionBuilder( functions[i][0] )
                        .variables("X", "Y")
                        .build()
                        .setVariable("X", atributtes[i])
                        .setVariable("Y", fact.getAttributes()[i]);
                
                atributtes[i] = expression.evaluate();
                
            }
            
            // Reemplazar los valores de X y Y, y evaluar la funcion para la regla
            expression = new ExpressionBuilder( functions[i][0] )
                    .variables("X", "Y")
                    .build()
                    .setVariable("X", atributtes[i])
                    .setVariable("Y", rule.getAttributes()[i]);
            
            atributtes[i] = expression.evaluate();
            
            // Ubicar los valores en el intervalo [0, 1]
            if (atributtes[i]>1) {
                atributtes[i] = 1.0;
            } else if (atributtes[i]<0) {
                atributtes[i] = 0.0;
            }
            
        }
        
        return atributtes;
    }
     
    // Determina si dos hechos son iguales en nombre y argumento
    private boolean equalFacts (Fact firstFact, Fact secondFact) {
        
        return firstFact.getName().equals(secondFact.getName()) 
                && firstFact.getArgument().equals(secondFact.getArgument());
    
    } 
    
    // Determina si un hecho ya fue inferido a partir de una regla
    private boolean alreadyExists (Fact newFact, Rule rule) {
        boolean existsRule = false;

        for ( Map.Entry<KnowledgePiece, Set<Fact>> edge : edges.entrySet() ) {
            for (Fact fact : edge.getValue()) {
                if ( equalFacts(fact, newFact) 
                        && edge.getKey() == rule ) {

                    existsRule = true;
                }
            }
        }
        
        return existsRule;
    }
    
    // Determina si existe agregacion cada vez que se infiere un nuevo hecho
    private boolean anyAggregation(Fact newFact) {
        boolean aggregation = false;

        for (Map.Entry<KnowledgePiece, Set<Fact>> entry : edges.entrySet()) {
            KnowledgePiece key = entry.getKey();

            if (key instanceof Fact && equalFacts((Fact) key, newFact)) {
                aggregation = true;
                break;
            }

            for (Fact fact : entry.getValue()) {
                if (equalFacts(fact, newFact)) {
                    aggregation = true;
                    break;
                }
            }

            if (aggregation) {
                break;
            }
        }

        return aggregation;
    }

    // Realiza la agregación entre hechos, vuelve a construir el arbol
    private void doAggregation(Set<Fact> potentialFacts, Fact newFact, Rule rule) {
        Fact auxFact = null;

        for (Fact fact : facts) {
            if (equalFacts(fact, newFact)) {
                auxFact = fact; // Se encuentra el hecho igual en la lista
                break;
            }
        }

        if (auxFact != null) {
            facts.remove(auxFact); // Se remueve el hecho igual de la lista hechos
        } else {
            auxFact = combineFacts(newFact);
        }

        newFact.setAttributes(support(potentialFacts, rule)); // Calcular los valores de la inferencia

        // Añadir la arista desde la regla al nuevo hecho
        if (!edges.containsKey(rule)) {
            edges.put(rule, new HashSet<>());
        }
        edges.get(rule).add(newFact);
        // Añadir aristas desde los hechos que permitieron inferir el nuevo hecho
        for (Fact potentialFact : potentialFacts) {
            if (!edges.containsKey(potentialFact)) {
                edges.put(potentialFact, new HashSet<>());
            }
            edges.get(potentialFact).add(newFact);
        }

        Fact aggregatedFact = new Fact(newFact.getName(), newFact.getArgument(), calculateAggregation(newFact, auxFact)); // Se calcula el hecho agregado
        facts.add(aggregatedFact); // Se agrega el nuevo hecho a la lista

        reBuilTree(aggregatedFact); // Reconstruir el árbol para el nuevo hecho
    }
    
    // Crea un nuevo hecho agregado a partir de hechos iguales en el grafo
    private Fact combineFacts (Fact newFact) {
        Set<Fact> aggregatedFacts = new HashSet<>();
            
        for (Map.Entry<KnowledgePiece, Set<Fact>> edge : edges.entrySet()) { // Se buscan los hechos iguales el nuevo hecho que no estan en la lista
            if (edge.getKey().getClass() == Fact.class
                    && equalFacts( (Fact) edge.getKey(), newFact) ) {

                aggregatedFacts.add((Fact) edge.getKey());

            }
            
            for (Fact piece : edge.getValue()) {
                if ( equalFacts(piece, newFact) ) {

                    aggregatedFacts.add( piece );
                }
            }
        }
        
        // Se combina los hechos encontrados en un unico hecho agregado
        return new Fact(newFact.getName(), newFact.getArgument(), calculateAggregation(aggregatedFacts) );
    }
        
    // Reconstruye el grafo cada vez que se identifica una nueva agregacion
    private void reBuilTree (Fact newFact) {
        Set<KnowledgePiece> newEdges = new HashSet<>(); // Nodos origen de las nuevas aristas hacia el nodo agregado
        Set<KnowledgePiece> removableEdges = new HashSet<>(); // Aristas que se eliminaran
        // Se recorre el grafo buscando hechos iguales
        for (Map.Entry<KnowledgePiece, Set<Fact>> entry : edges.entrySet()) {
            // Se buscan las aristas que se originen en los nodos agregados
            if ( entry.getKey() instanceof Fact && equalFacts( (Fact) entry.getKey(), newFact ) ) {
                newEdges.add(entry.getKey()); // Marcar los nodos agregados para las nuevas aristas
                eraseUpperNodes(entry.getValue()); // Eliminar los nodos superiores la nueva agregacion
            } else {
                for ( Fact fact : entry.getValue() ) {
                    if ( equalFacts(fact, newFact) ) {
                        newEdges.add(fact);
                        removableEdges.add(fact);
                    }
                }
            }
        }
        // Se eliminan las aristas
        for (KnowledgePiece removableEdge : removableEdges) {
            edges.remove(removableEdge);
        }
        // Se agregan aristas desde los nodos agregados hacia el nuevo nodo
        
        for (KnowledgePiece edge : newEdges) {
            if (!edges.containsKey(edge)) {
                edges.put(edge, new HashSet<>());
            }
            edges.get(edge).add(newFact);
        }
        
    }
    
    // Eliminar los nodos superiores dado un conjunto de nodos
    public void eraseUpperNodes (Set<Fact> values) {
        Set<KnowledgePiece> removableEdges = new HashSet<>(); // Aristas que se eliminaran
        
        if (values != null) {
            for (Fact value : values) {
                eraseUpperNodes(edges.get(value));
                removableEdges.add(value); // Se marcan las aristas superiores para eliminarlas
            }
        }
        // Se eliminan las aristas
        for (KnowledgePiece removableEdge : removableEdges) {
            edges.remove(removableEdge);
        }
    }
    
    // Calcular los valores de los atributos cuando hay agregacion
    private Double[] calculateAggregation(Fact newFact, Fact removableFact) {
        
        Double[] atributtes = new Double[ newFact.getAttributes().length ];
        Expression expression;
        
        for (int i = 0; i < atributtes.length ; i++) {
            
            atributtes[i] = 0.0;
            
            // Reemplazar las variables X y Y de la expresion
            expression = new ExpressionBuilder( functions[i][1] )
                    .variables("X", "Y")
                    .build()
                    .setVariable("X", newFact.getAttributes()[i])
                    .setVariable("Y", removableFact.getAttributes()[i]);
            
            // Evaluar la expresion con los parametros actuales
            atributtes[i] = expression.evaluate();
            
            // Ubicar los valores en el intervalo [0, 1]
            if (atributtes[i]>1) {
                atributtes[i] = 1.0;
            } else if (atributtes[i]<0) {
                atributtes[i] = 0.0;
            }
            
        }
        
        return atributtes;
        
    }

    // Calcular los valores de los atributos cuando hay agregacion en hechos que no estan en la lista
    private Double[] calculateAggregation(Set<Fact> aggregatedFacts) {
        
        Double[] atributtes = new Double[ aggregatedFacts.iterator().next().getAttributes().length ];
        
        Expression expression;
        
        for (int i = 0; i < atributtes.length ; i++) {
            
            atributtes[i] = null;
            
            for (Fact fact : aggregatedFacts) {
                
                if(atributtes[i] == null){
                    atributtes[i] = fact.getAttributes()[i];
                } else {
                    // Reemplazar las variables X y Y de la expresion
                    expression = new ExpressionBuilder(functions[i][1])
                            .variables("X", "Y")
                            .build()
                            .setVariable("X", atributtes[i])
                            .setVariable("Y", fact.getAttributes()[i]);
                    
                    // Evaluar la expresion con los parametros actuales
                    atributtes[i] = expression.evaluate();
                }
            }
            
            // Ubicar los valores en el intervalo [0, 1]
            if (atributtes[i]>1) {
                atributtes[i] = 1.0;
            } else if (atributtes[i]<0) {
                atributtes[i] = 0.0;
            }
            
        }
        
        return atributtes;
        
    }
    
    // Trata conflictos entre hechos que se contradicen
    private void conflict() {
        Set<Fact> negativeFacts = new HashSet<>();

        // Capturar todos los hechos con una negación
        for (Fact fact : facts) {
            if (fact.getName().contains("~")) {
                negativeFacts.add(fact);
            }
        }

        // Recorrer todos los hechos con una negación
        for (Fact nf : negativeFacts) {
            // Recorrer todos los hechos y compararlos con los hechos negados
            for (Fact fact : facts) {
                if (nf.getName().replace("~", "").equals(fact.getName()) &&
                    nf.getArgument().equals(fact.getArgument())) {

                    // Se crean los hechos resultantes del conflicto
                    Fact newFact1 = new Fact(nf.getName(), nf.getArgument(), calculateAttack(nf, fact));
                    Fact newFact2 = new Fact(fact.getName(), fact.getArgument(), calculateAttack(fact, nf));

                    // Se añaden las aristas desde los hechos en conflicto hacia los hechos resultantes
                    if (!edges.containsKey(nf)) {
                        edges.put(nf, new HashSet<>());
                    }
                    edges.get(nf).add(newFact1);

                    if (!edges.containsKey(fact)) {
                        edges.put(fact, new HashSet<>());
                    }
                    edges.get(fact).add(newFact2);
                }
            }
        }
    }
    
    // Calcular valores de los atributos para los hechos en conflicto
    private Double[] calculateAttack (Fact f1, Fact f2) {
        
        Double[] attributtes = new Double[f1.getAttributes().length]; // Array vacio
        Expression expression;
        
        for (int i = 0; i < attributtes.length; i++) { 
            
            // Reemplazar las variables X y Y de la expresion
            expression = new ExpressionBuilder( functions[i][2] )
                    .variables("X", "Y")
                    .build()
                    .setVariable("X", f1.getAttributes()[i])
                    .setVariable("Y", f2.getAttributes()[i]);
            
            // Evaluar la expresion con los parametros actuales
            attributtes[i] = expression.evaluate();
            
            // Ubicar los valores en el intervalo [0, 1]
            if (attributtes[i]>1) {
                attributtes[i] = 1.0;
            } else if (attributtes[i]<0) {
                attributtes[i] = 0.0;
            }
            
        }
        
        return attributtes;
        
    }
    
}
