import org.apache.camel.builder.RouteBuilder;

public class RouteB extends RouteBuilder {
    @Override
    public void configure() {
        from("direct:routeB")
                .to("direct:routeC");
    }
}