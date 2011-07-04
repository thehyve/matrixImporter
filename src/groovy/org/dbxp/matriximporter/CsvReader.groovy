package org.dbxp.matriximporter;

import java.io.File
import java.util.ArrayList;
import java.util.Map;

/**
 * This class is capable of importing CSV files (with , (comma), ; (semicolon) and tab as separators)
 * 
 * @author robert
 *
 */
public class CsvReader implements MatrixReader {
	public CsvReader() {}

	/** 
	 * Returns true if this class is able to parse
	 * the given file
	 * @param file	File object to read
	 * @return	True if this class can parse the given file, false otherwise
	 */
	public boolean canParse( File file ) {
		return ( file.getName().toLowerCase() =~ /\.(csv|txt)$/ ) as boolean;
	}

	/**
	 * Parses the given file and returns the matrix in that file
	 * @param file	File object to read
	 * @param hints	Hints for reading the csvfile. Possible keys are:
	 * 			startRow	0-based row number of the first row to read. (1 means start reading from the second row)
	 * 						Defaults to the first row in the file
	 * 			endRow		0-based row number of the last row to read.	 (2 means the 3rd row is the last to read)
	 * 						Defaults to the last row in the file
	 * 			delimiter	Delimiter that is used between the different fields on a line. Common values are , ; or <tab>.
	 * 						Defaults to the character , ; or <tab> that is most common in the file.
	 * @return		Two-dimensional data matrix of structure:
	 * 				[ 
	 * 					[ 1, 3, 5 ] // First line
	 * 					[ 9, 1, 2 ] // Second line
	 * 				]
	 * 				The matrix must be rectangular, so all lines should contain
	 * 				the same number of values
	 */
	public def parse( File file, Map hints ) {
		try {
			_parse( file, hints );
		} catch( Exception e ) {
			throw new Exception( "An error occured while parsing CSV file " + file?.getName(), e)
		}
	}
	
	/**
	 * Parses the given string as a CSV file and returns the matrix in that string
	 * @param contents	String to parse
	 * @param hints		Hints for reading the csvfile. Possible keys are:
	 * 			startRow	0-based row number of the first row to read. (1 means start reading from the second row)
	 * 						Defaults to the first row in the file
	 * 			endRow		0-based row number of the last row to read.	 (2 means the 3rd row is the last to read)
	 * 						Defaults to the last row in the file
	 * 			delimiter	Delimiter that is used between the different fields on a line. Common values are , ; or <tab>.
	 * 						Defaults to the character , ; or <tab> that is most common in the file.
	 * @return		Two-dimensional data matrix of structure:
	 * 				[ 
	 * 					[ 1, 3, 5 ] // First line
	 * 					[ 9, 1, 2 ] // Second line
	 * 				]
	 * 				The matrix must be rectangular, so all lines should contain
	 * 				the same number of values
	 */
	public def parse( String contents, Map hints ) {
		_parse( contents, hints );
	}
	
	/**
	 * Returns a description for this reader
	 * @return	Human readable description
	 */
	public String getDescription() {
		return "Matrix importer for reading CSV files"
	}

	/**
	* Parses the given flie or string as a CSV file and returns the matrix in that input. This method bundles the functionality
	* to parse files and strings. This can be done because both File and String objects contain an eachLine method to walk through the lines
	* 
	* @param input		File or String to parse.
	* @param hints		Hints for reading the csvfile. Possible keys are:
	* 			startRow	0-based row number of the first row to read. (1 means start reading from the second row)
	* 						Defaults to the first row in the file
	* 			endRow		0-based row number of the last row to read.	 (2 means the 3rd row is the last to read)
	* 						Defaults to the last row in the file
	* 			delimiter	Delimiter that is used between the different fields on a line. Common values are , ; or <tab>.
	* 						Defaults to the character , ; or <tab> that is most common in the file.
	* @return		Two-dimensional data matrix of structure:
	* 				[
	* 					[ 1, 3, 5 ] // First line
	* 					[ 9, 1, 2 ] // Second line
	* 				]
	* 				The matrix must be rectangular, so all lines should contain
	* 				the same number of values
	*/
	protected def _parse( def input, Map hints ) {
		if( !( input instanceof String) && !( input instanceof File ) ) {
			throw new Exception( "CSV reader can only parse files or strings. " + input?.class?.name + " given.")
		}
		
		// Count the number of lines in the file
		def numLines = 0
		input.eachLine { numLines++ }
	
		// Determine the start and end row if none is given
		def startRow = hints.startRow
		if( !startRow || startRow < 0 || startRow > numLines )
			startRow = 0;
	
		def endRow = hints.endRow
		if( !endRow || endRow < 0 || endRow >= numLines ) {
			endRow = numLines - 1;
		}
		
		// What treshold should be used when determining default delimiter
		def treshold = hints.treshold
		if( treshold == null )
			treshold = 0;
		
		def delimiter = hints.delimiter
		if( !delimiter ) {
			delimiter = determineDelimiterFromInput( input, treshold );
			
			// If no delimiter could be determined, raise an error
			if( !delimiter )
				throw new Exception( "CSV delimiter could not be automatically determined for input." )
		}
	
		// Now loop through all rows, retrieving data from the excel file
		ArrayList data = []
		def numColumns = 0;
		input.eachLine(0) { String line, int lineNumber ->
			if( lineNumber >= startRow && lineNumber <= endRow ) {
				def fields = line.split( delimiter );
				
				// The number of columns read is the number of fields on the first line we read
				if( numColumns == 0 )
					numColumns = fields.size();
					
				data << ( fields as List ) + ( [ "" ] * ( numColumns - fields.size() ) )
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
	protected String determineDelimiterFromInput( File input, int treshold = 5 ) {
		def delimiterCounts = [ ",": 0, ";": 0, "\t": 0 ];
		def possibleDelimiters = delimiterCounts.keySet().asList();

		// We stop counting after 10 lines, in order to speed up the process. If no apparent winner
		// is found after 10 lines, we won't find it by analysing the whole file
		def countLines = 10;
		
		// We don't use file.eachLine() here, since the closure that is called with that method
		// doesn't support break or continue statements.
		input.withInputStream { 
			def r = new BufferedReader( new InputStreamReader( it ) );
			def lineNumber = 0;
			for( def line = r.readLine(); null != line; line = r.readLine() ) {
				
				// Count the number of occurrences of all delimiters
				possibleDelimiters.each { delimiter ->
					delimiterCounts[ delimiter ] += line.count( delimiter )
				}
				
				lineNumber++;
				
				if( lineNumber >= countLines )
					break;
			}
		}
		
		// Determine the best delimiter. It is only returned if more than 5 of those characters have been
		// found
		def bestDelimiter = null;
		def bestCount = 0;
		
		delimiterCounts.each { 
			if( it.value > bestCount && it.value >= treshold ) {
				bestCount = it.value;
				bestDelimiter = it.key
			}
		}
		
		return bestDelimiter
	}
	
	/**
	* Tries to guess the delimiter used in this string. This is done by looking which
	* character from , ; and <tab> is most common.
	* @param 	input	String to parse
	* @return	Most probable delimiter. If none could be determined, null is given.
	*/
   protected String determineDelimiterFromInput( String input, int treshold = 5 ) {
	   def delimiterCounts = [ ",": 0, ";": 0, "\t": 0 ];
	   def possibleDelimiters = delimiterCounts.keySet().asList();

	   // Count the number of occurrences of all delimiters
	   possibleDelimiters.each { delimiter ->
		   delimiterCounts[ delimiter ] = input.count( delimiter )
	   }

	   // Determine the best delimiter. It is only returned if more than <treshold> of those characters have been
	   // found
	   def bestDelimiter = null;
	   def bestCount = 0;
	   
	   delimiterCounts.each {
		   if( it.value > bestCount && it.value >= treshold ) {
			   bestCount = it.value;
			   bestDelimiter = it.key
		   }
	   }
	   
	   return bestDelimiter
   }

}
