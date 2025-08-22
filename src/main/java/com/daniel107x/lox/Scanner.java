package com.daniel107x.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.daniel107x.lox.TokenType.*;
import static java.lang.Character.isAlphabetic;
import static java.lang.Character.isDigit;

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and",AND);
        keywords.put("class",CLASS);
        keywords.put("else",ELSE);
        keywords.put("false",FALSE);
        keywords.put("for",FOR);
        keywords.put("fun",FUN);
        keywords.put("if",IF);
        keywords.put("nil",NIL);
        keywords.put("or",OR);
        keywords.put("print",PRINT);
        keywords.put("return", RETURN);
        keywords.put("super",SUPER);
        keywords.put("this",THIS);
        keywords.put("true",TRUE);
        keywords.put("var",VAR);
        keywords.put("while",WHILE);
    }


    public Scanner(String source){
        this.source = source;
    }

    List<Token> scanTokens(){
        while(!isAtEnd()){
            start = current;
            scanToken();
        }
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    void scanToken(){
        char c = advance();
        switch (c){
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ':': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            case '!':
                addToken(nextCharacterMatches('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(nextCharacterMatches('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(nextCharacterMatches('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(nextCharacterMatches('=') ? GREATER_EQUAL : GREATER);
                break;
            case '/':
                // We check if the next character is a slash too, so in such case we are in front of a comment
                // The comment is parsed until we reach an EOL or the EOF characters
                // If this is not a comment, we just add the slash token
                if(nextCharacterMatches('/')) while(peek() != '\n' && !isAtEnd()) advance();
                else if(nextCharacterMatches('*')) blockComment();
                else addToken(SLASH);
                break;
            case ' ':
            case '\t':
            case '\r':
                // Ignore whitespaces
                break;
            case '\n':
                line++;
                break;
            case '"':
                string();
                break;
            default:
                if(isDigit(c)) number();
                else if(isAlpha(c)) identifier();
                else Lox.error(line, "Unexpected character");
                break;
        }
    }

    private void blockComment(){
        int nestedCommentLevel = 1;
        int lastCommentBlockLine = line;
        while(!isAtEnd() && nestedCommentLevel > 0){
            if(peek() == '/' && peekNext() == '*'){
                nestedCommentLevel++;
                lastCommentBlockLine = line;
                advance();
                advance();
            }
            else if(peek() == '*' && peekNext() == '/'){
                nestedCommentLevel--;
                advance();
                advance();
            }
            else if(peek() == '\n'){
                line++;
                advance();
            }
            else advance();
        }
        if(nestedCommentLevel > 0) Lox.error(lastCommentBlockLine, "Comment was not closed.");
    }

    private void identifier(){
        while(isAlphaNumeric(peek())) advance();
        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if(type == null) type = IDENTIFIER;
        addToken(type);
    }

    private boolean isAlpha(char c){
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c){
        return isDigit(c) || isAlpha(c);
    }

    private void number(){
        while(isDigit(peek())) advance();
        if(peek() == '.' && isDigit(peekNext())){
            do advance();
            while (isDigit(peek()));
        }
        String value = source.substring(start, current);
        addToken(NUMBER, Double.parseDouble(value));
    }

    private char peekNext(){
        if(current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private void string(){
        StringBuilder builder = new StringBuilder();
        while(peek() != '"' && !isAtEnd()){
            if(peek() == '\n') line++;
            advance();
        }
        if(isAtEnd()){ // We reached the end without closing the string
            Lox.error(line, "Unterminated string.");
            return;
        }
        // This is not the end, so the next character should be "
        advance();
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    private char advance(){
        return source.charAt(current++);
    }

    private void addToken(TokenType type){
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal){
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    private boolean nextCharacterMatches(char expected){
        if(isAtEnd()) return false;
        if(source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    private char peek(){
        if(isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private boolean isAtEnd(){
        return current >= source.length();
    }
}
