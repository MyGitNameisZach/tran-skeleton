import java.util.List;
import java.util.Optional;

public class TokenManager {
    private final List<Token> tokens;
    private int currentTokenIndex;

    public TokenManager(List<Token> tokens) {
        this.tokens = tokens;
        this.currentTokenIndex = 0;
    }

    public boolean done() {
        return currentTokenIndex >= tokens.size();
    }

    public Optional<Token> matchAndRemove(Token.TokenTypes t) {
        if (currentTokenIndex < tokens.size()) {
            Token currentToken = tokens.get(currentTokenIndex);
            if (currentToken.getType() == t) {
                currentTokenIndex++;
                return Optional.of(currentToken);
            }
        }
        return Optional.empty(); // No match found
    }

    public Optional<Token> peek(int i){
        int peekindex = currentTokenIndex + i;
        if(peekindex < tokens.size() && peekindex >= 0) {
            return Optional.of(tokens.get(peekindex));
        }
        return Optional.empty();
    }

    public boolean nextTwoTokensMatch(Token.TokenTypes first, Token.TokenTypes second){
        if(currentTokenIndex +1 < tokens.size()){
            return tokens.get(currentTokenIndex).getType()== first&&
                    tokens.get(currentTokenIndex+1).getType()== second;
        }
        return false;
    }
    public int getCurrentLine(){
        if(currentTokenIndex < tokens.size()){
            return tokens.get(currentTokenIndex).getLineNumber();
        }
        return -1;
    }

    public int getCurrentColumnNumber(){
        if(currentTokenIndex < tokens.size()){
            return tokens.get(currentTokenIndex).getColumnNumber();
        }
        return -1;
    }
}
