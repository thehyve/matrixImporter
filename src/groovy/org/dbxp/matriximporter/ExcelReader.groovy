package org.dbxp.matriximporter;

import java.io.File

import org.apache.poi.ss.usermodel.*

/**
 * This class is capable of importing Excel (.xls and .xlsx) files
 * 
 * @author robert
 *
 */
public class ExcelReader implements MatrixReader {
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
	 * @return		Two-dimensional data matrix of structure:
	 * 				[ 
	 * 					[ 1, 3, 5 ] // First line
	 * 					[ 9, 1, 2 ] // Second line
	 * 				]
	 * 				The matrix must be rectangular, so all lines should contain
	 * 				the same number of values
	 */
	public def parse( File file ) {
		def workbook = WorkbookFactory.create( file )
		def sheetIndex = 0;
		
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

		count = count ? Math.min(sheet.lastRowNum, count) : sheet.lastRowNum

		// Determine amount of columns
		def columnCount = sheet.getRow(sheet.getFirstRowNum())?.getLastCellNum()

		// Walk through all rows
		(sheet.firstRowNum..count).each { rowIndex ->

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
