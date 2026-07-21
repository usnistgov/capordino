package gov.nist.capordino.cprt.conversion;

public class UnimplementedFrameworkIdentifier extends Exception {
    public UnimplementedFrameworkIdentifier(String identifier) {
        super("Conversion for the framework identifier " + identifier + " is not implemented yet.");
    }
}
