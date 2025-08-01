import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

public class MultiLineFromDeclaration extends EndpointRouteBuilder {

    @Override
    public void configure() {
        from(sftp("sftp://url")
                .username("user")
                .password("pass")
                .noop(true)
                .idempotent(true)
                .delay(60000))
                .routeId("sftp-from-route")
                .log("Test");
    }

}