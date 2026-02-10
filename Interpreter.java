package Interpreter;

import AST.*;

import java.util.*;

public class Interpreter {
    private TranNode top;


    /** Constructor - get the interpreter ready to run. Set members from parameters and "prepare" the class.
     *
     * Store the tran node.
     * Add any built-in methods to the AST
     * @param top - the head of the AST
     */
    public Interpreter(TranNode top) {
        this.top = top;

        ClassNode classNodehere = new ClassNode();
        classNodehere.name ="console";
        var method = new ConsoleWrite();
        method.name="write";
        method.isShared=true;
        method.isVariadic = true;
        //ConsoleWriteMethod writeMethod = new ConsoleWriteMethod();

        classNodehere.methods.add(method);
//        classNodehere.methods.add(writeMethod);

        top.Classes.add(classNodehere);

    }

        /**
         * This is the public interface to the interpreter. After parsing, we will create an interpreter and call start to
         * start interpreting the code.
         * <p>
         * Search the classes in Tran for a method that is "isShared", named "start", that is not private and has no parameters
         * Call "InterpretMethodCall" on that method, then return.
         * Throw an exception if no such method exists.
         */
        public void start() {
            for (ClassNode classNode : top.Classes) {
                for (MethodDeclarationNode method : classNode.methods) {
                    if (method.name.equals("start") && method.isShared && !method.isPrivate && method.parameters.isEmpty()) {
                        interpretMethodCall(Optional.empty(), method, new LinkedList<>());
                        return;
                    }
                }
            }
            throw new RuntimeException("No suitable 'start' method found.");

        }

        //              Running Methods

        /**
         * Find the method (local to this class, shared (like Java's system.out.print), or a method on another class)
         * Evaluate the parameters to have a list of values
         * Use interpretMethodCall() to actually run the method.
         * <p>
         * Call GetParameters() to get the parameter value list
         * Find the method. This is tricky - there are several cases:
         * someLocalMethod() - has NO object name. Look in "object"
         * console.write() - the objectName is a CLASS and the method is shared
         * bestStudent.getGPA() - the objectName is a local or a member
         * <p>
         * Once you find the method, call InterpretMethodCall() on it. Return the list that it returns.
         * Throw an exception if we can't find a match.
         *
         * @param object - the object we are inside right now (might be empty)
         * @param locals - the current local variables
         * @param mc     - the method call
         * @return - the return values
         */
        private List<InterpreterDataType> findMethodForMethodCallAndRunIt(Optional<ObjectIDT> object, HashMap<String, InterpreterDataType> locals, MethodCallStatementNode mc) {
            List<InterpreterDataType> result = null;

            if (object.isPresent()) {
                // Check local methods
                for (MethodDeclarationNode method : object.get().astNode.methods) {
                    if (doesMatch(method, mc, result)) {
                        result = interpretMethodCall(object, method, getParameters(object, locals, mc));
                        break;
                    }
                }
            } else {
                for (ClassNode classNode : top.Classes) {
                    for (MethodDeclarationNode method : classNode.methods) {
                        if (doesMatch(method, mc, result)) {
                            result = interpretMethodCall(Optional.empty(), method, getParameters(object, locals, mc));
                            break;
                        }
                    }
                }
            }
            if (result == null) {
                throw new RuntimeException("Method call not found or not matching.");
            }
            return result;
        }

        /**
         * Run a "prepared" method (found, parameters evaluated)
         * This is split from findMethodForMethodCallAndRunIt() because there are a few cases where we don't need to do the finding:
         * in start() and dealing with loops with iterator objects, for example.
         * <p>
         * Check to see if "m" is a built-in. If so, call Execute() on it and return
         * Make local variables, per "m"
         * If the number of passed in values doesn't match m's "expectations", throw
         * Add the parameters by name to locals.
         * Call InterpretStatementBlock
         * Build the return list - find the names from "m", then get the values for those names and add them to the list.
         *
         * @param object - The object this method is being called on (might be empty for shared)
         * @param m      - Which method is being called
         * @param values - The values to be passed in
         * @return the returned values from the method
         */
        private List<InterpreterDataType> interpretMethodCall(Optional<ObjectIDT> object, MethodDeclarationNode m, List<InterpreterDataType> values) {
            List<InterpreterDataType> retVal = new LinkedList<>();

            // Handle built-in method (like console.write)
            if (m instanceof BuiltInMethodDeclarationNode) {
                retVal.addAll(((BuiltInMethodDeclarationNode) m).Execute(values));
            } else {
                // Handle user-defined methods (local methods)
                HashMap<String, InterpreterDataType> locals = new HashMap<>();
                if (m.parameters.size() != values.size()) {
                    throw new RuntimeException("Parameter count mismatch.");
                }

                // Map parameters to local variables
                for (int i = 0; i < values.size(); i++) {
                    String paramName = m.parameters.get(i).name;
                    locals.put(paramName, values.get(i));
                }
                // Initialize declared local variables
                for (VariableDeclarationNode localVar : m.locals) {
                    String localName = localVar.name;
                    // Initialize locals with a default value (e.g., null or a default instance)
                    locals.put(localName, null);
                }

                // Interpret the method body
                interpretStatementBlock(object, m.statements, locals);

                // Build the return list
                for (VariableDeclarationNode returnVar : m.returns) {
                    String returnName = returnVar.name;
                    if (locals.containsKey(returnName)) {
                        retVal.add(locals.get(returnName));
                    } else {
                        throw new RuntimeException("Return variable " + returnName + " not found.");
                    }
                }
            }
            return retVal;
        }
        //              Running Constructors

        /**
         * This is a special case of the code for methods. Just different enough to make it worthwhile to split it out.
         * <p>
         * Call GetParameters() to populate a list of IDT's
         * Call GetClassByName() to find the class for the constructor
         * If we didn't find the class, throw an exception
         * Find a constructor that is a good match - use DoesConstructorMatch()
         * Call InterpretConstructorCall() on the good match
         *
         * @param callerObj - the object that we are inside when we called the constructor
         * @param locals    - the current local variables (used to fill parameters)
         * @param mc        - the method call for this construction
         * @param newOne    - the object that we just created that we are calling the constructor for
         */
        private void findConstructorAndRunIt(Optional<ObjectIDT> callerObj, HashMap<String, InterpreterDataType> locals, MethodCallStatementNode mc, ObjectIDT newOne) {
            //Retrieve parameters
            List<InterpreterDataType> parameters = getParameters(callerObj, locals, mc); // Get the evaluated parameters


            Optional<ClassNode> classNodeOpt = getClassByName(newOne.astNode.name); // Get the class by name

            if (classNodeOpt.isEmpty()) {
                throw new RuntimeException("Class not found: " + newOne.astNode.name); // If the class isn't found, throw an error
            }

            ClassNode classNode = classNodeOpt.get(); // Retrieve the ClassNode

            //Look for a matching constructor in the class
            ConstructorNode matchingConstructor = null;
            for (ConstructorNode constructor : classNode.constructors) {
                if (doesConstructorMatch(constructor, mc, parameters)) {
                    matchingConstructor = constructor;
                    break;
                }
            }

            //If no matching constructor is found, throw an exception
            if (matchingConstructor == null) {
                throw new RuntimeException("No matching constructor found for class: " + newOne.astNode.name);
            }

            // Interpret the constructor call
            interpretConstructorCall(newOne, matchingConstructor, parameters); // Run the constructor
        }

        /**
         * Similar to interpretMethodCall, but "just different enough" - for example, constructors don't return anything.
         * <p>
         * Creates local variables (as defined by the ConstructorNode), calls Instantiate() to do the creation
         * Checks to ensure that the right number of parameters were passed in, if not throw.
         * Adds the parameters (with the names from the ConstructorNode) to the locals.
         * Calls InterpretStatementBlock
         *
         * @param object - the object that we allocated
         * @param c      - which constructor is being called
         * @param values - the parameter values being passed to the constructor
         */
        private void interpretConstructorCall(ObjectIDT object, ConstructorNode c, List<InterpreterDataType> values) {
            if (c.parameters.size() != values.size()) {
                throw new RuntimeException("Constructor parameters count mismatch. Expected "
                        + c.parameters.size() + " but got " + values.size());
            }

            //Add parameters to local variables
            //Add the parameters (with the names from the ConstructorNode) to the local variables
            HashMap<String, InterpreterDataType> locals = new HashMap<>();
            for (int i = 0; i < c.parameters.size(); i++) {
                String paramName = c.parameters.get(i).name;
                InterpreterDataType paramValue = values.get(i);
                locals.put(paramName, paramValue);
            }

            //Call Instantiate() to create the object
            // This is typically handled by the constructor's implementation, which might initialize the object's members.
            instantiate(object.astNode.name);

            //Interpret the constructor's statement block
            // Run the statements defined in the constructor's body
            for (StatementNode stmt : c.statements) {
                interpretStatementBlock(Optional.of(object), c.statements, locals);
            }
        }

        //              Running Instructions

        /**
         * Given a block (which could be from a method or an "if" or "loop" block, run each statement.
         * Blocks, by definition, do ever statement, so iterating over the statements makes sense.
         * <p>
         * For each statement in statements:
         * check the type:
         * For AssignmentNode, FindVariable() to get the target. Evaluate() the expression. Call Assign() on the target with the result of Evaluate()
         * For MethodCallStatementNode, call doMethodCall(). Loop over the returned values and copy the into our local variables
         * For LoopNode - there are 2 kinds.
         * Setup:
         * If this is a Loop over an iterator (an Object node whose class has "iterator" as an interface)
         * Find the "getNext()" method; throw an exception if there isn't one
         * Loop:
         * While we are not done:
         * if this is a boolean loop, Evaluate() to get true or false.
         * if this is an iterator, call "getNext()" - it has 2 return values. The first is a boolean (was there another?), the second is a value
         * If the loop has an assignment variable, populate it: for boolean loops, the true/false. For iterators, the "second value"
         * If our answer from above is "true", InterpretStatementBlock() on the body of the loop.
         * For If - Evaluate() the condition. If true, InterpretStatementBlock() on the if's statements. If not AND there is an else, InterpretStatementBlock on the else body.
         *
         * @param object     - the object that this statement block belongs to (used to get member variables and any members without an object)
         * @param statements - the statements to run
         * @param locals     - the local variables
         */
        private void interpretStatementBlock(Optional<ObjectIDT> object, List<StatementNode> statements, HashMap<String, InterpreterDataType> locals) {
            for (StatementNode statement : statements) {
                if (statement instanceof AssignmentNode) {
                    // Handle AssignmentNode
                    AssignmentNode assignNode = (AssignmentNode) statement;

                    // Fetch the target variable's name
                    String targetName = String.valueOf(assignNode.target);

                    // Evaluate the expression to get the new value
                    InterpreterDataType value = evaluate(locals, object, assignNode.expression);

                    if (object.isPresent() && object.get().members.containsKey(targetName)) {
                        // Update the value of the member in ObjectIDT
                        object.get().members.put(targetName, value);
                    } else if (locals.containsKey(targetName)) {
                        // Update a local variable
                        locals.put(targetName, value);
                    } else {
                        throw new RuntimeException("Variable not found: " + targetName);
                    }

                    // Find the variable using the updated method signature
                    InterpreterDataType target = findVariable(targetName, locals, object);

                    ExpressionNode expression = assignNode.expression;

                    // Evaluate the expression on the right side of the assignment

                    // Assign the evaluated value to the target variable
                    locals.put(targetName, value);
                } else if (statement instanceof MethodCallStatementNode) {
                    MethodCallStatementNode methodCall = (MethodCallStatementNode) statement;

                    // Extract parameters
                    List<InterpreterDataType> parameters = getParameters(object, locals, methodCall);

                    // Check if the method is a built-in
                    if ("write".equals(methodCall.methodName) ) {

                       ClassNode consoleClass = top.Classes.stream()
                                .filter(c -> "console".equals(c.name))
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException("Class 'console' not found."));

                        // Access the 'write' method
                        MethodDeclarationNode writeMethod = consoleClass.methods.stream()
                                .filter(m -> "write".equals(m.name))
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException("Method 'write' not found in class 'console'."));

                        // Cast to ConsoleWrite and execute
                        if (writeMethod instanceof ConsoleWrite) {
                            ((ConsoleWrite) writeMethod).Execute(parameters);
                        } else {
                            throw new RuntimeException("Method 'write' is not a ConsoleWrite instance.");
                        }

                } else {
                        // Handle user-defined method calls


                        // Loop over the returned values and store them in locals if needed
                        if (methodCall.returnValues != null) {
                            for (int i = 0; i < methodCall.returnValues.size(); i++) {
                                // Access each return value as a VariableReferenceNode
                                VariableReferenceNode returnValueNode = methodCall.returnValues.get(i);

                                // Evaluate the VariableReferenceNode to get an InterpreterDataType
                                InterpreterDataType evaluatedValue = evaluate(locals, object, returnValueNode);

                                // Put the evaluated value into locals using the variable's name as the key
                                locals.put(returnValueNode.name, evaluatedValue);
                            }
                        }
                    }
                } else if (statement instanceof LoopNode) {
                    LoopNode loopNode = (LoopNode) statement;

                    // Check if the loop uses an iterator or a boolean expression
                    boolean isIteratorLoop = false;
                    Optional<ObjectIDT> iteratorObject = Optional.empty();

                    VariableReferenceNode variableReferenceNode = loopNode.assignment.get();


                    // Evaluate the expression to check if it's an iterator
                    InterpreterDataType expressionResult = evaluate(locals, object, loopNode.expression);

                    // Check if the result is an ObjectIDT and if it implements an iterator interface
                    if (expressionResult instanceof ObjectIDT) {
                        ObjectIDT obj = (ObjectIDT) expressionResult;
                        String className = obj.astNode.name;

                        // get the class
                        Optional<ClassNode> classNodeOpt = getClassByName(className);
                        if (classNodeOpt.isPresent()) {
                            ClassNode classNode = classNodeOpt.get();


                            Optional<MethodDeclarationNode> getNextMethodOpt = classNode.methods.stream()
                                    .filter(m -> m.name.equals("getNext"))
                                    .findFirst();

                            if (getNextMethodOpt.isPresent()) {
                                isIteratorLoop = true;
                                iteratorObject = Optional.of(obj);
                            }
                        }
                    }
                    if (isIteratorLoop) {
                        // Iterator-based loop
                        ObjectIDT iterObj = iteratorObject.get();
                        ClassNode classNode = new ClassNode();

                        MethodCallStatementNode methodCallStatementNode = new MethodCallStatementNode();
                        methodCallStatementNode.methodName = "getNext";

                        List<InterpreterDataType> result = (List<InterpreterDataType>) getMethodFromObject(iterObj, methodCallStatementNode, List.of());


                        // The first value indicates if there's a next item (boolean)
                        BooleanIDT hasNextIDT = (BooleanIDT) result.get(0);  // Getting the first result
                        boolean hasNext = hasNextIDT.Value;
                        if (!hasNext) break;

                        // The second value is the next item from the iterator
                        InterpreterDataType nextValue = result.get(1);

                        // If there is an assignment variable, assign the value to it
                        if (loopNode.assignment.isPresent()) {
                            locals.put(loopNode.assignment.get().name, nextValue);
                        }

                        // Execute the loop body
                        interpretStatementBlock(object, loopNode.statements, locals);

                    } else {
                        // Boolean condition-based loop
                        while (((BooleanIDT) evaluate(locals, object, loopNode.expression)).Value) {
                            // If there's an assignment variable, assign the evaluated expression result to it
                            if (loopNode.assignment.isPresent()) {
                                InterpreterDataType loopConditionValue = evaluate(locals, object, loopNode.expression);
                                locals.put(loopNode.assignment.get().name, loopConditionValue);
                            }

                            // Execute the loop body
                            interpretStatementBlock(object, loopNode.statements, locals);
                        }
                    }
                }
            }
        }

        /**
         * evaluate() processes everything that is an expression - math, variables, boolean expressions.
         * There is a good bit of recursion in here, since math and comparisons have left and right sides that need to be evaluated.
         * <p>
         * See the How To Write an Interpreter document for examples
         * For each possible ExpressionNode, do the work to resolve it:
         * BooleanLiteralNode - create a new BooleanLiteralNode with the same value
         * - Same for all of the basic data types
         * BooleanOpNode - Evaluate() left and right, then perform either and/or on the results.
         * CompareNode - Evaluate() both sides. Do good comparison for each data type
         * MathOpNode - Evaluate() both sides. If they are both numbers, do the math using the built-in operators. Also handle String + String as concatenation (like Java)
         * MethodCallExpression - call doMethodCall() and return the first value
         * VariableReferenceNode - call findVariable()
         *
         * @param locals     the local variables
         * @param object     - the current object we are running
         * @param expression - some expression to evaluate
         * @return a value
         */
        private InterpreterDataType evaluate(HashMap<String, InterpreterDataType> locals, Optional<ObjectIDT> object, ExpressionNode expression) {

            if (expression instanceof BooleanLiteralNode) {
                return new BooleanIDT(((BooleanLiteralNode) expression).value);
            }

            if (expression instanceof NumericLiteralNode) {
                return new NumberIDT(((NumericLiteralNode) expression).value);
            }

            if (expression instanceof StringLiteralNode) {
                return new StringIDT(((StringLiteralNode) expression).value);
            }

            // Boolean Operation Node
            if (expression instanceof BooleanOpNode) {
                BooleanOpNode boolOpNode = (BooleanOpNode) expression;
                InterpreterDataType left = evaluate(locals, object, boolOpNode.left);
                InterpreterDataType right = evaluate(locals, object, boolOpNode.right);

                if (left instanceof BooleanIDT && right instanceof BooleanIDT) {
                    boolean leftValue = ((BooleanIDT) left).Value;
                    boolean rightValue = ((BooleanIDT) right).Value;
                    if (boolOpNode.op == BooleanOpNode.BooleanOperations.and) {
                        return new BooleanIDT(leftValue && rightValue);
                    } else if (boolOpNode.op == BooleanOpNode.BooleanOperations.or) {
                        return new BooleanIDT(leftValue || rightValue);
                    }
                }
                throw new RuntimeException("Boolean operation on non-boolean types");
            }

            // Comparison Node
            if (expression instanceof CompareNode) {
                CompareNode compareNode = (CompareNode) expression;
                InterpreterDataType left = evaluate(locals, object, compareNode.left);
                InterpreterDataType right = evaluate(locals, object, compareNode.right);

                if (left instanceof NumberIDT && right instanceof NumberIDT) {
                    double leftValue = ((NumberIDT) left).Value;
                    double rightValue = ((NumberIDT) right).Value;
                    switch (compareNode.op) {
                        case eq:
                            return new BooleanIDT(leftValue == rightValue);
                        case ne:
                            return new BooleanIDT(leftValue != rightValue);
                        case lt:
                            return new BooleanIDT(leftValue < rightValue);
                        case gt:
                            return new BooleanIDT(leftValue > rightValue);
                        case le:
                            return new BooleanIDT(leftValue <= rightValue);
                        case ge:
                            return new BooleanIDT(leftValue >= rightValue);
                        default:
                            throw new RuntimeException("Unsupported comparison operator: " + compareNode.op);
                    }
                }
                throw new RuntimeException("Comparison on non-comparable types");
            }

            // Math Op Node
            if (expression instanceof MathOpNode) {
                MathOpNode mathOpNode = (MathOpNode) expression;
                InterpreterDataType left = evaluate(locals, object, mathOpNode.left);
                InterpreterDataType right = evaluate(locals, object, mathOpNode.right);

                if (left instanceof NumberIDT && right instanceof NumberIDT) {
                    float leftValue = ((NumberIDT) left).Value;
                    float rightValue = ((NumberIDT) right).Value;

                    switch (mathOpNode.op) {
                        case add:
                            return new NumberIDT(leftValue + rightValue);
                        case subtract:
                            return new NumberIDT(leftValue - rightValue);
                        case multiply:
                            return new NumberIDT(leftValue * rightValue);
                        case divide:
                            return new NumberIDT(leftValue / rightValue);
                        default:
                            throw new RuntimeException("Unsupported math operator: " + mathOpNode.op);
                    }
                }
                throw new RuntimeException("Math operation on non-number types");
            }

            // Method Call Expression Node
            if (expression instanceof MethodCallExpressionNode) {
                MethodCallExpressionNode methodCallNode = (MethodCallExpressionNode) expression;

                // Prepare the parameters for the method call
                List<InterpreterDataType> parameters = new LinkedList<>();
                for (ExpressionNode parameter : methodCallNode.parameters) {
                    InterpreterDataType value = evaluate(locals, object, parameter);
                    parameters.add(value);
                }

                // Find the method declaration node (if needed)
                Optional<MethodDeclarationNode> methodDecNodeOpt = getClassByName(object.get().astNode.name)
                        .flatMap(classNode -> classNode.methods.stream()
                                .filter(m -> m.name.equals(methodCallNode.methodName))
                                .findFirst());

                if (methodDecNodeOpt.isPresent()) {
                    MethodDeclarationNode methodDecNode = methodDecNodeOpt.get();
                    List<InterpreterDataType> results = interpretMethodCall(object, methodDecNode, parameters);

                    // Return the first result
                    return results.isEmpty() ? null : results.get(0);
                }

                throw new RuntimeException("Method not found: " + methodCallNode.methodName);
            }
            if (expression instanceof VariableReferenceNode) {
                VariableReferenceNode varRefNode = (VariableReferenceNode) expression;
                String varName = varRefNode.name;

                if(varRefNode.name == null){
                    return null;
                }

                // Check local variables
                if (locals.containsKey(varName)) {
                    return locals.get(varName);
                }

                if(object.get().members.containsKey(varName)) {
                    return object.get().members.get(varName);
                }

                // Check object members
                if (object.isPresent() && object.get().members.containsKey(varName)) {
                    return object.get().members.get(varName);
                }


                // Variable not found
                throw new RuntimeException("Variable not found: " + varName);
            }
            if (expression instanceof NewNode) {
                NewNode newNode = (NewNode) expression;


                // Step 1: Retrieve the class definition by name
                Optional<ClassNode> classNodeOpt = getClassByName(newNode.className);
                if (classNodeOpt.isEmpty()) {
                    throw new RuntimeException("Class not found: " + newNode.className);
                }
                ClassNode classNode = classNodeOpt.get();

                ObjectIDT newObject = new ObjectIDT(classNode);


                for (MemberNode member : classNode.members) {
                    String memberName = member.declaration.name;
                    InterpreterDataType memberValue;

                    // Attempt to initialize from locals or the current object
                    if (locals.containsKey(memberName)) {
                        memberValue = locals.get(memberName);
                    } else if (object.isPresent() && object.get().members.containsKey(memberName)) {
                        memberValue = object.get().members.get(memberName);
                    } else {
                        // Use a default value or throw an error if uninitialized
                        memberValue = instantiate(member.declaration.type);
                    }
                    newObject.members.put(memberName, memberValue);
                }
                // Step 3: Invoke the constructor, if one exists
                Optional<ConstructorNode> constructorOpt = classNode.constructors.stream()
                        .filter(c -> c.parameters.size() == newNode.parameters.size())
                        .findFirst();
                if (constructorOpt.isPresent()) {
                    ConstructorNode constructor = constructorOpt.get();
                    List<InterpreterDataType> paramValues = new ArrayList<>();
                    for (ExpressionNode param : newNode.parameters) {
                        paramValues.add(evaluate(locals, object, param));
                    }
                    interpretConstructorCall(newObject, constructor, paramValues);
                }

                Optional<MethodDeclarationNode>methodDeclarationNode= classNode.methods.stream()
                        .filter(c->c.parameters.size()==newNode.parameters.size()).findFirst();
                if(methodDeclarationNode.isPresent()){
                    MethodDeclarationNode methodDecNode = methodDeclarationNode.get();
                    List<InterpreterDataType> paramValues = new ArrayList<>();
                    for (ExpressionNode param : newNode.parameters) {
                        paramValues.add(evaluate(locals, object, param));
                    }
                    interpretMethodCall(Optional.of(newObject), methodDecNode, paramValues);
                }

                // Return the newly created object
                return newObject;

            }

            throw new IllegalArgumentException("Unsupported ExpressionNode type: " + expression.getClass().getSimpleName());
        }
        //              Utility Methods

        /**
         * Used when trying to find a match to a method call. Given a method declaration, does it match this methoc call?
         * We double check with the parameters, too, although in theory JUST checking the declaration to the call should be enough.
         * <p>
         * Match names, parameter counts (both declared count vs method call and declared count vs value list), return counts.
         * If all of those match, consider the types (use TypeMatchToIDT).
         * If everything is OK, return true, else return false.
         * Note - if m is a built-in and isVariadic is true, skip all of the parameter validation.
         *
         * @param m          - the method declaration we are considering
         * @param mc         - the method call we are trying to match
         * @param parameters - the parameter values for this method call
         * @return does this method match the method call?
         */
        private boolean doesMatch(MethodDeclarationNode m, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
            // Check if method names match
            if (!m.name.equals(mc.methodName)) {
                return false;
            }

            // Check if the number of parameters match
            if (m.parameters.size() != parameters.size()) {
                return false;
            }

            // Check if each parameter type matches
            for (int i = 0; i < m.parameters.size(); i++) {
                VariableDeclarationNode paramNode = m.parameters.get(i);
                InterpreterDataType paramType = parameters.get(i);


                String paramTypeString = paramNode.type;

                // Check the class of paramType and compare with paramTypeString
                if (paramTypeString.equals("int") && !(paramType instanceof NumberIDT)) {
                    return false;
                } else if (paramTypeString.equals("String") && !(paramType instanceof StringIDT)) {
                    return false;
                } else if (paramTypeString.equals("boolean") && !(paramType instanceof BooleanIDT)) {
                    return false;
                }
            }

            return true;
        }

        /**
         * Very similar to DoesMatch() except simpler - there are no return values, the name will always match.
         *
         * @param c          - a particular constructor
         * @param mc         - the method call
         * @param parameters - the parameter values
         * @return does this constructor match the method call?
         */
        private boolean doesConstructorMatch(ConstructorNode c, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
            // Check if the number of parameters matches
            if (c.parameters.size() != parameters.size()) {
                return false;
            }

            // Check if each parameter type matches
            for (int i = 0; i < parameters.size(); i++) {
                String expectedType = c.parameters.get(i).type;
                InterpreterDataType actualValue = parameters.get(i);
                if (!typeMatchToIDT(expectedType, actualValue)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Used when we call a method to get the list of values for the parameters.
         * <p>
         * for each parameter in the method call, call Evaluate() on the parameter to get an IDT and add it to a list
         *
         * @param object - the current object
         * @param locals - the local variables
         * @param mc     - a method call
         * @return the list of method values
         */
        private List<InterpreterDataType> getParameters(Optional<ObjectIDT> object, HashMap<String, InterpreterDataType> locals, MethodCallStatementNode mc) {
            List<InterpreterDataType> parameterValues = new LinkedList<InterpreterDataType>();

            for (ExpressionNode parameter : mc.parameters) {
                // Evaluate the parameter expression to get an InterpreterDataType
                if(parameter== null){
                    return parameterValues;
                }
                InterpreterDataType value = evaluate(locals, object, parameter);
                parameterValues.add(value);
            }

            return parameterValues;
        }

        /**
         * Used when we have an IDT and we want to see if it matches a type definition
         * Commonly, when someone is making a function call - do the parameter values match the method declaration?
         * <p>
         * If the IDT is a simple type (boolean, number, etc) - does the string type match the name of that IDT ("boolean", etc)
         * If the IDT is an object, check to see if the name matches OR the class has an interface that matches
         * If the IDT is a reference, check the inner (refered to) type
         *
         * @param type the name of a data type (parameter to a method)
         * @param idt  the IDT someone is trying to pass to this method
         * @return is this OK?
         */
        private boolean typeMatchToIDT(String type, InterpreterDataType idt) {
            // Match simple types
            if (type.equals("boolean") && idt instanceof BooleanIDT) {
                return true;
            } else if (type.equals("number") && idt instanceof NumberIDT) {
                return true;
            } else if (type.equals("String") && idt instanceof StringIDT) {
                return true;
            }

            // Match object types
            if (idt instanceof ObjectIDT) {
                ObjectIDT objectIDT = (ObjectIDT) idt;
                if (objectIDT.astNode != null && objectIDT.astNode.getClass().getSimpleName().equals(type)) {
                    return true;
                }
            }

            // Handle references
            if (idt instanceof ReferenceIDT) {
                ReferenceIDT referenceIDT = (ReferenceIDT) idt;
                return typeMatchToIDT(type, referenceIDT.refersTo.get());
            }
            throw new RuntimeException("Unable to resolve type " + type);
        }

        /**
         * Find a method in an object that is the right match for a method call (same name, parameters match, etc. Uses doesMatch() to do most of the work)
         * <p>
         * Given a method call, we want to loop over the methods for that class, looking for a method that matches (use DoesMatch) or throw
         *
         * @param object     - an object that we want to find a method on
         * @param mc         - the method call
         * @param parameters - the parameter value list
         * @return a method or throws an exception
         */
        private MethodDeclarationNode getMethodFromObject(ObjectIDT object, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
            // Retrieve the class associated with the current object
            Optional<ClassNode> classNodeOpt = getClassByName(object.astNode.name);

            if (classNodeOpt.isPresent()) {
                ClassNode classNode = classNodeOpt.get();
                // Loop through the methods in the class and check for a match
                for (MethodDeclarationNode method : classNode.methods) {
                    if (doesMatch(method, mc, parameters)) {
                        return method;
                    }
                }
            }

            throw new RuntimeException("Unable to resolve method call " + mc);
        }

        /**
         * Find a class, given the name. Just loops over the TranNode's classes member, matching by name.
         * <p>
         * Loop over each class in the top node, comparing names to find a match.
         *
         * @param name Name of the class to find
         * @return either a class node or empty if that class doesn't exist
         */
        private Optional<ClassNode> getClassByName(String name) {
            // Assuming tranNode holds the top-level node that contains the list of classes.
            for (ClassNode classNode : top.Classes) {
                if (classNode.name.equals(name)) {
                    return Optional.of(classNode);
                }
            }
            return Optional.empty();
        }

        /**
         * Given an execution environment (the current object, the current local variables), find a variable by name.
         *
         * @param name   - the variable that we are looking for
         * @param locals - the current method's local variables
         * @param object - the current object (so we can find members)
         * @return the IDT that we are looking for or throw an exception
         */
        private InterpreterDataType findVariable(String name, HashMap<String, InterpreterDataType> locals, Optional<ObjectIDT> object) {
            // Check locals first
            // Check if the variable exists in the locals
            if (locals.containsKey(name)) {
                return locals.get(name);
            }

            // Check if the variable exists in the object's members
            if (object.isPresent() && object.get().members.containsKey(name)) {
                return object.get().members.get(name);
            }
            throw new RuntimeException("Unable to find variable " + name);
        }

        /**
         * Given a string (the type name), make an IDT for it.
         *
         * @param type The name of the type (string, number, boolean, character). Defaults to ReferenceIDT if not one of those.
         * @return an IDT with default values (0 for number, "" for string, false for boolean, ' ' for character)
         */
        private InterpreterDataType instantiate(String type) {
            switch (type.toLowerCase()) {
                case "number":
                    return new NumberIDT(0); //  numeric values
                case "string":
                    return new StringIDT(""); //  strings
                case "boolean":
                    return new BooleanIDT(false); //  boolean values
                case "char":
                    return new CharIDT(' '); // characters
                default:
                    return new ReferenceIDT(); // unsupported types
            }
        }
    }
