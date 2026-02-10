import AST.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Parser {
    private final TokenManager tokenManager;
    private final TranNode tranNode;

    public Parser(TranNode top, List<Token> tokens) {
        this.tranNode = top;
        this.tokenManager = new TokenManager(tokens);
    }

    // Tran = { Class | Interface }
    public void Tran() throws SyntaxErrorException {
        while (!tokenManager.done()){
            //make sure first two tokens are class and word
            if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.CLASS, Token.TokenTypes.WORD) ){
                Optional<ClassNode> classNode= Class();
                tranNode.Classes.add(classNode.get());
                //make sure first two tokens are interface and word
            }else if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.INTERFACE, Token.TokenTypes.WORD)){
                System.out.println("you made it to call interface");
                //call parse interface
                InterfaceNode interfaceNode = Interface()//input to AST
                        .orElseThrow(() -> new SyntaxErrorException("Failed to create InterfaceNode",
                                tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
                tranNode.Interfaces.add(interfaceNode);
            }else{
                throw new SyntaxErrorException("Expected class or interface"
                        ,tokenManager.getCurrentLine()
                        , tokenManager.getCurrentColumnNumber());
            }
        }
    }
    private Optional<ClassNode> Class() throws SyntaxErrorException {
        tokenManager.matchAndRemove(Token.TokenTypes.CLASS)//token is class
                .orElseThrow(()->new SyntaxErrorException("Expected 'class' keyword",
                    tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

            Token className = tokenManager.matchAndRemove(Token.TokenTypes.WORD)//token is word
                    .orElseThrow(() -> new SyntaxErrorException("Expected class name",
                            tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

            ClassNode classNode = new ClassNode();//new class node
            classNode.name = className.getValue();//set class name

        if(!tokenManager.done() && tokenManager.peek(0).isPresent()&&
                tokenManager.peek(0).get().getType() == Token.TokenTypes.IMPLEMENTS) {//case of implements
            tokenManager.matchAndRemove(Token.TokenTypes.IMPLEMENTS).orElseThrow(()
                    -> new SyntaxErrorException("Expected Implements",
                    tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

            Token implementname =tokenManager.matchAndRemove(Token.TokenTypes.WORD).orElseThrow(()
                    -> new SyntaxErrorException("Expected word",
                    tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));


                classNode.interfaces.add(implementname.getValue());//add to AST

            if(!tokenManager.done() && tokenManager.peek(0).isPresent()&&
                    tokenManager.peek(0).get().getType() == Token.TokenTypes.COMMA){//comma means implememnt this

                tokenManager.matchAndRemove(Token.TokenTypes.COMMA).orElseThrow(()
                        -> new SyntaxErrorException("Expected COMMA",
                        tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

                Token twoimplement =tokenManager.matchAndRemove(Token.TokenTypes.WORD).orElseThrow(()
                        -> new SyntaxErrorException("Expected word",
                        tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

                classNode.interfaces.add(twoimplement.getValue());
                RequireNewLine();

                if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) {
                    throw new SyntaxErrorException("Expected 'indent here' keyword",
                            tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }
                while(!tokenManager.done()&& tokenManager.peek(0).isPresent()&&
                        tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD) {
                    if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)){//if this than method dec
                        break;
                    }
                    classNode.members.add(member().get());//members like vairable dec
                    RequireNewLine();

                }
            }
            if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)){
                tokenManager.matchAndRemove(Token.TokenTypes.WORD)//expect method name
                        .orElseThrow(() -> new SyntaxErrorException("Expected method name at instead got " + tokenManager.peek(0).get().getType(), tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

                tokenManager.matchAndRemove(Token.TokenTypes.LPAREN)//expect LPAREN
                        .orElseThrow(() -> new SyntaxErrorException("Expected '(' after method name at instead got "+ tokenManager.peek(0).get().getType(), tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

                tokenManager.matchAndRemove(Token.TokenTypes.RPAREN)//expect RPAREN
                        .orElseThrow(() -> new SyntaxErrorException("Expected ')' to close method parameters at ", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

            }

        }

        if(!tokenManager.done() && tokenManager.peek(0).isPresent()//recursively calls
                && tokenManager.peek(0).get().getType() != Token.TokenTypes.DEDENT) {

            RequireNewLine();

            if (tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.INDENT) {
                tokenManager.matchAndRemove(Token.TokenTypes.INDENT);
            }
            if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.SHARED){
                    classNode.methods.add(methodDec().get());
                    RequireNewLine();
                while(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)) {
                    classNode.methods.add(methodDec().get());
                    RequireNewLine();
                }
            }

            while (!tokenManager.done() && tokenManager.peek(0).isPresent()//recursively calls
                    && tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD) {
                    if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)){
                        break;
                    }
                classNode.members.add(member().get());//more space for members
                RequireNewLine();
            }
           while(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)) {
               classNode.methods.add(methodDec().get());//methode dec
               RequireNewLine();
           }

            if(!tokenManager.done() && tokenManager.peek(0).isPresent()
            && tokenManager.peek(0).get().getType() == Token.TokenTypes.CONSTRUCT) {//case of constructor
                classNode.constructors.add(constructor().get());
            }
            if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)){
                classNode.methods.add(methodDec().get());//methode dec
                RequireNewLine();
            }
            if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.SHARED, Token.TokenTypes.WORD)) {
                classNode.methods.add(methodDec().get());
                RequireNewLine();
            }
        }
        while(!tokenManager.done() && tokenManager.peek(0).isPresent()&&
                tokenManager.peek(0).get().getType() == Token.TokenTypes.DEDENT) {
            if (tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()) {
                throw new SyntaxErrorException("Expected 'dedent y' keyword",
                        tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
        }
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.NEWLINE) {
            if (tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE).isEmpty()) {
                throw new SyntaxErrorException("Expected 'NEWLINE' keyword",
                        tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
        }
        //save token name and token
        return Optional.of(classNode);
    }
    private Optional<InterfaceNode> Interface() throws SyntaxErrorException {
        System.out.println("YOU MADE IT TO INTERFACE");//reaffirm token is interface
        if (tokenManager.matchAndRemove(Token.TokenTypes.INTERFACE).isEmpty()) {
                throw new SyntaxErrorException("Expected 'interface' keyword",
                        tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }//save token name and token
        Token interfaceName = tokenManager.matchAndRemove(Token.TokenTypes.WORD)
                    .orElseThrow(() -> new SyntaxErrorException("Expected interface name",
                            tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

        InterfaceNode interfaceNode = new InterfaceNode();
        interfaceNode.name = interfaceName.getValue();


        RequireNewLine();//new line
            //needs indent or error
        if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) {
                throw new SyntaxErrorException("Expected 'indent' keyword",
                        tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }//if were not dealing with the end were going into a method
        while (!tokenManager.done() && tokenManager.peek(0).isPresent()//recursively calls
                && tokenManager.peek(0).get().getType() != Token.TokenTypes.DEDENT) {
            Optional<MethodHeaderNode> methodHeader = MethodHeader();
            interfaceNode.methods.add(methodHeader.get());// Parses a method header
        }
        //after our method and what ever is in method we should expect end
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.DEDENT) {
            if (tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()) {
                throw new SyntaxErrorException("Expected 'dedent' keyword",
                        tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
        }
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.NEWLINE) {
            if (tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE).isEmpty()) {
                throw new SyntaxErrorException("Expected 'NEWLINE' keyword",
                        tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
        }
        return Optional.of(interfaceNode);
    }
        //new line acts as our end of current function or thing like ;
    private void RequireNewLine() throws SyntaxErrorException{
        while(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType()
        == Token.TokenTypes.NEWLINE) {
           if( tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE).isEmpty()){
               throw new SyntaxErrorException("expected new line required insteadd got"+
                       tokenManager.peek(0).get().getType(), tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
           }
        }
    }//our method after our interface or class
    private Optional<MethodHeaderNode> MethodHeader() throws SyntaxErrorException {
        Token name = tokenManager.matchAndRemove(Token.TokenTypes.WORD)//expect method name
                .orElseThrow(() -> new SyntaxErrorException("Expected method name at instead got " + tokenManager.peek(0).get().getType(), tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

        tokenManager.matchAndRemove(Token.TokenTypes.LPAREN)//expect LPAREN
                .orElseThrow(() -> new SyntaxErrorException("Expected '(' after method name at instead got "+ tokenManager.peek(0).get().getType(), tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

        tokenManager.matchAndRemove(Token.TokenTypes.RPAREN)//expect RPAREN
                .orElseThrow(() -> new SyntaxErrorException("Expected ')' to close method parameters at ", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

        Token returnMethod = null;
        MethodHeaderNode methodHeaderNode = new MethodHeaderNode();
        methodHeaderNode.name = name.getValue();//add name to ast

        if (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.COLON) {
            tokenManager.matchAndRemove(Token.TokenTypes.COLON);  // Match the colon
            methodHeaderNode.returns.add(variableDeclaration().get());
            RequireNewLine();//colon means were going to declare a variable
        }

            //not at end
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() != Token.TokenTypes.DEDENT) {
            RequireNewLine();
            if(tokenManager.peek(0).get().getType() ==Token.TokenTypes.INDENT) {//if indent match indent
                if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) {
                    throw new SyntaxErrorException("Expected 'indent' keyword",
                            tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }
            }
        }

        return Optional.of(methodHeaderNode);
    }
    //declare variable with loop for multiple
    private Optional<List<VariableDeclarationNode>> VariableDeclarations() throws SyntaxErrorException {
        List<VariableDeclarationNode> variableDeclarationNodes = new ArrayList<>();//list of var dec

            // Add the new variable to the list
        while(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD,Token.TokenTypes.WORD)) {
            variableDeclarationNodes.add(variableDeclaration().get());//add statetement to list
        }

        return Optional.of(variableDeclarationNodes);
    }
    //single variable definition
    private Optional<VariableDeclarationNode> variableDeclaration() throws SyntaxErrorException {
        Token name =null;
        Token type =null;
        VariableDeclarationNode variableDeclarationNode = new VariableDeclarationNode();
         type = tokenManager.matchAndRemove(Token.TokenTypes.WORD).//type of vairable
                orElseThrow(() -> new SyntaxErrorException("Expected variable at ", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

         name = tokenManager.matchAndRemove(Token.TokenTypes.WORD).//variable name
                orElseThrow(() -> new SyntaxErrorException("Expected variable name at ", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

        variableDeclarationNode.name = name.getValue();//save in ast
        variableDeclarationNode.type = type.getValue();//save in ast
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.COMMA) {
            tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
        }

        RequireNewLine();


        return Optional.of(variableDeclarationNode);
    }
    private Optional<ConstructorNode> constructor() throws SyntaxErrorException {
        tokenManager.matchAndRemove(Token.TokenTypes.CONSTRUCT).orElseThrow(()//match constructor
                -> new SyntaxErrorException("Expected Construct",
                tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

        ClassNode classNode = new ClassNode();

        tokenManager.matchAndRemove(Token.TokenTypes.LPAREN)//expect LPAREN
                .orElseThrow(() -> new SyntaxErrorException("Expected '(' after construct at instead got "+ tokenManager.peek(0).get().getType(), tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

        tokenManager.matchAndRemove(Token.TokenTypes.RPAREN)//expect RPAREN
                .orElseThrow(() -> new SyntaxErrorException("Expected ')' to close at ", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
        ConstructorNode constructorNode = new ConstructorNode();


        classNode.constructors.add(constructorNode);//populate class constructor
        RequireNewLine();

        tokenManager.matchAndRemove(Token.TokenTypes.INDENT)//expect LPAREN
                .orElseThrow(() -> new SyntaxErrorException("Expected Indent ", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

        constructorNode.statements.addAll(statements().get());

        if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.DEDENT) {
            tokenManager.matchAndRemove(Token.TokenTypes.DEDENT);
        }
        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD,Token.TokenTypes.WORD)) {
            constructorNode.locals.add(variableDeclaration().get());
        }

        return Optional.of(constructorNode);//return out
    }
    private Optional<List<StatementNode>> statements() throws SyntaxErrorException {
        List<StatementNode> statementNodes = new ArrayList<>();//list of statements
        StatementNode statement = null;

        Token name=null;
        Token num = null;

        while(!tokenManager.done() && tokenManager.peek(0).isPresent()//recursively calls
                && tokenManager.peek(0).get().getType() != Token.TokenTypes.DEDENT){
            if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD,Token.TokenTypes.LPAREN)) {
                break;
            }
            if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.SHARED,Token.TokenTypes.WORD)) {
                break;
            }
            statementNodes.add(statement().get());//add call statement to list
            RequireNewLine();
        }

        return Optional.of(statementNodes);
    }
    private Optional<StatementNode> statement() throws SyntaxErrorException {
        StatementNode statementNode = null;
        Token num =null;
        AssignmentNode assignmentNode = new AssignmentNode();
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.LOOP){
             return loop();//in case of loop
        }
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.IF){
            return parseIf();//in case of if
        }
            // look between assignment and method call
          return  disambiguate();
    }
    private Optional<MemberNode> member() throws SyntaxErrorException {

        MemberNode memberNode = new MemberNode();
        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)){//variable dec for member
        memberNode.declaration = variableDeclaration().get();//var dec member
        RequireNewLine();
        }
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.INDENT){
            tokenManager.matchAndRemove(Token.TokenTypes.INDENT);
        }

        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.ACCESSOR, Token.TokenTypes.COLON)){//accesor then ...
            tokenManager.matchAndRemove(Token.TokenTypes.ACCESSOR);
            tokenManager.matchAndRemove(Token.TokenTypes.COLON);
        }
        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.MUTATOR, Token.TokenTypes.COLON)){//mutator then .....
            tokenManager.matchAndRemove(Token.TokenTypes.MUTATOR);
            tokenManager.matchAndRemove(Token.TokenTypes.COLON);
        }
        return Optional.of(memberNode);
    }


    private Optional<MethodDeclarationNode> methodDec () throws SyntaxErrorException {
            MethodDeclarationNode methodDeclarationNode = new MethodDeclarationNode();
            if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.SHARED){
                tokenManager.matchAndRemove(Token.TokenTypes.SHARED);
                methodDeclarationNode.isShared = true;
            }

                if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)){//method dec like blahblah()
                    Token methodname = null;
                    methodname =tokenManager.matchAndRemove(Token.TokenTypes.WORD)
                            .orElseThrow(() -> new SyntaxErrorException("Expected method name ", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
                    methodDeclarationNode.name = methodname.getValue();
                    tokenManager.matchAndRemove(Token.TokenTypes.LPAREN);
                    tokenManager.matchAndRemove(Token.TokenTypes.RPAREN);
                    if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.COLON) {
                        tokenManager.matchAndRemove(Token.TokenTypes.COLON);
                            methodDeclarationNode.returns.addAll(VariableDeclarations().get());
                    }
                    RequireNewLine();
                   if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.INDENT){
                       tokenManager.matchAndRemove(Token.TokenTypes.INDENT);
                   }

                    while(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)){//locals are variables after method dec
                        methodDeclarationNode.locals.addAll(VariableDeclarations().get());
                        RequireNewLine();
                    }

                    methodDeclarationNode.statements.addAll(statements().get());

                    if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.DEDENT) {
                        tokenManager.matchAndRemove(Token.TokenTypes.DEDENT);
                    }
                }
                while(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)){//locals are variables after method dec
                    methodDeclarationNode.locals.add(variableDeclaration().get());//local var dec
                    RequireNewLine();
                }

        return Optional.of(methodDeclarationNode);
    }
    private Optional<StatementNode> loop() throws SyntaxErrorException {
        LoopNode loopNode = new LoopNode();//new loop
        tokenManager.matchAndRemove(Token.TokenTypes.LOOP);//match loop
            //some more to be added this is to enter lop
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD) {
            loopNode.assignment= Optional.of(variableref().get());//assignment in loop
        }
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.ASSIGN){
            tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN);
        }

        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.DOT)){
            loopNode.expression = methodcallexpression().get();
        }
        RequireNewLine();
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.INDENT) {
            tokenManager.matchAndRemove(Token.TokenTypes.INDENT);
        }//statement in loop
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.IF){
            loopNode.statements.addAll(statements().get());
        }
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD) {
            loopNode.statements.addAll(statements().get());
        }


        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.ELSE){
            tokenManager.matchAndRemove(Token.TokenTypes.ELSE);
        }
        return Optional.of(loopNode);
    }
    private Optional<StatementNode> parseIf() throws SyntaxErrorException {
        IfNode ifNode = new IfNode();
        ifNode.statements = new ArrayList<>();
        ifNode.elseStatement=Optional.empty();

        List<ExpressionNode> expressionNodes = new ArrayList<>();

        tokenManager.matchAndRemove(Token.TokenTypes.IF);
        if(tokenManager.peek(3).isPresent()&& tokenManager.peek(3).get().getType() == Token.TokenTypes.NEWLINE){
            ifNode.condition= BoolExpFactor().get();
        }

        while(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() != Token.TokenTypes.NEWLINE) {

            ifNode.condition =BoolExpTerm().get();//fill condintion with things like n>b
        }

        RequireNewLine();
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.INDENT){
            tokenManager.matchAndRemove(Token.TokenTypes.INDENT);
        }
        ifNode.statements.addAll(statements().get());//statements after condition
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.DEDENT){
            tokenManager.matchAndRemove(Token.TokenTypes.DEDENT);
        }
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.ELSE){
            ElseNode elseNode = new ElseNode();
            tokenManager.matchAndRemove(Token.TokenTypes.ELSE);
            RequireNewLine();
            tokenManager.matchAndRemove(Token.TokenTypes.INDENT);
            elseNode.statements = statements().get();
            ifNode.elseStatement = Optional.of(elseNode);
        }

        return Optional.of(ifNode);
    }

    Optional<StatementNode> disambiguate()throws SyntaxErrorException {
        StatementNode statementNode = null;
        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.ASSIGN)){
            AssignmentNode assignmentNode = new AssignmentNode();
            if(tokenManager.peek(3).isPresent() && tokenManager.peek(3).get().getType() == Token.TokenTypes.LPAREN){
                return methodcall();//to get difference in normal and method call
            }
            return parseassignment();
        }
        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.COMMA)){
             return methodcall();//mutiple variable method call
        }
        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.DOT)){
            return methodcall();//for stuff like a.method()
        }

        return Optional.empty();
    }
    private Optional<ExpressionNode>BoolExpTerm()throws SyntaxErrorException{
      BooleanOpNode booleanOpNode = new BooleanOpNode();//instance of bool op
      BooleanOpNode booleanOpNode2 = new BooleanOpNode();//incase of more than 2 conditions
        booleanOpNode.op =null;
        booleanOpNode.left=BoolExpFactor().get();//fill left before AND or OR



        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.AND){
            tokenManager.matchAndRemove(Token.TokenTypes.AND);
            booleanOpNode.op = BooleanOpNode.BooleanOperations.and;//fill ast with and
        }
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.OR){
            tokenManager.matchAndRemove(Token.TokenTypes.OR);
            booleanOpNode.op = BooleanOpNode.BooleanOperations.or;//fil ast with or
        }

        booleanOpNode.right=BoolExpFactor().get();//fill right after AND or OR

        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.AND||
                tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.OR){

            booleanOpNode2.left = booleanOpNode;//fill new instance with whole old bool op instance

            if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.AND){
                tokenManager.matchAndRemove(Token.TokenTypes.AND);
                booleanOpNode2.op = BooleanOpNode.BooleanOperations.and;//add and to AST
            }
            if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.OR){
                tokenManager.matchAndRemove(Token.TokenTypes.OR);
                booleanOpNode2.op = BooleanOpNode.BooleanOperations.or;//add or to AST
            }

            booleanOpNode2.right=BoolExpFactor().get();//fill right
                return Optional.of(booleanOpNode2);
        }
        return Optional.of(booleanOpNode);
    }
    private Optional<ExpressionNode>BoolExpFactor()throws SyntaxErrorException{

        CompareNode compareNode = new CompareNode();

        compareNode.left = variableref().get();//get letter
            //in cases of comparisions or not equal or equal
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.LESSTHAN){
            tokenManager.matchAndRemove(Token.TokenTypes.LESSTHAN);
           compareNode.op = CompareNode.CompareOperations.lt;//Filling op with neccesary comparision
        }
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.GREATERTHAN){
            tokenManager.matchAndRemove(Token.TokenTypes.GREATERTHAN);
            compareNode.op = CompareNode.CompareOperations.gt;
        }
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.LESSTHANEQUAL){
            tokenManager.matchAndRemove(Token.TokenTypes.LESSTHANEQUAL);
            compareNode.op = CompareNode.CompareOperations.le;
        }
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.GREATERTHANEQUAL){
            tokenManager.matchAndRemove(Token.TokenTypes.GREATERTHANEQUAL);
            compareNode.op = CompareNode.CompareOperations.ge;
        }
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.NOTEQUAL){
            tokenManager.matchAndRemove(Token.TokenTypes.NOTEQUAL);
            compareNode.op = CompareNode.CompareOperations.ne;
        }
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.EQUAL){
            tokenManager.matchAndRemove(Token.TokenTypes.EQUAL);
            compareNode.op = CompareNode.CompareOperations.eq;
        }

        compareNode.right = variableref().get();

        return Optional.of(compareNode);
    }

    private Optional<StatementNode> methodcall() throws SyntaxErrorException {
        MethodCallStatementNode methodCallNode = new MethodCallStatementNode();
        List<VariableReferenceNode> returns = new ArrayList<>();

        while(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.COMMA)){
            returns.add(variableref().get());// for a,b,c = method()
            tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
        }
        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.ASSIGN)) {
            if (tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD) {
                returns.add(variableref().get());//to get last of multi variable assign
            }
        }
        methodCallNode.returnValues = returns;
            if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.ASSIGN) {
                tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN);
            }
            //getting just method call like method() without using method expression
        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)){
            Token methodname =null;
            methodname = tokenManager.matchAndRemove(Token.TokenTypes.WORD).orElseThrow(()
                    -> new SyntaxErrorException("Expected methodname", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
            methodCallNode.methodName = methodname.getValue();
            tokenManager.matchAndRemove(Token.TokenTypes.LPAREN);
            tokenManager.matchAndRemove(Token.TokenTypes.RPAREN);
        }
        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.DOT)){
           Token objectname =null;//remove object name
           objectname = tokenManager.matchAndRemove(Token.TokenTypes.WORD).orElseThrow(()
                   -> new SyntaxErrorException("Expected objectname", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
           methodCallNode.objectName= Optional.of(objectname.getValue());

           tokenManager.matchAndRemove(Token.TokenTypes.DOT);
            //define method name string
            Token methodname =null;
            methodname = tokenManager.matchAndRemove(Token.TokenTypes.WORD).orElseThrow(()
                    -> new SyntaxErrorException("Expected methodname", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

            methodCallNode.methodName = methodname.getValue();
                //to get param(now)
            if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.LPAREN){
                tokenManager.matchAndRemove(Token.TokenTypes.LPAREN);
                methodCallNode.parameters.add(variableref().get());
                tokenManager.matchAndRemove(Token.TokenTypes.RPAREN);
            }
        }


       return Optional.of(methodCallNode);
    }
    private Optional<VariableReferenceNode> variableref() throws SyntaxErrorException {
        VariableReferenceNode variableReferenceNode = new VariableReferenceNode();
        Token ident = null;
        Token num= null;
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD) {
            ident = tokenManager.matchAndRemove(Token.TokenTypes.WORD).orElseThrow(()
                    -> new SyntaxErrorException("Expected identifier at ", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
            //take given letter and match and remove
            variableReferenceNode.name = ident.getValue();
        }
        if(tokenManager.peek(0).isPresent() && tokenManager.peek(0).get().getType() == Token.TokenTypes.NUMBER) {
            num = tokenManager.matchAndRemove(Token.TokenTypes.NUMBER).orElseThrow(()
                    -> new SyntaxErrorException("Expected identifier at ", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
            variableReferenceNode.name = num.getValue();
        }
         return Optional.of(variableReferenceNode);
    }

    private Optional<StatementNode> parseassignment() throws SyntaxErrorException {
        //in case of a=b
        AssignmentNode assignmentNode = new AssignmentNode();

        //get letter
        assignmentNode.target= variableref().get();
        tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN);

        if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.TRUE){
            assignmentNode.expression = expressionNode().get();
        }
        if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.FALSE){
            assignmentNode.expression = expressionNode().get();
        }
        if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.QUOTEDSTRING){
            assignmentNode.expression = expressionNode().get();
        }
        if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.NUMBER) {
            assignmentNode.expression = expressionNode().get();
        }
        if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.NEW){
            assignmentNode.expression = expressionNode().get();
        }

        //get letter
        if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD) {
            if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.NEWLINE)){
                assignmentNode.expression = variableref().get();
                return Optional.of(assignmentNode);
            }
            assignmentNode.expression = expressionNode().get();
        }

        return Optional.of(assignmentNode);
    }
    private Optional<ExpressionNode>  expressionNode() throws SyntaxErrorException {
            //special keywords
        if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.TRUE){
            return factor();
        }
        if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.FALSE){
            return factor();
        }
        if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.NEW){
            return factor();
        }
        if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.QUOTEDSTRING){
            return factor();
        }
        if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD){
            if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.PLUS)){
                MathOpNode mathOpNode = new MathOpNode();
                mathOpNode.left = variableref().get();
                mathOpNode.op =MathOpNode.MathOperations.add;
                tokenManager.matchAndRemove(Token.TokenTypes.PLUS);
                mathOpNode.right = variableref().get();
                return Optional.of(mathOpNode);
            }
            return factor();
        }
        if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.NUMBER) {
            if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.NUMBER, Token.TokenTypes.NEWLINE)){
                return factor();
            }
            MathOpNode mathOpNode = new MathOpNode();
            mathOpNode.left= factor().get();

            if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.PLUS){
                mathOpNode.op = MathOpNode.MathOperations.add;
                tokenManager.matchAndRemove(Token.TokenTypes.PLUS);
            }
            if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.MINUS){
                mathOpNode.op = MathOpNode.MathOperations.subtract;
                tokenManager.matchAndRemove(Token.TokenTypes.MINUS);
            }
            if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.TIMES){
                mathOpNode.op = MathOpNode.MathOperations.multiply;
                tokenManager.matchAndRemove(Token.TokenTypes.TIMES);
            }
            if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.DIVIDE){
                mathOpNode.op = MathOpNode.MathOperations.divide;
                tokenManager.matchAndRemove(Token.TokenTypes.DIVIDE);
            }
            //if case has parethesis
            if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.LPAREN){
                tokenManager.matchAndRemove(Token.TokenTypes.LPAREN);

                mathOpNode.right= expressionNode().get();

                tokenManager.matchAndRemove(Token.TokenTypes.RPAREN);
                //make new node if more space needed
                MathOpNode mathOpNode2 = new MathOpNode();
                mathOpNode2.left = mathOpNode;
                if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.PLUS){
                    mathOpNode2.op = MathOpNode.MathOperations.add;
                    tokenManager.matchAndRemove(Token.TokenTypes.PLUS);
                }
                if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.MINUS){
                    mathOpNode2.op = MathOpNode.MathOperations.subtract;
                    tokenManager.matchAndRemove(Token.TokenTypes.MINUS);
                }
                if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.TIMES){
                    mathOpNode2.op = MathOpNode.MathOperations.multiply;
                    tokenManager.matchAndRemove(Token.TokenTypes.TIMES);
                }
                if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.DIVIDE){
                    mathOpNode2.op = MathOpNode.MathOperations.divide;
                    tokenManager.matchAndRemove(Token.TokenTypes.DIVIDE);
                }

                mathOpNode2.right = factor().get();
                return Optional.of(mathOpNode2);
            }
            mathOpNode.right = factor().get();

            return Optional.of(mathOpNode);
        }

        return Optional.empty();
    }
    private Optional<ExpressionNode> factor() throws SyntaxErrorException {
            //fill ast and remove special keyword
        if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.TRUE){
            BooleanLiteralNode booleanLiteralNode = new BooleanLiteralNode(true);
            tokenManager.matchAndRemove(Token.TokenTypes.TRUE);
            return Optional.of(booleanLiteralNode);

        }
        if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.FALSE){
            BooleanLiteralNode booleanLiteralNode = new BooleanLiteralNode(false);
            tokenManager.matchAndRemove(Token.TokenTypes.FALSE);
            return Optional.of(booleanLiteralNode);

        }
        if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.NEW){
            NewNode newNode = new NewNode();
            Token classname = null;
           tokenManager.matchAndRemove(Token.TokenTypes.NEW);
           classname = tokenManager.matchAndRemove(Token.TokenTypes.WORD)//token is word
                   .orElseThrow(() -> new SyntaxErrorException("Expected classname",
                           tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
           newNode.className = classname.getValue();

           tokenManager.matchAndRemove(Token.TokenTypes.LPAREN);
           tokenManager.matchAndRemove(Token.TokenTypes.RPAREN);
           return Optional.of(newNode);
        }
        if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.QUOTEDSTRING){
            StringLiteralNode stringLiteralNode = new StringLiteralNode();

            Token sum = tokenManager.matchAndRemove(Token.TokenTypes.QUOTEDSTRING)//token is word
                    .orElseThrow(() -> new SyntaxErrorException("Expected quoted string",
                            tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

            stringLiteralNode.value = sum.getValue();
            return Optional.of(stringLiteralNode);
        }
        if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.NUMBER) {
            NumericLiteralNode numericLiteralNode = new NumericLiteralNode();
            Token num = tokenManager.matchAndRemove(Token.TokenTypes.NUMBER)//token is word
                    .orElseThrow(() -> new SyntaxErrorException("Expected quoted string",
                            tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

            numericLiteralNode.value =Float.parseFloat(num.getValue());
            return Optional.of(numericLiteralNode);


        }
        if(tokenManager.peek(0).isPresent()&& tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD){

        }
        return Optional.empty();
    }
    private Optional <ExpressionNode> methodcallexpression() throws SyntaxErrorException {
        MethodCallExpressionNode methodCallExpressionNode = new MethodCallExpressionNode();
        methodCallExpressionNode.objectName = Optional.empty();
        Token methodname = null;
            //using method call
        //for console.print
        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD,Token.TokenTypes.DOT)){
            Token objectname = null;
            objectname =tokenManager.matchAndRemove(Token.TokenTypes.WORD).orElseThrow(()
                    -> new SyntaxErrorException("Expected method name",tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));
            methodCallExpressionNode.objectName = Optional.of(objectname.getValue());

            tokenManager.matchAndRemove(Token.TokenTypes.DOT);
        }

        methodname = tokenManager.matchAndRemove(Token.TokenTypes.WORD).orElseThrow(()
                -> new SyntaxErrorException("Expected method name",tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber()));

        methodCallExpressionNode.methodName = methodname.getValue();

        tokenManager.matchAndRemove(Token.TokenTypes.LPAREN);
        tokenManager.matchAndRemove(Token.TokenTypes.RPAREN);

        return Optional.of(methodCallExpressionNode);

    }

}