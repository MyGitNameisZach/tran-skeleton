import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;

public class Lexer {
    private final TextManager textManager;//manages input text
    private final HashMap<Character, Token.TokenTypes> punctuationMap;
    private final HashMap<String, Token.TokenTypes> keywordsMap;
    LinkedList<Token> ListOfTokens = new LinkedList<>();
    private int previousIndent = 0;//holds indent for DEDENT cases
    private int lineNumber = 1;
    private int characterPosition = 1;
    private int currentIndentation = 0;

    public Lexer(String text) {
        this.textManager = new TextManager(text);
        this.keywordsMap = new HashMap<>();
        this.punctuationMap = new HashMap<>();
        initializePunctuationMap();
        initializeKeywordsMap();
    }

    //map to search for keywords instead of sort
    private void initializeKeywordsMap() {
        //Add keywords to the map
        keywordsMap.put("if", Token.TokenTypes.IF);
        keywordsMap.put("else", Token.TokenTypes.ELSE);
        keywordsMap.put("class", Token.TokenTypes.CLASS);
        keywordsMap.put("interface", Token.TokenTypes.INTERFACE);
        keywordsMap.put("accessor", Token.TokenTypes.ACCESSOR);
        keywordsMap.put("colon", Token.TokenTypes.COLON);
        keywordsMap.put("mutator", Token.TokenTypes.MUTATOR);
        keywordsMap.put("loop", Token.TokenTypes.LOOP);
        keywordsMap.put("and", Token.TokenTypes.AND);
        keywordsMap.put("or", Token.TokenTypes.OR);
        keywordsMap.put("not", Token.TokenTypes.NOT);
        keywordsMap.put("true", Token.TokenTypes.TRUE);
        keywordsMap.put("false", Token.TokenTypes.FALSE);
        keywordsMap.put("shared", Token.TokenTypes.SHARED);
        keywordsMap.put("construct", Token.TokenTypes.CONSTRUCT);
        keywordsMap.put("new", Token.TokenTypes.NEW);
        keywordsMap.put("private", Token.TokenTypes.PRIVATE);
        keywordsMap.put("implements", Token.TokenTypes.IMPLEMENTS);
    }

    //map of punctuation
    private void initializePunctuationMap() {
        punctuationMap.put('=', Token.TokenTypes.ASSIGN);
        punctuationMap.put('>', Token.TokenTypes.GREATERTHAN);
        punctuationMap.put('<', Token.TokenTypes.LESSTHAN);
        punctuationMap.put('!', Token.TokenTypes.NOTEQUAL);
        punctuationMap.put(':', Token.TokenTypes.COLON);
        punctuationMap.put('(', Token.TokenTypes.LPAREN);
        punctuationMap.put(')', Token.TokenTypes.RPAREN);
        punctuationMap.put('.', Token.TokenTypes.DOT);
        punctuationMap.put('+', Token.TokenTypes.PLUS);
        punctuationMap.put('-', Token.TokenTypes.MINUS);
        punctuationMap.put(',', Token.TokenTypes.COMMA);
        punctuationMap.put('*', Token.TokenTypes.TIMES);

    }

    public List<Token> Lex() throws Exception {//makes linked list of input
        while (!textManager.isAtEnd()) {//loop
            char c = textManager.getCharacter();//get single character from input

            if (Character.isLetter(c)) {//if c is letter
                ListOfTokens.add(parseWord(c));
                System.out.println("Parsed token: " + ListOfTokens);
            } else if (Character.isDigit(c)) {//if c is num
                ListOfTokens.add(parseNumber(c));
                System.out.println("Parsed token: " + ListOfTokens);
            } else if (isPunctuation(c)) {//if c is punctuation
                ListOfTokens.add(parsePunctuation(c));
                System.out.println("Parsed token: " + ListOfTokens);
            }else if( c=='&'||c=='|'){
                ListOfTokens.add(parseand(c));
            } else if (c == '\n') {
                lineNumber++;
                currentIndentation = 0;
                characterPosition = 0;
                ListOfTokens.add(new Token(Token.TokenTypes.NEWLINE, lineNumber, characterPosition));
                System.out.println("Parsed token: " + ListOfTokens);
                    ListOfTokens.addAll(parseNewline(c));
                System.out.println("Parsed token: " + ListOfTokens);
            } else if (c == '\t') {
                characterPosition += 4;
            } else if (c == '\"') {//start of quote
                ListOfTokens.add(parseQuotedString());
                System.out.println("Parsed token: " + ListOfTokens);
            } else if (c == '\'') {//start of exact character
                ListOfTokens.add(parseCharacter());
                System.out.println("Parsed token: " + ListOfTokens);
            } else if (c == '{') {
                parseComment();
            }
            while(textManager.isAtEnd() && previousIndent > 0) {
                previousIndent -= 4;
                ListOfTokens.add(new Token(Token.TokenTypes.DEDENT, lineNumber, characterPosition));
                System.out.println("Parsed token: " + ListOfTokens);
            }
        }

        return ListOfTokens;//out all defined tokens from text
    }


    //Token management for words/letters
    private Token parseWord(char firstChar) throws Exception {
        StringBuilder CurrentWord = new StringBuilder();//blank string
        CurrentWord.append(firstChar);//adds character to new string
        //easier to use string builder and append then initializing String Current Word
        while (!textManager.isAtEnd() && Character.isLetter(textManager.peekCharacter())) {
            CurrentWord.append(textManager.getCharacter());//starts word with character
        }
        String word = CurrentWord.toString();
        if (keywordsMap.containsKey(word)) {
            return new Token(keywordsMap.get(word), textManager.getLineNumber(),
                    textManager.getColumnNumber());
        } else {
            //return method with Token Type, Line, Column, and uses toString for print style
            return new Token(Token.TokenTypes.WORD, textManager.getLineNumber(),
                    textManager.getColumnNumber(), CurrentWord.toString());
        }
    }

    private Token parseNumber(char firstChar) throws Exception {//in case of num
        StringBuilder num = new StringBuilder();//num holder like parse word
        num.append(firstChar);
        boolean period = false;//set to false until period/decimal
        while (!textManager.isAtEnd()) {//not at end of text
            char twoChar = textManager.peekCharacter();
            if (Character.isDigit(twoChar)) {//two num in a row
                num.append(textManager.getCharacter());
            } else if (twoChar == '.' && !period) {//case of decimal
                num.append(textManager.getCharacter());
                period = true;
            } else {
                break;
            }
        }
        //similar return to word, Instead new Token of NUMBER
        return new Token(Token.TokenTypes.NUMBER, textManager.getLineNumber(),
                textManager.getColumnNumber(), num.toString());
    }
    private Token parseand(char c) throws Exception {
        Token.TokenTypes type = null;
        if (c == '&' && !textManager.isAtEnd() && textManager.peekCharacter() == '&') {
            textManager.getCharacter();
            type = Token.TokenTypes.AND;
        }
        if (c == '|' && !textManager.isAtEnd() && textManager.peekCharacter() == '|') {
            textManager.getCharacter();
            type = Token.TokenTypes.OR;
        }

        return new Token(type, textManager.getLineNumber(), textManager.getColumnNumber());
    }

    //this one was difficult lol
    //our current character is a punctuation
    //were looking at the next character after to see what Token it is
    private Token parsePunctuation(char c) throws Exception {
        Token.TokenTypes type;
        if (punctuationMap.containsKey(c)) {
            type = punctuationMap.get(c); //Get the token type from the map
            if (c == '=' && !textManager.isAtEnd() && textManager.peekCharacter() == '=') {
                textManager.getCharacter();
                type = Token.TokenTypes.EQUAL;
            } else if (c == '>' && !textManager.isAtEnd() && textManager.peekCharacter() == '=') {
                textManager.getCharacter();
                type = Token.TokenTypes.GREATERTHANEQUAL;
            } else if (c == '<' && !textManager.isAtEnd() && textManager.peekCharacter() == '=') {
                textManager.getCharacter();
                type = Token.TokenTypes.LESSTHANEQUAL;
            } else if (c == '!' && !textManager.isAtEnd() && textManager.peekCharacter() == '=') {
                textManager.getCharacter();
                type = Token.TokenTypes.NOTEQUAL;
            } else if (c == '.' && !textManager.isAtEnd() && Character.isDigit(textManager.peekCharacter())) {
                StringBuilder num = new StringBuilder();
                num.append(c);
                while (!textManager.isAtEnd() && Character.isDigit(textManager.peekCharacter())) {
                    num.append(textManager.getCharacter());
                }
                return new Token(Token.TokenTypes.NUMBER, textManager.getLineNumber(), textManager.getColumnNumber(), num.toString());

            }
        } else {
            throw new Exception("Unexpected punctuation: " + c);
        }//return like other parse except no need for string
        return new Token(type, textManager.getLineNumber(), textManager.getColumnNumber());
    }

    private boolean isPunctuation(char c) {
        return punctuationMap.containsKey(c);//defined punctuations
    }

    //in case of tab
    private List<Token> parseNewline(char space) throws Exception {
        currentIndentation = 0;//set to 0 after new line
        characterPosition = 0;//set position to 0
        List<Token> returnVal = new LinkedList<>();
        char c = textManager.peekCharacter();//check next character
            if (c == '\n') {//for new line than newline
                lineNumber++;//increment line num
                textManager.getCharacter();//get next
                returnVal.add(new Token(Token.TokenTypes.NEWLINE, lineNumber, characterPosition));
                if(textManager.peekCharacter() == '\t'){//tab after new line new line
                    textManager.getCharacter();
                    characterPosition += 4;//for indent
                    currentIndentation += 4;//for indent
                    returnVal.add(new Token(Token.TokenTypes.INDENT,lineNumber,characterPosition));
                    while(textManager.peekCharacter() =='\t'){
                        textManager.getCharacter();
                        characterPosition += 4;
                        currentIndentation += 4;
                        returnVal.add(new Token(Token.TokenTypes.INDENT,lineNumber,characterPosition));
                    }

                    if (currentIndentation > previousIndent) {//case of indent
                        previousIndent = currentIndentation;
                        returnVal.add(new Token(Token.TokenTypes.INDENT, lineNumber, characterPosition));
                    } else if (previousIndent > currentIndentation) {//case of dedent
                        while (previousIndent > currentIndentation) {
                            previousIndent -= 4;
                            returnVal.add(new Token(Token.TokenTypes.DEDENT, lineNumber, characterPosition));
                        }
                        previousIndent = currentIndentation;
                    }
                    return returnVal;
                }
                return returnVal;
            } else if (Character.isDigit(textManager.peekCharacter())) {//after newline immediately digit
                if (previousIndent > currentIndentation) {
                    while (previousIndent > currentIndentation) {
                        previousIndent -= 4;
                        returnVal.add(new Token(Token.TokenTypes.DEDENT, lineNumber, characterPosition));
                    }
                }
                return returnVal;
            } else if (Character.isLetter(textManager.peekCharacter())) {//after newline immediately letter
                if (previousIndent > currentIndentation) {
                    while (previousIndent > currentIndentation) {
                        previousIndent -= 4;
                        returnVal.add(new Token(Token.TokenTypes.DEDENT, lineNumber, characterPosition));
                    }
                }
                return returnVal;
            } else if (c == '\u0000') {//special case of peek
                if (previousIndent > currentIndentation) {
                    while (previousIndent > currentIndentation) {
                        // for (int i = previousIndent; i > currentIndentation + 4; i -=4) {
                        previousIndent -= 4;
                        returnVal.add(new Token(Token.TokenTypes.DEDENT, lineNumber, characterPosition));
                    }
                    return returnVal;
                }
            }else if(c =='\t'){//teab after newline
                textManager.getCharacter();
                characterPosition += 4;
                currentIndentation += 4;
                while(textManager.peekCharacter() =='\t'){
                    textManager.getCharacter();
                    characterPosition += 4;
                    currentIndentation += 4;
                }
                if(textManager.peekCharacter() == '\n'){//newline tab then tab
                    textManager.getCharacter();
                    lineNumber++;
                    returnVal.add(new Token(Token.TokenTypes.NEWLINE, lineNumber, characterPosition));
                    while(textManager.peekCharacter() =='\n'){
                        lineNumber++;
                        textManager.getCharacter();
                        returnVal.add(new Token(Token.TokenTypes.NEWLINE, lineNumber, characterPosition));
                    }
                }
                    if (currentIndentation > previousIndent) {
                        previousIndent = currentIndentation;
                        returnVal.add(new Token(Token.TokenTypes.INDENT, lineNumber, characterPosition));
                    } else if (previousIndent > currentIndentation) {
                        while (previousIndent > currentIndentation) {
                            // for (int i = previousIndent; i > currentIndentation + 4; i -=4) {
                            previousIndent -= 4;
                            returnVal.add(new Token(Token.TokenTypes.DEDENT, lineNumber, characterPosition));
                        }
                        previousIndent = currentIndentation;
                }
                return returnVal;
            }

            while (c == ' ' && textManager.peekCharacter() == ' ') {//newline than normal space indents
                textManager.getCharacter();
                currentIndentation++;
                characterPosition++;
            }
                if (currentIndentation > previousIndent) {//dedent and indent logic
                    previousIndent = currentIndentation;
                    returnVal.add(new Token(Token.TokenTypes.INDENT, lineNumber, characterPosition));
                } else if (previousIndent > currentIndentation) {
                    while (previousIndent > currentIndentation) {
                        // for (int i = previousIndent; i > currentIndentation + 4; i -=4) {
                        previousIndent -= 4;
                        returnVal.add(new Token(Token.TokenTypes.DEDENT, lineNumber, characterPosition));
                    }
                    previousIndent = currentIndentation;
                    //return new Token(Token.TokenTypes.DEDENT, lineNumber, characterPosition);
                }
            return returnVal;
    }

    private Token parseQuotedString() throws Exception {
        StringBuilder QuotedString = new StringBuilder();//build blank string
        boolean escape = false;
        while (!textManager.isAtEnd()) {
            char c = textManager.getCharacter();//not peeking actually pulling character
            if (escape) {
                //since we used a stringbuilder we can append the character
                QuotedString.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '\"') {
                //proper case of string so we can return as quote
                return new Token(Token.TokenTypes.QUOTEDSTRING, lineNumber, characterPosition, QuotedString.toString());
            } else if (c == '\n') {
                //new lines inside the quoted string
                lineNumber++;
                characterPosition = 0;
                QuotedString.append(c);
            } else {
                //this is important to return our text in quote as well as the quote token
                QuotedString.append(c);
            }
        }
        throw new SyntaxErrorException("Unclosed string literal", lineNumber, characterPosition);
    }//case of literal character

    private Token parseCharacter() throws Exception {
        char c = textManager.getCharacter(); // Consume the first character after the single quote
        if (c == '\\') { // Handle escape sequences
            c = textManager.getCharacter(); // Get the actual escape character
        }
        if (textManager.getCharacter() != '\'') { // Ensure the closing quote is present
            throw new SyntaxErrorException("Unclosed character literal", lineNumber, characterPosition);
        }
        return new Token(Token.TokenTypes.QUOTEDCHARACTER, lineNumber, characterPosition, String.valueOf(c));
    }
    private void parseComment() throws Exception {
        while (!textManager.isAtEnd()) {//comment case
            char c = textManager.getCharacter();
            if (c == '}') {//end comment
                textManager.getCharacter();
                characterPosition++;
              break;
            }
            if (c == '\n') {//newline in comment
                lineNumber++;
                characterPosition = 0;
            }
            characterPosition++;
        }
    }
}