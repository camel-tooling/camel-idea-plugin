import org.apache.camel.builder.RouteBuilder;
public final class CompleteDirectEndpointName1TestData extends RouteBuilder {
    @Override
    public void configure() {
        from("direct:abcNew?param1=xxx")
                .to("direct:test");
        from("direct:def")
                .to("direct:abcNew")
                .to("direct:abcNew");
        from("direct:test")
                .to("direct:def");
    }
}