package org.dbxp.matriximporter

/**
 * This class is capable of importing CSV files (with , (comma), ; (semicolon) and tab as separators)
 * 
 * @author robert
 *
 */
public class CsvReader extends MatrixReader {

    private static final int readAheadLimit = 16384

	/**
	 * Returns true if this class is able to parse files with a given name. This
     * is done by checking if the extension equals '.csv' or '.txt'. Also
     * returns true if fileName is null or ''.
     *
	 * @param hints	Hints for reading the csvfile. Possible keys are:
	 * 			startRow	0-based row number of the first row to read. (1 means start reading from the second row)
	 * 						Defaults to the first row in the file
	 * 			endRow		0-based row number of the last row to read.	 (2 means the 3rd row is the last to read)
	 * 						Defaults to the last row in the file
	 * 			delimiter	Delimiter that is used between the different fields on a line. Common values are , ; or <tab>.
	 * 						Defaults to the character , ; or <tab> that is most common in the file.
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

        if (hints.endRow == null) hints.endRow = Integer.MAX_VALUE

        def startRow =  forceValueInRange(hints.startRow, 0, Integer.MAX_VALUE)
        def endRow =    forceValueInRange(hints.endRow, startRow, Integer.MAX_VALUE)

        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader( inputStream ) )

		String delimiter = hints.delimiter
		if( !delimiter ) {
			delimiter = determineDelimiterFromInput( bufferedReader, hints.threshold ?: 5 )
		}
	
		// Now loop through all rows, retrieving data from the file
		def dataMatrix = []
		bufferedReader.eachLine(0) { String line, int lineNumber ->
			if( lineNumber >= startRow && lineNumber <= endRow ) {

                dataMatrix << (line.split( delimiter ) as ArrayList)

			}
		}

        bufferedReader.close()

        // pad lines shorter than longest line with empty strings if requested
        if (hints.makeRowsEqualLength) {

            def maxSize = dataMatrix*.size().max{ it }

            dataMatrix.eachWithIndex{ line, i ->

                dataMatrix[i] += [""] * (maxSize - line.size())

            }
        }
	
		return dataMatrix
	}
	
	/**
	 * Tries to guess the delimiter used in this csv file. This is done by looking which 
	 * character from , ; and <tab> is most common.
	 * @param input	File to parse
	 * @return		Most probable delimiter. If none could be determined, null is given.
	 */
	protected char determineDelimiterFromInput( Reader reader, int threshold ) {


        def possibleDelimiters = [',' as char, ';' as char, '\t' as char]
        def delimiterCounts = [:]
        possibleDelimiters.each{delimiterCounts[it as int] = 0}

        reader.mark(readAheadLimit)

        // Read characters in a buffer
        char[] characterBuffer = new char[readAheadLimit - 1]
        int charactersRead  = reader.read((char[]) characterBuffer, 0, readAheadLimit - 1)

        reader.reset()

        // tally occurrences of the possible delimiters
        characterBuffer[0..charactersRead - 1].each { char c ->
            if (c in possibleDelimiters) delimiterCounts[c as int]++
        }
        
		// Determine the best delimiter. It is only returned if more than value
		// of 'threshold' of those characters have been found
		char bestDelimiter = 0
		def bestCount = 0

        possibleDelimiters.each {
            def count = delimiterCounts[it as int]

            if( count > bestCount && count >= threshold) {
                bestCount = count
                bestDelimiter = it
            }
        }

        if( !bestDelimiter )
            throw new Exception( "CSV delimiter could not be automatically determined for inputStream." )


		return bestDelimiter
	}
}