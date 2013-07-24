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

package org.bibalex.util;
/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.MessageFormat;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * ErrorHandler that reports error to the specified output stream.
 * 
 * @author
 * 	Kohsuke Kawaguchi (kohsuke,kawaguchi@sun.com)
 */
public class StreamErrorReporter implements ErrorHandler {
    
    private final PrintStream out;
    
    public StreamErrorReporter( PrintStream o ) { this.out = o; }
    public StreamErrorReporter( OutputStream o ) { this(new PrintStream(o)); }
    
    public void warning(SAXParseException e) throws SAXException {
        print("[Warning]",e);
    }

    public void error(SAXParseException e) throws SAXException {
        print("[ERROR  ]",e);
    }

    public void fatalError(SAXParseException e) throws SAXException {
        print("[Fatal  ]",e);
    }

    private void print( String header, SAXParseException e ) {
        out.println(header+' '+e.getMessage());
        out.println(MessageFormat.format("   line {0} at {1}",
            new Object[]{
                Integer.toString(e.getLineNumber()),
                e.getSystemId()}));
    }
}

