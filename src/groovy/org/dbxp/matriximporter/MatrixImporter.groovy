package org.dbxp.matriximporter

import org.apache.commons.logging.LogFactory

/**
 * Singleton class to access the import methods for matrix imports. Use the importFile method for reading files.
 * 
 *  	MatrixImporter.getInstance().importFile( new File( '/tmp/temporaryfile' ) )
 *
 *  Also available are: 'importString', 'importByteArray', and 'importInputStream'.
 *  
 *  Currently, excel files (.xls and .xlsx) and csv files (comma, tab or semicolon delimited) are supported.
 *  You can use the register() and unregister() methods to add or remove readers for specific file types
 *  
 *  	// Create a reader implementing the MatrixReader interface. Also implemented the required methods	
 *  	class MyTypeReader implements MatrixReader { ... }
 *  	
 *  	// Register a reader for your own filetype	
 *  	def myTypeReader = new MyTypeReader()
 *  	MatrixImporter.getInstance().register( myTypeReader )
 *  
 *  	// Import the file
 *  	MatrixImporter.getInstance().importFile( new File( '/tmp/myTypeFile' ) )
 *  
 *  	// If needed
 *  	MatrixImporter.getInstance().unregister( MyTypeReader.class )
 *  	// or
 *  	MatrixImporter.getInstance().unregister( myTypeReader )
 *  
 * @author Robert Horlings
 *
 */
class MatrixImporter {

    private static def log = LogFactory.getLog(this)

	// Singleton instance
	private static MatrixImporter _instance = null

	// List of registered readers
	private List<MatrixReader> readers = []

    /**
     *
     * @param file
     * @param hints
     * @return
     */
	public importFile( File file, Map hints = [:] ) {
        importInputStream(file.newInputStream(), hints + [fileName: file.name])
    }

    /**
     *
     * @param string
     * @param hints
     * @return
     */
    public importString( String string, Map hints  = [:] ) {
        importInputStream( new ByteArrayInputStream(string.getBytes("UTF-8")) , hints)
    }

    /**
     *
     * @param bytes
     * @param hints
     * @return
     */
    public importByteArray( byte[] bytes, Map hints  = [:] ) {
        importInputStream(new ByteArrayInputStream(bytes), hints)
    }

	/**
	 * Imports a file using an existing MatrixReader. If no reader is found that
	 * is able to parse the file, null is returned.
	 * @param file	File to read
     * @param hints	Map with hints for the reader. The value corresponding to the key 'fileName' might be used
     *              by the readers to determine whether they can parse the file.
     *              Might also include keys like 'startRow', 'endRow' and 'sheet'.
     * 				Readers implementing this interface may or may not listen to the hints given. See the documentation
     * 				of different implementing classes.
	 * @return		Two-dimensional data matrix with the contents of the file. The matrix has the structure:
	 * 				[ 
	 * 					[ 1, 3, 5 ] // First line
	 * 					[ 9, 1, 2 ] // Second line
	 * 				]
	 */
	public importInputStream( InputStream inputStream ) {
        importInputStream( inputStream, [:], false)
    }
    public importInputStream( InputStream inputStream, Map hints ) {
            importInputStream( inputStream, hints, false)
    }
	public importInputStream( InputStream inputStream, Map hints, Boolean returnInfo) {
		// Set a mark on the beginning of this inputstream, in order
		// to be able to reset this stream when start reading with a new reader
		if( inputStream.markSupported() )
			inputStream.mark( Integer.MAX_VALUE );

		// Loop through all registered readers, and parse the file using
		// the first reader that is able to parse the file.
		for( reader in readers ) {
			if( reader.canParse( hints ) ) {
                def matrix, parseInfo

                try {
                    // Reset inputStream, since it may have been used by other readers
					if( inputStream.markSupported() ) {
						inputStream.reset();
					}
                    
                    (matrix, parseInfo) = reader.parse( inputStream, hints )
                } catch (e) {
                    // we take it an exception means this reader was unable to parse
                    // the input.
                    log.info('Unable to parse using reader: ' + reader.class + ' with description: ' + reader.description + '.', e)
                }

                // Only return the value if the file has been correctly parsed
                // (i.e. the structure != null). Otherwise, we should try to parse
                // the file using another reader (if applicable)
                if( matrix ) {
                    return returnInfo ? [matrix, [readerClassName: reader.class.simpleName] + parseInfo] : matrix
                }
			}
		}

		// If parsing didn't work out, return null
		returnInfo ? [null, null] : null
	}

	/**
	 * Registers a reader with the importer, so the reader can be used to parse files
	 * @param c	Object that implements MatrixReader
	 */
	public void registerReader( MatrixReader c ) {
		if( c instanceof MatrixReader && !( c in readers ) )
		readers << c
	}

	/**
	 * Removes a reader from the importer, so the reader will not be used to parse files
	 * @param c		Class that implements MatrixReader
	 */
	public void unregisterReader( MatrixReader c ) {
		readers = readers.findAll { !( it.is( c ) ) }
	}

	/**
	 * Removes a reader from the importer, so the reader will not be used to parse files
	 * @param c		Class that implements MatrixReader 
	 */
	public void unregisterReader( Class c ) {
		readers = readers.findAll { !( it.class == c ) }
	}
	
	/**
	 * Returns a list of readers that have been registered
	 * @return	List with MatrixReader objects
	 */
	public List<MatrixReader> getReaders() {
		return [] + readers
	}

	/**
	 * Returns the singleton instance of the MatrixImporter
	 * @return
	 */
	public static MatrixImporter getInstance() {
		if( _instance == null )
		_instance = new MatrixImporter()

		return _instance
	}
	
	
	// Private constructor in order to facilitate the singleton pattern
	private MatrixImporter() {
		registerReader( new ExcelReader() )
		registerReader( new CsvReader() )
	}

}
