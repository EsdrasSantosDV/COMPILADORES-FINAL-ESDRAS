package lexico.com.esdras.org.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AnalisadorSintatico {


    //VARIAVEIS NOVAS
    private final static int SIZEOF_INT = 4;

    private int nivel;

    private TabelaSimbolos tabela;


    private int endereco;
    private int offsetVariavel;
    private int contRotulo = 1;


    private List<Registro> ultimasVariaveisDeclaradas = new ArrayList<>();
    private Tipo ultimoTipoUsado = null;
    private List<String> sectionData = new ArrayList<>();


    private String nomeArquivoSaida;
    private String caminhoArquivoSaida;
    private BufferedWriter bw;
    private FileWriter fw;

    private String rotulo = "";
    private String rotFim;
    private String rotElse;
    private String operadorRelacional;
    private boolean writeln;



    //DEMARCAÇÃO DAS VARIAVEIS NOVAS

    private AnalisadorLexico lexico;
    private Token token;

    private int linha;

    private int coluna;


    public void LerToken(){
        token = lexico.getToken(linha, coluna);
        coluna = token.getColuna()+token.getTamanhoToken();
        linha = token.getLinha();
        System.out.println(token);

    }

    public AnalisadorSintatico(String nomeArquivo){
        linha=1;
        coluna=1;
        this.lexico=new AnalisadorLexico(nomeArquivo);
    }

    public void Analisar(){
        LerToken();

        this.endereco = 0;

        nomeArquivoSaida = "CODIGOC.c";
        caminhoArquivoSaida = Paths.get(nomeArquivoSaida).toAbsolutePath().toString();

        bw = null;
        fw = null;

        try {
            fw = new FileWriter(caminhoArquivoSaida, Charset.forName("UTF-8"));
            bw = new BufferedWriter(fw);
            programa();
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("== TABELA DE SIMBOLOS ==");
        System.out.println(this.tabela);


    }

    private String criarRotulo(String texto) {
        String retorno = "rotulo" + texto + contRotulo;
        contRotulo++;
        return retorno;
    }

    private void gerarCodigo(String instrucoes) {
        try {
            if (rotulo.isEmpty()) {
                bw.write(instrucoes + "\n");
            } else {
                bw.write(rotulo + ": " +  instrucoes + "\n");
                rotulo = "";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void mensagemErro(String msg) {
        System.err.println("Linha: " + token.getLinha() +
                ", Coluna: " + token.getColuna() +
                msg);
    }

    public void programa() {
        if ((token.getClasse() == Classe.cPalRes)
                && (token.getValor().getValorIdentificador().equalsIgnoreCase("program"))) {
            LerToken();
            if (token.getClasse() == Classe.cId) {
                LerToken();
                A1();
                corpo();
                if (token.getClasse() == Classe.cPonto) {
                    LerToken();
                } else {
                    mensagemErro(" - FALTOU ENCERRAR COM PONTO");
                }
                A2();
            } else {
                mensagemErro(" -FALTOU IDENTIFICAR O NOME DO PROGRAMA");
            }
        } else {
            mensagemErro(" -FALTOU COMEÇAR COM PROGRAMA");
        }
    }

    public void A1()
    {
       tabela=new TabelaSimbolos();

       tabela.setTabelaPai(null);

        Registro registro=new Registro();
        registro.setNome(token.getValor().getValorIdentificador());
        registro.setCategoria(Categoria.PROGRAMAPRINCIPAL);

        registro.setNivel(0);
        registro.setOffset(0);
        registro.setTabelaSimbolos(tabela);
        registro.setRotulo("main");
        tabela.inserirRegistro(registro);
        nivel=0;
        offsetVariavel=0;
        String codigo = "#include <stdio.h>\n" +
                "\nint main(){\n";

        gerarCodigo(codigo);

    }

    public void A2()
    {
        Registro registro=new Registro();
        registro.setNome(null);
        registro.setCategoria(Categoria.PROGRAMAPRINCIPAL);
        registro.setNivel(0);
        registro.setOffset(0);
        registro.setTabelaSimbolos(tabela);
        registro.setRotulo("finalCode");
        tabela.inserirRegistro(registro);
        nivel=0;
        offsetVariavel=0;
        String codigo = "\n}\n";
        gerarCodigo(codigo);
    }

    public void corpo() {
        declara();
        if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("begin"))) {
            LerToken();
            sentencas();
            if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("end"))) {
                LerToken();
            }else {
                mensagemErro(" -FALTOU FINALIZAR COM END");
            }
        }else {
            mensagemErro(" -FALTOU O BEGIN NO CORPO");
        }
    }

    public void declara() {
        if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("var"))) {
            LerToken();
            dvar();
            mais_dc();
        }
    }

    public void mais_dc() {
        if (token.getClasse() == Classe.cPontoVirgula) {
            LerToken();
            cont_dc();
        } else {
            mensagemErro(" -FALTOU COLOCAR O PONTO E VIRGULA");
        }
    }

    public void cont_dc() {
        if (token.getClasse() == Classe.cId) {
            dvar();
            mais_dc();
        }
    }

    public void dvar() {
        variaveis();
        if (token.getClasse() == Classe.cDoisPontos) {
            LerToken();
            tipo_var();
        }else {
            mensagemErro(" -FALTOU OS DOIS PONTOS");
        }
    }

    public void tipo_var() {
        if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("integer"))) {
            A3("int");
            LerToken();


        }else if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("real"))) {
            A3("float");
            LerToken();
        }else {
            mensagemErro(" -FALTOU A DECLARAÇÃO DO INTEGER");
        }




    }

    private void A3(String type) {
        String codigo= '\t'+type;
        for(int i=0;i<this.ultimasVariaveisDeclaradas.size();i++)
        {
            codigo=codigo+' '+ this.ultimasVariaveisDeclaradas.get(i).getNome();
            if(i == this.ultimasVariaveisDeclaradas.size()-1)
            {
                codigo=codigo + ';';
            }
            else{
                codigo=codigo + ',';
            }
        }
        gerarCodigo(codigo);
    }



    public void variaveis() {
        if (token.getClasse() == Classe.cId) {
            A4();
            LerToken();
            mais_var();
        }else {
            mensagemErro(" -FALTOU IDENTIFICADOR");
        }
    }

    public void A4()
    {
        Registro registro=new Registro();
        registro.setNome(token.getValor().getValorIdentificador());
        registro.setCategoria(Categoria.VARIAVEL);
        registro.setNivel(0);
        registro.setOffset(0);
        registro.setTabelaSimbolos(tabela);
        this.endereco++;
        registro.setRotulo("variavel"+this.endereco);
        ultimasVariaveisDeclaradas.add(registro);
        this.tabela.inserirRegistro(registro);
    }

    public void mais_var(){
        if (token.getClasse() == Classe.cVirgula) {
            LerToken();
            variaveis();
        }
    }


    public void sentencas() {
        comando();
        mais_sentencas();
    }


    public void mais_sentencas() {
        if (token.getClasse() == Classe.cPontoVirgula) {
            LerToken();
            cont_sentencas();
        }else {
            mensagemErro(" -FALTOU O PONTO E VIRGULA");
        }
    }



    public void cont_sentencas() {
        if (((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("read"))) ||
                ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("write"))) ||
                ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("for"))) ||
                ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("repeat"))) ||
                ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("while"))) ||
                ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("if"))) ||
                ((token.getClasse() == Classe.cId))
        ) {
            sentencas();
        }
    }


    public List<Token> var_read(List<Token> arrayTokens) {
        if (token.getClasse() == Classe.cId) {
            arrayTokens.add(token);
            LerToken();
            //{A5}
            arrayTokens = mais_var_read(arrayTokens);
        }else {
            mensagemErro(" -FALTOU O IDENTIFICADOR");
        }
        return arrayTokens;
    }


    public List<Token> mais_var_read(List<Token> arrayTokens) {
        if (token.getClasse() == Classe.cVirgula) {
            LerToken();
            arrayTokens = var_read(arrayTokens);
        }
        return arrayTokens;
    }



    public String var_write(String codigo) {

        if (token.getClasse() == Classe.cId) {
            codigo=codigo+token.getValor().getValorIdentificador();
            LerToken();
            //{A6}
            codigo=mais_var_write(codigo);
        }else {
            mensagemErro(" -FALTOU O IDENTIFICADOR");
        }

        return codigo;
    }


    public String mais_var_write(String codigo) {
        if (token.getClasse() == Classe.cVirgula) {
            codigo=codigo+ ',';
            LerToken();
            codigo=var_write(codigo);

        }
        return codigo;
    }

    public void comando() {

        if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("read"))){
            String codigo="\tscanf";
            LerToken();
            if (token.getClasse() == Classe.cParEsq) {
                codigo=codigo+"(\"";
                LerToken();
                List<Token> arrayToken = new ArrayList<Token>();
                arrayToken=var_read(arrayToken);
                for(Token i: arrayToken){
                    codigo=codigo+"%d ";
                }
                codigo=codigo+"\", ";
                for(Token i: arrayToken){
                    if(i == arrayToken.get(arrayToken.size()-1)){
                        codigo=codigo+"&"+i.getValor().getValorIdentificador();
                    }else{
                        codigo=codigo+"&"+i.getValor().getValorIdentificador()+", ";
                    }
                }
                if (token.getClasse() == Classe.cParDir) {

                    codigo=codigo+");";
                    gerarCodigo(codigo);
                    LerToken();
                }else {
                    mensagemErro(" -FALTOU PARENTESE DIREITO )");
                }
            }else {
                mensagemErro(" -FALTOU PARENTESE ESQUERDO (");
            }
        }else
            //write ( <var_write> ) |
            if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("write"))){
                String referencias="\tprintf";
                String codigo = "";
                LerToken();
                if (token.getClasse() == Classe.cParEsq) {
                    referencias = referencias + "(\"";
                    LerToken();

                    codigo=codigo+var_write("");

                    if (codigo.length() >  0) {
                        referencias = referencias + "%d ".repeat(codigo.split(",").length);
                        referencias = referencias + "\", ";
                    } else {
                        referencias = referencias + "\"";
                    }

                    if (token.getClasse() == Classe.cParDir) {
                        codigo=codigo+");";
                        gerarCodigo(referencias + codigo);
                        LerToken();
                    }else {
                        mensagemErro(" -FALTOU PARENTESE DIREITO )");
                    }
                }else {
                    mensagemErro(" -FALTOU PARENTESE ESQUERDO (");
                }
            }else

                if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("for"))){
                    String codigo="\n\tfor(";
                    LerToken();
                    if (token.getClasse() == Classe.cId) {
                        String identificador = token.getValor().getValorIdentificador();
                        codigo=codigo+identificador;
                        LerToken();

                        if (token.getClasse() == Classe.cAtribuicao){
                            codigo=codigo+"=";
                            LerToken();

                            codigo=codigo+expressao();

                            if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("to"))){
                                codigo=codigo+";";
                                LerToken();
                                codigo=codigo+identificador;
                                codigo=codigo+"<="+expressao()+";";
                                codigo=codigo+identificador + "++)";
                                if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("do"))){
                                    LerToken();
                                    if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("begin"))){
                                        codigo=codigo+"{";
                                        gerarCodigo(codigo);
                                        LerToken();
                                        sentencas();
                                        if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("end"))){
                                            String codigoFinal = "\t}";
                                            gerarCodigo(codigoFinal);
                                            LerToken();
                                            //{A29}
                                        }else {
                                            mensagemErro(" -FALTOU O END NO FOR");
                                        }
                                    }else {
                                        mensagemErro(" -FALTOU O BEGIN NO FOR");
                                    }
                                }else {
                                    mensagemErro(" -FALTOU O DO NO FOR");
                                }
                            }else {
                                mensagemErro(" -FALTOU O TO NO FOR");
                            }
                        }else {
                            mensagemErro(" -FALTOU O DOIS PONTOS E IGUAL NO FOR");
                        }
                    }else {
                        mensagemErro(" -FALTOU O IDENTIFICADOR NO FOR NO INICIO DO FOR");
                    }
                }else

                    if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("repeat"))){
                        String codigo="\n\tdo {\n\t";

                        LerToken();
                        gerarCodigo(codigo);
                        //{A23}
                        sentencas();
                        if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("until"))){

                            LerToken();
                            if (token.getClasse() == Classe.cParEsq){
                                String codigoFinal="\n\t}while";
                                 codigoFinal=codigoFinal+"(";
                                LerToken();

                                codigoFinal=codigoFinal+condicao();

                                if (token.getClasse() == Classe.cParDir){
                                    codigoFinal=codigoFinal+");";
                                    gerarCodigo(codigoFinal);
                                    LerToken();

                                    //{A24}
                                }else {
                                    mensagemErro(" -FALTOU FECHAR PARENTESES NO REPEAT");
                                }
                            }else {
                                mensagemErro(" -FALTOU ABRIR PARENTES NO REPEAT");
                            }
                        }else {
                            mensagemErro(" -FALTOU UNTIL NO REPEAT");
                        }
                    }

                    else if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("while"))){
                        String codigo="\n\twhile";
                        LerToken();
                        //{A20}
                        if (token.getClasse() == Classe.cParEsq){
                            codigo=codigo+"(";
                            LerToken();
                            codigo=codigo+condicao();
                            if (token.getClasse() == Classe.cParDir){
                                codigo=codigo+")";
                                LerToken();
                                //{A21}
                                if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("do"))){
                                    LerToken();
                                    if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("begin"))){
                                        codigo=codigo+"{\n";
                                        gerarCodigo(codigo);
                                        LerToken();
                                        sentencas();
                                        if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("end"))){
                                            codigo="\t}\n";
                                            gerarCodigo(codigo);
                                            LerToken();
                                            //{A22}
                                        }else {
                                            mensagemErro(" -FALTOU END NO WHILE");
                                        }
                                    }else {
                                        mensagemErro(" -FALTOU BEGIN NO WHILE");
                                    }
                                }else {
                                    mensagemErro(" -FALTOU DO NO WHILE");
                                }
                            }else {
                                mensagemErro(" -FALTOU O PARENTESE DIREITO NO WHILE");
                            }
                        }else {
                            mensagemErro(" -FALTOU O PARENTESE ESQUERDO NO WHILE");
                        }
                    }
                    else if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("if"))){
                        String codigo="";
                        LerToken();
                        if (token.getClasse() == Classe.cParEsq){
                            codigo=codigo+"\n\tif(";
                            LerToken();
                            codigo=codigo+condicao();
                            if (token.getClasse() == Classe.cParDir){
                                codigo=codigo+")";
                                LerToken();
                                //{A17}
                                if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("then"))){
                                    LerToken();
                                    if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("begin"))){
                                        codigo=codigo +" {";
                                        gerarCodigo(codigo);
                                        LerToken();
                                        sentencas();
                                        if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("end"))){
                                            LerToken();

                                            String codigoFinal = "";
                                            codigoFinal = codigoFinal + "\t}";
                                            gerarCodigo(codigoFinal);
                                            //{A22}
                                            pfalsa();
                                            //{A19}
                                        }else {
                                            mensagemErro(" -FALTOU END NO WHILE");
                                        }
                                    }else {
                                        mensagemErro(" -FALTOU BEGIN NO WHILE");
                                    }
                                }else {
                                    mensagemErro(" -FALTOU DO NO WHILE");
                                }
                            }else {
                                mensagemErro(" -FALTOU O PARENTESE DIREITO NO WHILE");
                            }
                        }else {
                            mensagemErro(" -FALTOU O PARENTESE ESQUERDO NO WHILE");
                        }
                    }
                    else if (token.getClasse() == Classe.cId){
                        String codigo="\n\t";
                        codigo=codigo+token.getValor().getValorIdentificador();
                        LerToken();
                        //ação 13
                        if (token.getClasse() == Classe.cAtribuicao){
                            codigo=codigo+"=";
                            LerToken();
                            codigo=codigo+expressao()+";";
                            gerarCodigo(codigo);
                            //{A14}
                        }
                        else {
                            mensagemErro("FALTOU A ATRIBUIÇÃO");
                        }
                    }
    }

    public String condicao() {
        String expressao1 = expressao();
        String relacao = relacao();
        //{A15}
        String expressao2 = expressao();
        //{A16}

        return expressao1 + relacao + expressao2;
    }


    public void pfalsa() {
        String codigo = "";
        if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("else"))){
            codigo = codigo + "\telse";
            LerToken();
            if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("begin"))){
                codigo = codigo + "{";
                gerarCodigo(codigo);
                LerToken();
                sentencas();
                if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("end"))){
                    String codigoFinal = "\n\t}";
                    gerarCodigo(codigoFinal);
                    LerToken();
                }else {
                    mensagemErro(" -FALTOU FINALIZAR COM O END");
                }
            }else {
                mensagemErro(" -FALTOU INICIALIZAR COM O BEGIN");
            }
        }
    }

    public String relacao() {
        String operador="";
        if (token.getClasse() == Classe.cIgual) {
            operador="=";
            LerToken();
        }else if (token.getClasse() == Classe.cMaior) {
            operador=">";
            LerToken();
        }else if (token.getClasse() == Classe.cMenor) {
            operador="<";
            LerToken();
        }else if (token.getClasse() == Classe.cMaiorIgual) {
            operador = ">=";
            LerToken();
        }else if (token.getClasse() == Classe.cMenorIgual) {
            operador = "<=";
            LerToken();
        }else if (token.getClasse() == Classe.cDiferente) {
            operador = "!=";
            LerToken();
        }else {
            mensagemErro(" -FALTOU O OPERADOR DE RELAÇÃO");
        }

        return operador;
    }

    public String expressao() {
        String termo = termo();
        String outrosTermos = outros_termos();

        return termo + outrosTermos;
    }

    public String outros_termos() {
        String op = "";
        String termo= "";
        String outrosTermos = "";

        if (token.getClasse() == Classe.cMais || token.getClasse() == Classe.cMenos) {
            op = op_ad();
            termo = termo();
            outrosTermos = outros_termos();
        }

        return op + termo + outrosTermos;
    }

    public String op_ad() {
        String op = "";
        if (token.getClasse() == Classe.cMais) {
            op = "+";
            LerToken();
        } else if (token.getClasse() == Classe.cMenos) {
            op = "-";
            LerToken();
        }else {
            mensagemErro(" - FALTOU COLOCAR O OPERADOR DE ADIÇÃO OU DE SUBTRAÇÃO");
        }
        return op;
    }

    public String termo() {
        String fator = fator();
        String maisFatores = mais_fatores();

        return fator + maisFatores;
    }


    public String mais_fatores() {
        if (token.getClasse() == Classe.cMultiplicacao || token.getClasse() == Classe.cDivisao) {
            String op = op_mul();
            //{A11}
            String fator = fator();
            //{A12}
            String outrosFatores = mais_fatores();

            return op + fator + outrosFatores;
        }

        return "";
    }

    public String op_mul() {
        String op = "";
        if (token.getClasse() == Classe.cMultiplicacao) {
            op = "*";
            LerToken();
        } else if (token.getClasse() == Classe.cDivisao) {
            op = "/";
            LerToken();
        } else {
            mensagemErro(" -FALTOU A MULTIPLICAÇÃO E DIVISÃO");
        }

        return op;
    }


    public String fator() {
        String returnFator = "";
        if (token.getClasse() == Classe.cId) {
            returnFator = token.getValor().getValorIdentificador();

            LerToken();
            //{A7}
        } else if (token.getClasse() == Classe.cInt) {
            returnFator = String.valueOf(token.getValor().getValorInteiro());
            LerToken();
            //{A8}
        } else if (token.getClasse() == Classe.cReal) {
            returnFator = String.valueOf(token.getValor().getValorDecimal());
            LerToken();
        }else if (token.getClasse() == Classe.cParEsq){
            returnFator="(";
            LerToken();
            returnFator = returnFator + expressao();
            if (token.getClasse() == Classe.cParDir){
                returnFator=returnFator + ")";
                LerToken();
            }else {
                mensagemErro(" -FALTOU PARENTESE DIREITO");
            }
        }else {
            mensagemErro(" -FALTOU FATOR IN NUM EXP");
        }

        return returnFator;
    }

}
