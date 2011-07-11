package org.dbxp.matriximporter

/**
 * This class is capable of importing CSV files (with , (comma), ; (semicolon) and tab as separators)
 * 
 * @author robert
 *
 */
public class CsvReader extends MatrixReader {

	/**
	 * Returns true if this class is able to parse files with a given name. This
     * is done by checking if the extension equals '.csv' or '.txt'. Also
     * returns true if fileName is null or ''.
     *
	 * @param file	File object to read
	 * @return true if the reader can parse the file, false otherwise
	 */
	public boolean canParse( Map hints = [:] ) {
		def fileName = hints.fileName
        return fileName ? fileName.matches(/.+\.(csv|txt)$/) : true
	}
	
	/**
	 * Returns a description for this reader
	 * @return	Human readable description
	 */
	public String getDescription() {
		return "Matrix importer for reading CSV files"
	}

	/**
	* Parses the given inputStream as a CSV file and returns the matrix in that inputStream.
	* 
	* @param inputStream	InputStream to parse.
	* @param hints		    Hints for reading the csv file. Possible keys are:
	* 			startRow	0-based row number of the first row to read. (1 means start reading from the second row)
	* 						Defaults to the first row in the file
	* 			endRow		0-based row number of the last row to read.	 (2 means the 3rd row is the last to read)
	* 						Defaults to the last row in the file
	* 			delimiter	Delimiter that is used between the different fields on a line. Common values are , ; or <tab>.
	* 						Defaults to the character , ; or <tab> that is most common in the file.
     * 			makeRowsEqualLength
     * 		                Pad lines shorter than longest line with empty strings so all lines will contain the same number of values
     *
	* @return		Two-dimensional data matrix of structure:
	* 				[
	* 					[ 1, 3, 5 ] // First line
	* 					[ 9, 1, 2 ] // Second line
	* 				]
	*/
	def parse( InputStream inputStream, Map hints ) {
		// Count the number of lines in the file
		def numLines = 0

//		inputStream.eachLine { numLines++ }
//        inputStream.reset() // start
	
		// Determine the start and end row if none is given
		def startRow = hints.startRow
		if( !startRow || startRow < 0 || startRow > numLines )
			startRow = 0
	
		def endRow = hints.endRow
		if( !endRow || endRow < startRow ) {
			endRow = Long.MAX_VALUE
		}

		String delimiter = hints.delimiter
		if( !delimiter ) {
			delimiter = determineDelimiterFromInput( inputStream, hints.threshold ?: 5 )
			
			// If no delimiter could be determined, raise an error
			if( !delimiter )
				throw new Exception( "CSV delimiter could not be automatically determined for inputStream." )
		}
	
		// Now loop through all rows, retrieving data from the file
		ArrayList data = []
		inputStream.eachLine(0) { String line, int lineNumber ->
			if( lineNumber >= startRow && lineNumber <= endRow ) {

                data << line.split( delimiter )

			}
		}

        // pad lines shorter than longest line with empty strings if requested
        if (hints.makeRowsEqualLength) {

            def maxSize = data*.size().max{ it }

            data.eachWithIndex{ line, i ->

                data[i] += [""] * (maxSize - line.size())

            }
        }
	
		return data
	}
	
	/**
	 * Tries to guess the delimiter used in this csv file. This is done by looking which 
	 * character from , ; and <tab> is most common.
	 * @param input	File to parse
	 * @return		Most probable delimiter. If none could be determined, null is given.
	 */
	protected String determineDelimiterFromInput( InputStream inputStream, int threshold ) {
		def delimiterCounts = [ ",": 0, ";": 0, "\t": 0 ]
		def possibleDelimiters = delimiterCounts.keySet().asList()

		// We stop counting after 10 lines, in order to speed up the process. If no apparent winner
		// is found after 10 lines, we won't find it by analysing the whole file
		def countLines = 10
		
		// We don't use inputStream.eachLine() here, since the closure that is called with that method
		// doesn't support break or continue statements.
        def r = new BufferedReader( new InputStreamReader( inputStream ) )
        def lineNumber = 0
        for( def line = r.readLine(); null != line; line = r.readLine() ) {

            // Count the number of occurrences of all delimiters
            possibleDelimiters.each { delimiter ->
                delimiterCounts[ delimiter ] += line.count( delimiter )
            }

            lineNumber++

            if( lineNumber >= countLines )
                break
        }
		
		// Determine the best delimiter. It is only returned if more than value
		// of 'threshold' of those characters have been found
		def bestDelimiter = null
		def bestCount = 0
		
		delimiterCounts.each { 
			if( it.value > bestCount && it.value >= threshold ) {
				bestCount = it.value
				bestDelimiter = it.key
			}
		}
		
		return bestDelimiter
	}

}
