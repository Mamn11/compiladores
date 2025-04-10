import java.io.*;
import java.util.*;

// Classe principal que contém a função main
public class main {
    public static void main(String[] args) {
        String caminhoArquivo = "C:/Users/mamn2/OneDrive/Documentos/Compiladores/codigo-fonte.lc";
        AnaliseLexica.lerArquivoAteEnd(caminhoArquivo);
        AnaliseLexica.exibirTabela();

        List<String> listaTokens = AnaliseLexica.obterTokens(); // Método que retorna tokens extraídos
        AnalisadorSintatico analisador = new AnalisadorSintatico(listaTokens);
        analisador.analisar();
    }
}

// Classe responsável pela análise léxica
class AnaliseLexica {
    private static final Map<String, Simbolo> tabelaSimbolos = new HashMap<>();
    private static final Map<String, Integer> contadorTokens = new HashMap<>();
    private static final List<String> palavrasReservadas = Arrays.asList(
            "final", "int", "byte", "string", "while", "if", "else",
            "and", "or", "not", "==", "=", "(", ")", "<", ">", ">=",
            "<=", ",", "+", "-", "*", "/", ";", "begin", "end",
            "readln", "write", "writeln", "true", "false", "boolean"
    );

    static {
        contadorTokens.put("RESERVADA", 1);
        contadorTokens.put("ID", 1);
        contadorTokens.put("CONST", 1);
        contadorTokens.put("COMENTARIO", 1);

        for (String palavra : palavrasReservadas) {
            String token = "RESERVADA_" + contadorTokens.get("RESERVADA");
            tabelaSimbolos.put(palavra, new Simbolo(token, null));
            contadorTokens.put("RESERVADA", contadorTokens.get("RESERVADA") + 1);
        }
    }

    private static void inserirNaTabela(String lexema, String tipo) {
        if (!tabelaSimbolos.containsKey(lexema)) {
            String token = tipo + "_" + contadorTokens.get(tipo);
            tabelaSimbolos.put(lexema, new Simbolo(token, null));
            contadorTokens.put(tipo, contadorTokens.get(tipo) + 1);
        }
    }

    public static void lerArquivoAteEnd(String caminhoArquivo) {
        try (BufferedReader leitor = new BufferedReader(new FileReader(caminhoArquivo))) {
            StringBuilder lexemaAtual = new StringBuilder();
            boolean dentroString = false;
            boolean dentroComentarioBloco = false;
            boolean dentroComentarioLinha = false;
            int caractere;

            while ((caractere = leitor.read()) != -1) {
                char c = (char) caractere;

                if (c == '\n') {
                    System.out.println("Erro léxico: quebra de linha não permitida");
                    continue;
                }

                if (dentroString) {
                    lexemaAtual.append(c);
                    if (c == '"' || c == '\'') {
                        System.out.println("String encontrada: " + lexemaAtual);
                        lexemaAtual.setLength(0);
                        dentroString = false;
                    }
                } else if (c == '"' || c == '\'') {
                    dentroString = true;
                    lexemaAtual.append(c);
                } else if (dentroComentarioBloco) {
                    lexemaAtual.append(c);
                    if (lexemaAtual.toString().endsWith("*/")) {
                        System.out.println("Comentário de bloco encontrado: " + lexemaAtual);
                        lexemaAtual.setLength(0);
                        dentroComentarioBloco = false;
                    }
                } else if (dentroComentarioLinha) {
                    lexemaAtual.append(c);
                } else if (c == '/' && leitor.ready()) {
                    int nextChar = leitor.read();
                    if (nextChar == '*') {
                        dentroComentarioBloco = true;
                        lexemaAtual.append("/*");
                    } else if (nextChar == '/') {
                        dentroComentarioLinha = true;
                        lexemaAtual.append("//");
                    } else {
                        System.out.println("Erro léxico: '/' inesperado");
                    }
                } else if (Character.isLetterOrDigit(c) || c == '_') {
                    lexemaAtual.append(c);
                } else {
                    if (lexemaAtual.length() > 0) {
                        String lexema = lexemaAtual.toString();
                        if (lexema.matches("\\d+")) {
                            inserirNaTabela(lexema, "CONST");
                            System.out.println("Constante encontrada: " + tabelaSimbolos.get(lexema).token);
                        } else if (tabelaSimbolos.containsKey(lexema)) {
                            System.out.println("Palavra Reservada encontrada: " + tabelaSimbolos.get(lexema).token);
                        } else if (lexema.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                            inserirNaTabela(lexema, "ID");
                            System.out.println("ID encontrado: " + tabelaSimbolos.get(lexema).token);
                        } else {
                            System.out.println("Erro léxico: lexema inválido - " + lexema);
                        }
                        lexemaAtual.setLength(0);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo: " + e.getMessage());
        }
    }

    public static List<String> obterTokens() {
        return new ArrayList<>(tabelaSimbolos.keySet());
    }

    public static void exibirTabela() {
        System.out.println("\nTabela de Símbolos");
        System.out.println("=".repeat(60));
        System.out.printf("%-25s | %-20s | %s%n", "Lexema", "Token", "Bytes");
        System.out.println("-".repeat(60));

        tabelaSimbolos.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    System.out.printf("%-25s | %-20s | null%n", entry.getKey(), entry.getValue().token);
                });

        System.out.println("=".repeat(60));
    }

    static class Simbolo {
        String token;
        Object bytes;

        Simbolo(String token, Object bytes) {
            this.token = token;
            this.bytes = bytes;
        }
    }
}

class AnalisadorSintatico {
    private List<String> tokens;
    private int posicaoAtual;

    public AnalisadorSintatico(List<String> tokens) {
        this.tokens = tokens;
        this.posicaoAtual = 0;
    }

    public void analisar() {
        // Verifica se o código começa com 'begin' e termina com 'end'
        if (!tokens.contains("begin")) {
            System.out.println("Erro sintático: Código deve começar com 'begin'");
        }
        if (!tokens.contains("end")) {
            System.out.println("Erro sintático: Código deve terminar com 'end'");
        }

        // Validação de estrutura de comandos e blocos
        while (posicaoAtual < tokens.size()) {
            String token = tokens.get(posicaoAtual);

            if (token.equals("if")) {
                verificarIf();
            } else if (token.equals("while")) {
                verificarWhile();
            } else if (token.equals("else")) {
                verificarElse();
            } else if (token.equals("begin") || token.equals("end")) {
                verificarBeginend();
            }
            posicaoAtual++;
        }

        // Finalização da análise
        System.out.println("Análise sintática concluída!");
    }

    // Verifica se um 'if' tem a estrutura correta
    private void verificarIf() {
        posicaoAtual++;
        if (posicaoAtual < tokens.size() && tokens.get(posicaoAtual).equals("(")) {
            posicaoAtual++;
            // Verifica se há uma expressão dentro do parêntese
            if (posicaoAtual < tokens.size() && !tokens.get(posicaoAtual).equals(")")) {
                System.out.println("Erro sintático: Expressão esperada após '(' em 'if'");
                return;
            }
            posicaoAtual++;  // Para passar o ')'
            if (posicaoAtual < tokens.size() && tokens.get(posicaoAtual).equals("begin")) {
                posicaoAtual++;
                while (posicaoAtual < tokens.size() && !tokens.get(posicaoAtual).equals("end")) {
                    posicaoAtual++;
                }
                if (posicaoAtual >= tokens.size() || !tokens.get(posicaoAtual).equals("end")) {
                    System.out.println("Erro sintático: Falta 'end' no bloco do 'if'");
                }
            } else {
                System.out.println("Erro sintático: Esperado 'begin' após 'if' para iniciar o bloco");
            }
        } else {
            System.out.println("Erro sintático: Esperado '(' após 'if'");
        }
    }

    // Verifica se um 'while' tem a estrutura correta
    private void verificarWhile() {
        posicaoAtual++;
        if (posicaoAtual < tokens.size() && tokens.get(posicaoAtual).equals("(")) {
            posicaoAtual++;
            // Verifica se há uma expressão dentro do parêntese
            if (posicaoAtual < tokens.size() && !tokens.get(posicaoAtual).equals(")")) {
                System.out.println("Erro sintático: Expressão esperada após '(' em 'while'");
                return;
            }
            posicaoAtual++;  // Para passar o ')'
            if (posicaoAtual < tokens.size() && tokens.get(posicaoAtual).equals("begin")) {
                posicaoAtual++;
                while (posicaoAtual < tokens.size() && !tokens.get(posicaoAtual).equals("end")) {
                    posicaoAtual++;
                }
                if (posicaoAtual >= tokens.size() || !tokens.get(posicaoAtual).equals("end")) {
                    System.out.println("Erro sintático: Falta 'end' no bloco do 'while'");
                }
            } else {
                System.out.println("Erro sintático: Esperado 'begin' após 'while' para iniciar o bloco");
            }
        } else {
            System.out.println("Erro sintático: Esperado '(' após 'while'");
        }
    }

    // Verifica se um 'else' está corretamente posicionado após um 'if'
    private void verificarElse() {
        posicaoAtual++;
        if (posicaoAtual < tokens.size() && tokens.get(posicaoAtual).equals("begin")) {
            posicaoAtual++;
            while (posicaoAtual < tokens.size() && !tokens.get(posicaoAtual).equals("end")) {
                posicaoAtual++;
            }
            if (posicaoAtual >= tokens.size() || !tokens.get(posicaoAtual).equals("end")) {
                System.out.println("Erro sintático: Falta 'end' no bloco do 'else'");
            }
        } else {
            System.out.println("Erro sintático: Esperado 'begin' após 'else' para iniciar o bloco");
        }
    }

    // Verifica a correspondência de chaves begin e and
    private void verificarBeginend() {
        if (tokens.get(posicaoAtual).equals("begin")) {
            // Um novo bloco começa
            System.out.println("Início de bloco encontrado: begin");
        } else if (tokens.get(posicaoAtual).equals("end")) {
            // Um bloco termina
            System.out.println("Fim de bloco encontrado: end");
        }
    }

    // Método responsável por verificar a estrutura de uma operação de soma no código fonte
    private void verificarSoma() {

        // Avança para o próximo token (pulando o token atual)
        posicaoAtual++;

        // Verifica se ainda existem tokens a serem analisados
        // e se o próximo token é o operador de soma "+"
        if (posicaoAtual < tokens.size() && tokens.get(posicaoAtual).equals("+")) {

            // Avança novamente para o próximo token após o "+"
            posicaoAtual++;

            // Verifica se existe um token após o "+"
            // e se esse token não é inválido
            if (posicaoAtual < tokens.size() && !tokens.get(posicaoAtual).equals("")) {

                // Exibe mensagem de erro caso não haja uma expressão válida após o "+"
                System.out.println("Erro sintático: Expressão esperada após '+' '");
                return; // Encerra a verificação pois o erro foi detectado
            } else if (tokens.get(posicaoAtual).equals(";")) {
                // Um bloco termina
                System.out.println("Fim de de linha encontrado: ;");
            }


            private void verificaSub() {

                // Avança para o próximo token (pulando o token atual)
                posicaoAtual++;

                // Verifica se ainda existem tokens a serem analisados
                // e se o próximo token é o operador de soma "+"
                if (posicaoAtual < tokens.size() && tokens.get(posicaoAtual).equals("-")) {

                    // Avança novamente para o próximo token após o "-"
                    posicaoAtual++;

                    // Verifica se existe um token após o "+"
                    // e se esse token não é inválido
                    if (posicaoAtual < tokens.size() && !tokens.get(posicaoAtual).equals("")) {

                        // Exibe mensagem de erro caso não haja uma expressão válida após o "+"
                        System.out.println("Erro sintático: Expressão esperada após '-' '");
                        return; // Encerra a verificação pois o erro foi detectado
                    } else if (tokens.get(posicaoAtual).equals(";")) {
                        // Um bloco termina
                        System.out.println("Fim de de linha encontrado: ;");
                    }


                } else {
                    // Caso não encontre o "+" após o que estava esperando, apresenta erro
                    System.out.println("Erro sintático: Esperado '(' após 'while'");
                }
            }

        }
    }}
