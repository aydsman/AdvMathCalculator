package backend.models;

/**
 * Typeset definite integral: ∫ with bounds, integrand, and differential.
 */
public record DefiniteIntegral(String lower, String upper, String integrand, String variable) {}
