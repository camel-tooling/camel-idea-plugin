import org.apache.camel.builder.RouteBuilder;

public class RouteA extends RouteBuilder {
    @Override
    public void configure() {
        from("direct:routeA")
                .to("direct:routeB");
    }
}