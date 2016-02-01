import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import adl_2daa.gen.encoder.TestDatabaseCreator;
import adl_2daa.gen.encoder.TestExpressionDecoder;
import adl_2daa.gen.encoder.TestExpressionEncoder;
import adl_2daa.gen.encoder.TestNestingEncoder;
import adl_2daa.gen.encoder.TestSequenceDecoder;
import adl_2daa.gen.encoder.TestSequenceEncoder;
import adl_2daa.gen.filter.TestDistinctEOBFilter;
import adl_2daa.gen.filter.TestSpawnMatchFilter;
import adl_2daa.gen.generator.TestASTComparator;
import adl_2daa.gen.generator.TestSequenceWrapper;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	TestDatabaseCreator.class,
	TestExpressionDecoder.class,
	TestExpressionEncoder.class,
	TestNestingEncoder.class,
	TestSequenceDecoder.class,
	TestSequenceEncoder.class,
	
	TestDistinctEOBFilter.class,
	TestSpawnMatchFilter.class,
	
	TestASTComparator.class,
	TestSequenceWrapper.class,
	})
public class TestSuite {

}
