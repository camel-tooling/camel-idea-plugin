import org.apache.camel.builder.RouteBuilder;
public final class CompleteDirectEndpointName1TestData extends RouteBuilder {
    @Override
    public void configure() {
        from("direct:a<caret>bc?param1=xxx")
                .to("direct:test");
        from("direct:def")
                .to("direct:abc")
                .to("direct:abc");
        from("direct:test")
                .to("direct:def");
    }
}