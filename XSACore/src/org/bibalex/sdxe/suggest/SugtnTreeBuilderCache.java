//   Copyright 2013 Bibliotheca Alexandrina, Wellcome Trust Library
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

package org.bibalex.sdxe.suggest;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;
import org.bibalex.sdxe.exception.SugtnException;
import org.bibalex.util.StreamErrorReporter;
import org.xml.sax.SAXException;

import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.parser.XSOMParser;
import com.sun.xml.xsom.util.DomAnnotationParserFactory;

/**
 * Cache for tree builder schemas
 * 
 * @author younos
 * 
 */

public class SugtnTreeBuilderCache {
	private static final Logger LOG = Logger.getLogger("org.bibalex.sdxe");
	
// private Map<String, SugtnTreeBuilder> sugtnBuilderCache = null;
	
// public SugtnTreeBuilderCache() {
// this.sugtnBuilderCache = Collections
// .synchronizedMap(new WeakHashMap<String, SugtnTreeBuilder>());
//		
// }
	
	// YA20100819 Cache only what needs Disk IO to prevent concurrency issue
	/**
	 * Map to cache only what needs Disk IO to prevent concurrency issue using schema path as key and XS Schema set as value
	 *  
	 */
	private Map<String, XSSchemaSet> schemaOMCache = Collections
			.synchronizedMap(new WeakHashMap<String, XSSchemaSet>());
	
	// END YA20100819
	
	/**
	 * 
	 * Creates suggested tree builder for a schema.
	 * 
	 * Looks up the schema path in the map. If it exists, it sends its schema object model to {@link SugtnTreeBuilder} 
	 * to construct tree builder.
	 * If it does not exist, schema object model is first created then inserted into schemaOMCache before sent to 
	 * {@link SugtnTreeBuilder} to construct tree builder.
	 * 
	 * @param schemaPath
	 * 			Path of the schema object model
	 * @return	{@link SugtnTreeBuilder}
	 * 			Object of tree builder using the schema given as parameter
	 * 
	 */
	public SugtnTreeBuilder builderForSchema(String schemaPath) {
		SugtnTreeBuilder result = null;
// synchronized (this.sugtnBuilderCache) {
//			
// if (this.sugtnBuilderCache.containsKey(schemaPath)) {
//				
// result = this.sugtnBuilderCache.get(schemaPath);
//				
// } else {
//		
		try {
			XSSchemaSet xss;
			synchronized (this.schemaOMCache) {
				
				if (this.schemaOMCache.containsKey(schemaPath)) {
					
					xss = this.schemaOMCache.get(schemaPath);
					
				} else {
					
					XSOMParser reader = new XSOMParser();
					
					// set an error handler so that you can receive error messages
					reader.setErrorHandler(new StreamErrorReporter(System.out));
					// DomAnnotationParserFactory is a convenient default to use
					reader.setAnnotationParser(new DomAnnotationParserFactory());
					
					reader.parse(new File(schemaPath));
					
					xss = reader.getResult();
					
					if (xss == null) {
						throw new SugtnException("No Schemas Set was returned by parser!");
					}
					
					this.schemaOMCache.put(schemaPath, xss);
					
				}
// this.sugtnBuilderCache.put(schemaPath, result);
				
			}
			
			result = new SugtnTreeBuilder(xss);
			
// }
// }
		} catch (SugtnException e) {
			LOG.fatal(e, e);
		} catch (SAXException e) {
			LOG.fatal(e, e);
		} catch (IOException e) {
			LOG.fatal(e, e);
		}
		return result;
	}
}
