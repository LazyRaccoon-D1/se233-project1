package se233.project1;

import org.junit.jupiter.api.TestInstance;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({VectorSettingsTest.class, ColorAnalyzerTest.class, InputResolverTest.class, ImageLoaderTest.class,
        VectorizerTest.class, VectorizerMockTest.class, BatchTaskTest.class, ReflectionTest.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JUnitTestSuite {
}
