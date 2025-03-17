package Main;

import KnowledgePieces.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;


public class InferenceEngine {
    
    private final Map<KnowledgePiece, Fact> edges;
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
    public Map<KnowledgePiece,Fact> buildTree() {
        
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
         
        newFact.setAttributes( support (potentialFacts, rule) );
        
        facts.add(newFact); // añade el nuevo hecho a la lista de hechos
        edges.put(rule, newFact); // arista regla -> nuevo hecho
        
        for (Fact potentialFact : potentialFacts) {
            edges.put(potentialFact, newFact); // arista hecho -> nuevo hecho
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

        for ( Map.Entry<KnowledgePiece, Fact> edge : edges.entrySet() ) {
            if ( equalFacts(edge.getValue(), newFact) 
                    && edge.getKey() == rule ) {
                
                existsRule = true;
            
            }
            
        }
        
        return existsRule;
        
    }
    
    // Determina si existe agregacion cada vez que se infiere un nuevo hecho
    private boolean anyAggregation (Fact newFact) {
        
        boolean aggregation = false;
        
        for (Map.Entry<KnowledgePiece, Fact> edge : edges.entrySet()) {
            
            if ( edge.getClass() == newFact.getClass() 
                    && equalFacts( (Fact) edge.getKey(), newFact ) ) {
                
                aggregation = true;
                
            } else if ( equalFacts( edge.getValue(), newFact ) ) {
                
                aggregation = true;
                
            }
            
        }
        
        return aggregation;        
    }

    // Realiza la agregación entre hechos, vuelve a construir el arbol
    private void doAggregation(Set<Fact> potentialFacts, Fact newFact, Rule rule) {

        Fact auxFact = null;
        
        for (Fact fact : facts) {
            if ( equalFacts(fact, newFact) ) {
                auxFact = fact; // Se encuentra el hecho igual en la lista
            }
        }
        
        if (auxFact != null) {
            
            facts.remove(auxFact); // Se remueve el hecho igual de la lista hechos
        
        } else {
            
            auxFact = combineFacts(newFact);
        } 
        
        newFact.setAttributes( support (potentialFacts, rule) ); // Calcular los valores de la inferencia 
        edges.put(rule, newFact); // arista regla -> nuevo hecho
        
        for (Fact potentialFact : potentialFacts) {
            edges.put(potentialFact, newFact); // arista hecho -> nuevo hecho
        }
        
        Fact aggregatedFact = new Fact(newFact.getName(), newFact.getArgument(), calculateAggregation(newFact, auxFact)); // Se calcula el hecho agregado
        facts.add(aggregatedFact); // Se agrega el nuevo hecho a la lista
        
        reBuilTree(aggregatedFact); // Recontruir el arbol para el nuevo hecho
        
    }
    
    // Crea un nuevo hecho agregado a partir de hechos iguales en el grafo
    private Fact combineFacts (Fact newFact) {
        
        Set<Fact> aggregatedFacts = new HashSet<>();
            
        for (Map.Entry<KnowledgePiece, Fact> edge : edges.entrySet()) { // Se buscan los hechos iguales el nuevo hecho que no estan en la lista

            if (edge.getKey().getClass() == Fact.class
                    && equalFacts( (Fact) edge.getKey(), newFact) ) {

                aggregatedFacts.add((Fact) edge.getKey());

            } else if (equalFacts( edge.getValue(), newFact)) {
                
                aggregatedFacts.add( edge.getValue() );
                
            }

        }
        
        Fact aux = new Fact(newFact.getName(), newFact.getArgument(), calculateAggregation(aggregatedFacts) ); // Se combina los hechos encontrados en un unico hecho agregado
        
        return aux;
        
    }
    
    // Reconstruye el grafo cada vez que se identifica una nueva agregacion
    private void reBuilTree (Fact newFact) {
        
        Set<KnowledgePiece> removableEdges = new HashSet<>(); // Aristas que se eliminaran
        Set<KnowledgePiece> newEdges = new HashSet<>(); // Nodos origen de las nuevas aristas hacia el nodo agregado
        KnowledgePiece key;
        
        for (Map.Entry<KnowledgePiece, Fact> entry : edges.entrySet()) {
            
            if ( entry.getKey().getClass() == Fact.class
                    && equalFacts( (Fact) entry.getKey(), newFact ) ) { // Se buscan las aristas que se originen en los nodos agregados
                
                key = entry.getKey();
                
                newEdges.add(key); // Se agregan aristas desde los nodos agregados hacia el nuevo nodo
                
                while ( key != null && edges.containsKey(key) ) { // Se recorre el arbol hacia arriba desde los nodos agregados

                    Fact aux = edges.get(key);
                    
                    for (Map.Entry<KnowledgePiece, Fact> entry2 : edges.entrySet()) {
                        if (entry2.getValue() == aux) {
                            removableEdges.add(entry2.getKey()); // Se quitan las aristas que llegan hacia los nodos que se eliminan
                        }
                    }
                    
                    removableEdges.add(aux);
                    facts.remove(aux); // Se eliminan los hechos superiores a la agregacion de la lista
                    removableEdges.add(key); // Se eliminan las aristas superiores a la agregacion
                    key = aux;
                    
                }
                
            } else if (equalFacts( entry.getValue(), newFact ) ) {
                
                key = entry.getValue();
                newEdges.add(key); // Se agregan aristas desde los nodos agregados hacia el nuevo nodo
                
            }

        }
        
        for (KnowledgePiece edge : removableEdges) {
            edges.remove(edge);
        }
        
        for (KnowledgePiece edge : newEdges) {
            edges.put(edge, newFact);
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
    private void conflict () {
        
        Set<Fact> negativeFacts = new HashSet<>();
        
        // Capturar todos los hechos con una negacion
        for ( Fact fact : facts ) {
            if (fact.getName().contains("~")) {
                negativeFacts.add(fact); // Añade a la lista los hechos con negaciones
            }
        }
        
        // Recorrer todos los hechos con una negacion
        for (Fact nf : negativeFacts) {
            // Recorrer todos los hechos y compararlos con los hechos negados
            for ( Fact fact : facts ) {
                if (nf.getName().replace("~", "").equals(fact.getName()) &&
                    nf.getArgument().equals(fact.getArgument())) { // Se identifican hechos que entran en conflicto
                    
                    // Se crean los hechos resultantes de el conflicto (El hecho derrotado y el debilidado)
                    Fact newFact1 = new Fact(nf.getName(), nf.getArgument(), calculateAttack(nf, fact)); 
                    Fact newFact2 = new Fact(fact.getName(), fact.getArgument(), calculateAttack(fact, nf));
                    
                    // Se añaden las aristas desde los hechos en conflicto hacia los hechos resultantes del ataque
                    edges.put(nf, newFact1);
                    edges.put(fact, newFact2);
                    
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
