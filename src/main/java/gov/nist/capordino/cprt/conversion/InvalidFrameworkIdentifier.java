package gov.nist.capordino.cprt.conversion;

public class InvalidFrameworkIdentifier extends Exception {
    public InvalidFrameworkIdentifier(String expected, String actual) {
        super("The framework identifier " + actual + " does not match the expected identifier " + expected);
    }
}
