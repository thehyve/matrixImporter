package matriximporter

import grails.test.GrailsUnitTestCase
import org.dbxp.matriximporter.ExcelReader

class MatrixImporterTests extends GrailsUnitTestCase {
    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testMockPPSH() {
        def excelReader = new ExcelReader()

        def matrix = excelReader.parse(new File('test_data/mock_PPSH.xlsx'))

        assert matrix.size == 14
        assert matrix[0].size == 154
    }
}
