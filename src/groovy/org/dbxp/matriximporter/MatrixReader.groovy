package org.dbxp.matriximporter;

import java.io.File;
import java.util.List;

/**
 * This interface describes the methods needed
 * to import data from a specific filetype.
 * 
 * Classes that implement this interface can be registered
 * to function by calling MatrixImporter.register(class)
 * 
 * @author robert
 *
 */
public interface MatrixReader {
	/** 
	 * Returns true if this class is able to parse
	 * the given file
	 * @param file	File object to read
	 * @return	True if this class can parse the given file, false otherwise
	 */
	public boolean canParse( File file );
	
	
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
	public def parse( File file );
}
