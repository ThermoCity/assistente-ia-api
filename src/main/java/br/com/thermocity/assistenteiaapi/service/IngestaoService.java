package br.com.thermocity.assistenteiaapi.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class IngestaoService {

    private final VectorStore vectorStore;

    public IngestaoService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    // carrega os guias oficiais (resources/docs/*.md) no vector store na subida da aplicação
    @PostConstruct
    public void carregarGuias() {
        try {
            Map<String, String> fontes = lerFontes();
            Resource[] arquivos = new PathMatchingResourcePatternResolver().getResources("classpath:docs/*.md");
            TokenTextSplitter divisor = TokenTextSplitter.builder().build();

            List<Document> trechos = new ArrayList<>();
            for (Resource arquivo : arquivos) {
                String nomeArquivo = arquivo.getFilename();
                String fonte = fontes.getOrDefault(nomeArquivo, nomeArquivo);

                TextReader leitor = new TextReader(arquivo);
                leitor.getCustomMetadata().put("source", nomeArquivo);
                leitor.getCustomMetadata().put("fonte", fonte);

                // cada trecho começa com a referência do guia, para o modelo saber de onde veio a informação
                for (Document trecho : divisor.apply(leitor.get())) {
                    trechos.add(new Document("[Fonte: " + fonte + "]\n" + trecho.getText(), trecho.getMetadata()));
                }
            }

            vectorStore.add(trechos);
            log.info("RAG: {} trechos carregados de {} guias oficiais", trechos.size(), arquivos.length);
        } catch (IOException | RuntimeException e) {
            log.error("RAG: não foi possível carregar os guias oficiais", e);
        }
    }

    // docs/fontes.csv (arquivo;titulo;instituicao;url;data) -> "titulo – instituicao (data)" por arquivo
    private Map<String, String> lerFontes() throws IOException {
        Map<String, String> fontes = new HashMap<>();
        Resource csv = new ClassPathResource("docs/fontes.csv");
        if (!csv.exists()) return fontes;

        try (BufferedReader leitor = new BufferedReader(new InputStreamReader(csv.getInputStream(), StandardCharsets.UTF_8))) {
            leitor.readLine(); // cabeçalho
            String linha;
            while ((linha = leitor.readLine()) != null) {
                String[] campos = linha.split(";", -1);
                if (campos.length >= 5) {
                    fontes.put(campos[0].trim(), campos[1].trim() + " – " + campos[2].trim() + " (" + campos[4].trim() + ")");
                }
            }
        }
        return fontes;
    }
}
