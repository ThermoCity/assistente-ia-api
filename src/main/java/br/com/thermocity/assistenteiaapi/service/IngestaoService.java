package br.com.thermocity.assistenteiaapi.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
            Resource[] arquivos = new PathMatchingResourcePatternResolver().getResources("classpath:docs/*.md");
            TokenTextSplitter divisor = TokenTextSplitter.builder().build();

            List<Document> trechos = new ArrayList<>();
            for (Resource arquivo : arquivos) {
                TextReader leitor = new TextReader(arquivo);
                leitor.getCustomMetadata().put("source", arquivo.getFilename());
                trechos.addAll(divisor.apply(leitor.get()));
            }

            vectorStore.add(trechos);
            log.info("RAG: {} trechos carregados de {} guias oficiais", trechos.size(), arquivos.length);
        } catch (IOException | RuntimeException e) {
            log.error("RAG: não foi possível carregar os guias oficiais", e);
        }
    }
}