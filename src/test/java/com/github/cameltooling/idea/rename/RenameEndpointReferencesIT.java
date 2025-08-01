package com.github.cameltooling.idea.rename;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import org.jetbrains.annotations.Nullable;

public class RenameEndpointReferencesIT  extends CamelLightCodeInsightFixtureTestCaseIT {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/rename/endpoint";
    }

    @Override
    protected @Nullable String[] getMavenDependencies() {
        return new String[] {CAMEL_CORE_MODEL_MAVEN_ARTIFACT};
    }

    public void testRenameProducerInJava() {
        myFixture.configureByText("RenameInJava.java",
         """
            import org.apache.camel.builder.RouteBuilder;
            public final class CompleteDirectEndpointName1TestData extends RouteBuilder {
                @Override
                public void configure() {
                    from("direct:abc?param1=xxx")
                        .to("direct:<caret>test");
                    from("direct:def")
                        .to("direct:test");
                    from("direct:test")
                        .to("direct:def");
                }
            }
            """);
        myFixture.renameElementAtCaret("newTest");
        myFixture.checkResult( """
            import org.apache.camel.builder.RouteBuilder;
            public final class CompleteDirectEndpointName1TestData extends RouteBuilder {
                @Override
                public void configure() {
                    from("direct:abc?param1=xxx")
                        .to("direct:newTest");
                    from("direct:def")
                        .to("direct:newTest");
                    from("direct:newTest")
                        .to("direct:def");
                }
            }
            """);
    }

    public void testRenameConsumerInJava() {
        myFixture.configureByText("RenameInJava.java",  """
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
            """);
        myFixture.renameElementAtCaret("newAbc");
        myFixture.checkResult("""
            import org.apache.camel.builder.RouteBuilder;
            public final class CompleteDirectEndpointName1TestData extends RouteBuilder {
                @Override
                public void configure() {
                    from("direct:newAbc?param1=xxx")
                        .to("direct:test");
                    from("direct:def")
                        .to("direct:newAbc")
                        .to("direct:newAbc");
                    from("direct:test")
                        .to("direct:def");
                }
            }
            """);
    }

    public void testRenameInXml() {
        myFixture.configureByText("rename.xml",  """
            <camelContext xmlns="http://camel.apache.org/schema/blueprint">
              <route>
                <from uri="direct:a<caret>bc"/>
                <to uri="direct:test"/>
              </route>
              <route>
                <from uri="direct:test"/>
                <to uri="direct:abc"/>
                <to uri="direct:abc"/>
              </route>
            </camelContext>""");
        myFixture.renameElementAtCaret("newAbc");
        myFixture.checkResult("""
            <camelContext xmlns="http://camel.apache.org/schema/blueprint">
              <route>
                <from uri="direct:newAbc"/>
                <to uri="direct:test"/>
              </route>
              <route>
                <from uri="direct:test"/>
                <to uri="direct:newAbc"/>
                <to uri="direct:newAbc"/>
              </route>
            </camelContext>""");
    }

    public void testRenameInYaml() {
        myFixture.configureByText("rename.yaml",  """
            routes:
              - from:
                  uri: direct:a<caret>bc
                to:
                  uri: direct:test
              - from:
                  uri: direct:test
                to:
                  uri: "direct:abc"
                to:
                  uri: direct:abc
            """);
        myFixture.renameElementAtCaret("newAbc");
        myFixture.checkResult("""
            routes:
              - from:
                  uri: direct:newAbc
                to:
                  uri: direct:test
              - from:
                  uri: direct:test
                to:
                  uri: "direct:newAbc"
                to:
                  uri: direct:newAbc
            """);
    }

    public void testRenameAcrossDifferentFiles() {
        myFixture.configureByFiles("Route.java", "context.xml", "routes.yaml");
        myFixture.renameElementAtCaret("abcNew");
        myFixture.checkResultByFile("Route.java", "Route_after.java", true);
        myFixture.checkResultByFile("context.xml", "context_after.xml", true);
        myFixture.checkResultByFile("routes.yaml", "routes_after.yaml", true);
    }

}
