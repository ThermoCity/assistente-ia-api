package br.com.thermocity.assistenteiaapi.service;

import br.com.thermocity.assistenteiaapi.dto.PerfilContato;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Orientação fixa por nível de calor, usada quando o modelo de IA falha ("gerada sem IA").
// O texto segue as páginas do Ministério da Saúde e o Informe Técnico 03/2025 da SES-RJ.
public final class OrientacaoPadrao {

    public static final String AVISO = "Esta orientação apoia o contato e não é diagnóstico. "
            + "Em emergência (pele muito quente, confusão mental, desmaio ou convulsão), ligue 192 (SAMU).";

    private static final String[] NOMES = {"Normal", "Atenção", "Alerta", "Alto risco", "Emergência"};

    public static final List<String> SINAIS_ALERTA = List.of(
            "Pele muito quente, confusão mental, desmaio ou convulsão: ligar 192 (SAMU) imediatamente.",
            "Tontura, fraqueza, náusea, dor de cabeça, cãibras ou transpiração excessiva: orientar a procurar a UBS.",
            "Urina escura, boca seca ou tontura ao levantar (sinais de desidratação): comunicar o enfermeiro da equipe.");

    public static final List<String> FONTES = List.of(
            "Ondas de calor (Saúde de A a Z) – Ministério da Saúde",
            "Insolação (Saúde de A a Z) – Ministério da Saúde",
            "Informe Técnico nº 03/2025 – Calor extremo e visitas domiciliares – Secretaria de Estado de Saúde do Rio de Janeiro");

    private OrientacaoPadrao() {
    }

  public static String distancia(Integer metros) {
        if (metros == null) return "";
        if (metros < 1000) return metros + " m";
        return String.format(Locale.forLanguageTag("pt-BR"), "%.1f km", metros / 1000.0);
    }

    public static String nomeDoNivel(int nivel) {
        return nivel >= 1 && nivel <= 5 ? NOMES[nivel - 1] : "Desconhecido";
    }

    // Os passos mudam conforme o nível de calor e as vulnerabilidades da pessoa
    public static List<String> roteiro(PerfilContato perfil) {
        List<String> passos = new ArrayList<>();
        passos.add("Apresente-se como agente da " + (perfil.ubsNome() == null ? "UBS" : perfil.ubsNome()) + " e explique que está ligando por causa do calor de hoje (NC"
                + perfil.nivelCalor() + " - " + nomeDoNivel(perfil.nivelCalor()) + ").");
        passos.add("Pergunte como a pessoa está se sentindo hoje e se teve algum dos sinais de alerta.");
        passos.add("Pergunte se está bebendo água ao longo do dia e lembre de beber mesmo sem sede, "
                + "exceto se o médico tiver orientado restrição.");
        passos.add("Oriente evitar o sol entre 10h e 16h e manter a casa ventilada.");
        if (perfil.condicaoCronica()) {
            passos.add("Pergunte sobre os remédios de uso contínuo e oriente não parar nem mudar a dose por conta própria.");
        }
        if (perfil.moraSozinha()) {
            passos.add("Combine quem pode verificar como a pessoa está ao longo do dia (familiar ou vizinho).");
        }
        if (perfil.mobilidadeReduzida()) {
            passos.add("Pergunte se há alguém para ajudar a pessoa a ir até um lugar fresco, se precisar.");
        }
        if (perfil.nivelCalor() >= 3 && perfil.refugioNome() != null) {
            passos.add("Indique o refúgio mais próximo para as horas mais quentes: " + perfil.refugioNome()
                    + (perfil.refugioDistanciaMetros() == null ? "." : ", a " + distancia(perfil.refugioDistanciaMetros()) + "."));
        }
        if (perfil.nivelCalor() >= 4) {
            passos.add("Se houver qualquer sinal de alerta ou a pessoa estiver sem apoio, comunique o enfermeiro "
                    + "da equipe para avaliar uma visita ainda hoje.");
        }
        passos.add("Registre o resultado do contato.");
        return passos;
    }
}