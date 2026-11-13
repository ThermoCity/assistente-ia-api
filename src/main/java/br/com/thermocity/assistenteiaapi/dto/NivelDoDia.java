package br.com.thermocity.assistenteiaapi.dto;

// Nível de calor da cidade hoje (NC1 a NC5) e o índice de calor máximo previsto
public record NivelDoDia(int nivel, Double icMaxC) {}
