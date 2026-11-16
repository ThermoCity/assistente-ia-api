package br.com.thermocity.assistenteiaapi.service;

import br.com.thermocity.assistenteiaapi.dto.PerfilContato;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

// Parte "R" do RAG: busca os trechos dos guias oficiais mais parecidos com a situação da pessoa
@Service
public class BuscaGuiasService {

    private final VectorStore vectorStore;
    private final int quantidadeTrechos;

    public BuscaGuiasService(VectorStore vectorStore, @Value("${thermocity.rag.trechos:4}") int quantidadeTrechos) {
        this.vectorStore = vectorStore;
        this.quantidadeTrechos = quantidadeTrechos;
    }

    public List<Document> buscar(PerfilContato perfil) {
        return vectorStore.similaritySearch(SearchRequest.builder()
                .query(consulta(perfil))
                .topK(quantidadeTrechos)
                .build());
    }

    // A busca descreve a situação, sem nenhum dado que identifique a pessoa
    static String consulta(PerfilContato perfil) {
        StringBuilder texto = new StringBuilder("cuidados com pessoa idosa durante onda de calor extremo");
        if (perfil.moraSozinha()) texto.append(", idoso que mora sozinho");
        if (perfil.mobilidadeReduzida()) texto.append(", mobilidade reduzida");
        if (perfil.condicaoCronica()) texto.append(", doença crônica e medicamentos");
        if (perfil.nivelCalor() >= 3) texto.append(", local fresco ou refúgio nas horas mais quentes");
        texto.append(", sinais de alerta, quando procurar a unidade de saúde ou ligar 192");
        return texto.toString();
    }
}