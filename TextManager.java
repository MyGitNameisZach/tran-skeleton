public class TextManager {
    private final String text;
    private  int position;
    private int line;
    private int column;

    public TextManager(String text) {
        this.text = text;
        position = 0;
        this.line = 1;
        this.column = 0;
    }


    public int getColumnNumber() {
        return column;
    }
    public int getLineNumber() {
        return line;
    }

    public  boolean isAtEnd() {
        return position >= text.length();
    }

    public char peekCharacter() {

        if(isAtEnd()) {
            return '\0';
        };
        return text.charAt(position);
    }

    public char peekCharacter(int distance) {
        return '~';
    }

    public  char getCharacter() throws Exception {

        if (isAtEnd()) {
           return '\0';

        }
        char C=text.charAt(position);
        position++;
        if(C== '\n'){
            line++;
            column = 0;
        }else{
            column++;
        }
            return C;

    }
}