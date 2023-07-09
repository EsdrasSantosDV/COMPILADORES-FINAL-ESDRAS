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

        nomeArquivoSaida = "CODIGOC";
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
                "int main(){\n";

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
        String codigo = "}\n";
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


    public void var_read() {
        if (token.getClasse() == Classe.cId) {
            LerToken();
            //{A5}
            mais_var_read();
        }else {
            mensagemErro(" -FALTOU O IDENTIFICADOR");
        }
    }


    public void mais_var_read() {
        if (token.getClasse() == Classe.cVirgula) {
            LerToken();
            var_read();
        }
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
            LerToken();
            if (token.getClasse() == Classe.cParEsq) {
                LerToken();
                var_read();
                if (token.getClasse() == Classe.cParDir) {
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
                String codigo="\tprintf";
                LerToken();
                if (token.getClasse() == Classe.cParEsq) {
                    codigo=codigo+"(";
                    LerToken();
                    codigo=codigo+var_write("");
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

                if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("for"))){
                    LerToken();
                    if (token.getClasse() == Classe.cId) {
                        LerToken();

                        if (token.getClasse() == Classe.cAtribuicao){
                            LerToken();
                            expressao();
                            //{A26}
                            if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("to"))){
                                LerToken();
                                //{A27}
                                expressao();
                                //{A28}
                                if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("do"))){
                                    LerToken();
                                    if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("begin"))){
                                        LerToken();
                                        sentencas();
                                        if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("end"))){
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
                        LerToken();
                        //{A23}
                        sentencas();
                        if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("until"))){
                            LerToken();
                            if (token.getClasse() == Classe.cParEsq){
                                LerToken();
                                condicao();
                                if (token.getClasse() == Classe.cParDir){
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
                        LerToken();
                        //{A20}
                        if (token.getClasse() == Classe.cParEsq){
                            LerToken();
                            condicao();
                            if (token.getClasse() == Classe.cParDir){
                                LerToken();
                                //{A21}
                                if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("do"))){
                                    LerToken();
                                    if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("begin"))){
                                        LerToken();
                                        sentencas();
                                        if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("end"))){
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
                        LerToken();
                        if (token.getClasse() == Classe.cParEsq){
                            LerToken();
                            condicao();
                            if (token.getClasse() == Classe.cParDir){
                                LerToken();
                                //{A17}
                                if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("then"))){
                                    LerToken();
                                    if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("begin"))){
                                        LerToken();
                                        sentencas();
                                        if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("end"))){
                                            LerToken();
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
                        LerToken();
                        //ação 13
                        if (token.getClasse() == Classe.cAtribuicao){
                            LerToken();
                            expressao();
                            //{A14}
                        }
                        else {
                            mensagemErro("FALTOU A ATRIBUIÇÃO");
                        }
                    }
    }

    public void condicao() {
        expressao();
        relacao();
        //{A15}
        expressao();
        //{A16}
    }


    public void pfalsa() {
        if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("else"))){
            LerToken();
            if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("begin"))){
                LerToken();
                sentencas();
                if ((token.getClasse() == Classe.cPalRes) && (token.getValor().getValorIdentificador().equalsIgnoreCase("end"))){
                    LerToken();
                }else {
                    mensagemErro(" -FALTOU FINALIZAR COM O END");
                }
            }else {
                mensagemErro(" -FALTOU INICIALIZAR COM O BEGIN");
            }
        }
    }

    public void relacao() {
        if (token.getClasse() == Classe.cIgual) {
            LerToken();
        }else if (token.getClasse() == Classe.cMaior) {
            LerToken();
        }else if (token.getClasse() == Classe.cMenor) {
            LerToken();
        }else if (token.getClasse() == Classe.cMaiorIgual) {
            LerToken();
        }else if (token.getClasse() == Classe.cMenorIgual) {
            LerToken();
        }else if (token.getClasse() == Classe.cDiferente) {
            LerToken();
        }else {
            mensagemErro(" -FALTOU O OPERADOR DE RELAÇÃO");
        }
    }

    public void expressao() {
        termo();
        outros_termos();
    }

    public void outros_termos() {
        if (token.getClasse() == Classe.cMais || token.getClasse() == Classe.cMenos) {
            op_ad();
            termo();
            outros_termos();
        }
    }

    public void op_ad() {
        if (token.getClasse() == Classe.cMais || token.getClasse() == Classe.cMenos) {
            LerToken();
        }else {
            mensagemErro(" - FALTOU COLOCAR O OPERADOR DE ADIÇÃO OU DE SUBTRAÇÃO");
        }
    }

    public void termo() {
        fator();
        mais_fatores();
    }


    public void mais_fatores() {
        if (token.getClasse() == Classe.cMultiplicacao || token.getClasse() == Classe.cDivisao) {
            op_mul();
            //{A11}
            fator();
            //{A12}
            mais_fatores();
        }
    }

    public void op_mul() {
        if (token.getClasse() == Classe.cMultiplicacao || token.getClasse() == Classe.cDivisao) {
            LerToken();
        }else {
            mensagemErro(" -FALTOU A MULTIPLICAÇÃO E DIVISÃO");
        }
    }


    public void fator() {
        if (token.getClasse() == Classe.cId) {
            LerToken();
            //{A7}
        }else if (token.getClasse() == Classe.cInt || token.getClasse() == Classe.cReal) {
            LerToken();
            //{A8}
        }else if (token.getClasse() == Classe.cParEsq){
            LerToken();
            expressao();
            if (token.getClasse() == Classe.cParDir){
                LerToken();
            }else {
                mensagemErro(" -FALTOU PARENTESE DIREITO");
            }
        }else {
            mensagemErro(" -FALTOU FATOR IN NUM EXP");
        }
    }

}
