package backend.math.calculus.integral;

import backend.models.DefiniteIntegral;
import backend.models.Step;
import backend.parser.ASTNode;
import backend.parser.ASTNode.Num;

/** Shared definite-integral step builder for all integration operations. */
public final class IntegralNotation {

    private IntegralNotation() {}

    public static Step definiteIntegralStep(Num lower, Num upper, ASTNode integrand, String variable) {
        return definiteIntegralStep("Definite integral", lower, upper, integrand, variable);
    }

    public static Step definiteIntegralStep(
            String description, Num lower, Num upper, ASTNode integrand, String variable) {
        return Step.definiteIntegral(
                description,
                new DefiniteIntegral(
                        lower.toDisplay(),
                        upper.toDisplay(),
                        parenthesizeIfNeeded(integrand.toDisplay()),
                        variable));
    }

    static String parenthesizeIfNeeded(String integrand) {
        if (integrand.contains("+") || (integrand.contains("-") && !integrand.startsWith("-"))) {
            return "(" + integrand + ")";
        }
        return integrand;
    }
}
