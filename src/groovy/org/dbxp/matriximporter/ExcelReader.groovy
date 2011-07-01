package org.dbxp.matriximporter;

import java.io.File

import org.apache.poi.ss.usermodel.*
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator

/**
 * This class is capable of importing Excel (.xls and .xlsx) files
 * 
 * @author robert
 *
 */
public class ExcelReader implements MatrixReader {
	public ExcelReader() {}

	/** 
	 * Returns true if this class is able to parse
	 * the given file
	 * @param file	File object to read
	 * @return	True if this class can parse the given file, false otherwise
	 */
	public boolean canParse( File file ) {
		return ( file.getName().toLowerCase() =~ /\.xlsx?$/ ) as boolean;
	}

	/**
	 * Parses the given file and returns the matrix in that file
	 * @param file	File object to read
	 * @param hints	Hints for reading the excel file. Possible keys are:
	 * 			startRow	0-based row number of the first row to read. (1 means start reading from the second row)
	 * 						Defaults to the first row in the file
	 * 			endRow		0-based row number of the last row to read.	 (2 means the 3rd row is the last to read)
	 * 						Defaults to the last row in the file
	 * 			sheedIndx	0-based index of the excel sheet to be read
	 * 						Defaults to 0
	 * @return		Two-dimensional data matrix of structure:
	 * 				[ 
	 * 					[ 1, 3, 5 ] // First line
	 * 					[ 9, 1, 2 ] // Second line
	 * 				]
	 * 				The matrix must be rectangular, so all lines should contain
	 * 				the same number of values
	 */
	public def parse( File file, Map hints ) {
		def sheetIndex = hints.sheetIndex ?: 0;
		
		// Read the file with Apache POI 
		def workbook = WorkbookFactory.create( file.newInputStream() )
		def sheet = workbook.getSheetAt(sheetIndex)
		
		def df = new DataFormatter()
		def dataMatrix = []
		def formulaEvaluator = null

		// Is this an XLS (old fashioned Excel file)?
		try {
			formulaEvaluator = new HSSFFormulaEvaluator(sheet, workbook);
		} catch (Exception e) {
			log.error ".import wizard could not create Excel (XLS) formula evaluator, skipping to Excel XML (XLSX)"
		}

		// Or is this an XLSX (modern style Excel file)?
		if (formulaEvaluator==null) try {
			formulaEvaluator = new XSSFFormulaEvaluator(workbook);
		} catch (Exception e) {
			log.error ".import wizard could not create Excel XML (XLSX) formula evaluator either, unknown Excel formula format?"
		}

		// Determine start and end row numbers to be read
		def startRow = hints.startRow
		if( !startRow || startRow < sheet.firstRowNum || startRow > sheet.lastRowNum )
			startRow = sheet.firstRowNum
		
		def endRow = hints.endRow
		if( !endRow || endRow < sheet.firstRowNum || endRow > sheet.lastRowNum )
			endRow = sheet.lastRowNum

		// Determine amount of columns: the number of columns in the first row
		def columnCount = sheet.getRow(startRow)?.getLastCellNum()

		// Walk through all rows
		(startRow..endRow).each { rowIndex ->

			def dataMatrixRow = []

			// Get the current row
			def excelRow = sheet.getRow(rowIndex)

			// Excel contains some data?
			if (excelRow)
				columnCount.times { columnIndex ->

					// Read the cell, even is it a blank
					def cell = excelRow.getCell(columnIndex, Row.CREATE_NULL_AS_BLANK)
					// Set the cell type to string, this prevents any kind of formatting

					// It is a numeric cell?
					if (cell.cellType == Cell.CELL_TYPE_NUMERIC)
					// It isn't a date cell?
					if (!DateUtil.isCellDateFormatted(cell))
						cell.setCellType(Cell.CELL_TYPE_STRING)

					switch (cell.cellType) {
						case Cell.CELL_TYPE_STRING:     dataMatrixRow.add( cell.stringCellValue )
							break
						case Cell.CELL_TYPE_NUMERIC:    dataMatrixRow.add( df.formatCellValue(cell) )
							break
						case Cell.CELL_TYPE_FORMULA:    (cell != null) ? dataMatrixRow.add(formulaEvaluator.evaluateInCell(cell)) :
						dataMatrixRow.add('')
							break
						default:                        dataMatrixRow.add( '' )

					}
				}

			if ( dataMatrixRow.any{it} ) // is at least 1 of the cells non empty?
				dataMatrix.add(dataMatrixRow)
		}

		dataMatrix

	}
}
