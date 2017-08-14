/*
 * Licence
 * Copyright (c) 2004, Wojciech Gradkowski
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the distribution
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.jmatio.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jmatio.types.MLArray;
import com.jmatio.types.MLCell;
import com.jmatio.types.MLChar;
import com.jmatio.types.MLNumericArray;
import com.jmatio.types.MLObject;
import com.jmatio.types.MLStructure;

/**
 * The JMatIO query parser. Allows to use Matlab-like syntax to access {@link MLArray} objects. 
 * <p>
 * 
 * @author wgradkowski
 *
 */
public class MLArrayQuery
{
    private String queryString;
    
    private static final String regexp = "([a-zA-Z0-9]+)(\\(([0-9]+|:)(,([0-9:]+|:))?\\))?\\.?";
    private static final Pattern pat = Pattern.compile( regexp );
    
    public MLArrayQuery( String queryString )
    {
        if ( !Pattern.matches( "^(" + regexp + ")+$", queryString ) )
        {
            throw new IllegalArgumentException();
        }
        
        this.queryString = queryString;
    }
    
    /**
     * 
     * 
     * @param array
     * @param query
     * @return
     */
    public static Object q( MLArray array, String query )
    {
        MLArrayQuery q = new MLArrayQuery( query );
        
        return q.query( array );
    }
    
    /**
     * Parses the query string and returns the object it refers to.
     * 
     * @param array
     *            source {@link MLArray}
     * @return query result
     */
    public Object query( MLArray array )
    {
        Matcher mat = pat.matcher( queryString );
        
        MLArray current = null;
        
        int prevM = 0;
        int prevN = 0;
        
        while ( mat.find() )
        {
            String name   = mat.group( 1 );
            String rangeM = mat.group( 3 );
            String rangeN = mat.group( 5 );
            
            int m = rangeM != null ? Integer.parseInt( rangeM ) -1 : -1;
            int n = rangeN != null ? Integer.parseInt( rangeN ) -1 : -1;
            
            if ( current == null )
            {
                current = array;
                
                if ( !current.getName().equals( name ) && !current.getName().equals( "@" ) )
                {
                    throw new RuntimeException("No such array or field <" + name + "> in <" + (current != null ? current.getName() : "/") + ">" );
                }
                
                prevM = m;
                prevN = n;
                
                continue;
            }
            
            int type = current.getType();
            
            switch ( type )
            {
                case MLArray.mxOBJECT_CLASS:
                    {
                        MLObject object = (MLObject) current;
                    
                        MLArray field  = object.getObject().getField( name, prevM, prevN );
                        
                        if ( field == null )
                        {
                            throw new RuntimeException("no such field: " + name );
                        }
                        current = field;
                    }
                    break;
                case MLArray.mxSTRUCT_CLASS:
                    {
                        MLStructure struct = (MLStructure) current;
                        
                        MLArray field  = struct.getField( name, prevM > 0 ? prevM : 0, prevN > 0 ? prevN : 0 );
                        
                        if ( field == null )
                        {
                            throw new RuntimeException("no such field: " + name );
                        }
                        current = field;
                    }
                    break;
                case MLArray.mxCELL_CLASS:
                    {
                        MLCell mlcell = (MLCell) current;
                        if ( m > -1 && n > -1 )
                        {
                            current = mlcell.get( m, n );
                        }
                        else if ( m > -1 )
                        {
                            current = mlcell.get( m );
                        }
                        else
                        {
                            throw new RuntimeException();
                        }
                    }
                    break;
                default:
            }
            
            prevM = m;
            prevN = n;
        }
        
        return getContent(current, prevM, prevN );
    }

    /**
     * Returns the content of the field/cell/object.
     * 
     * @param array
     *            the parent structure/cell
     * @param m
     *            column or -1
     * @param n
     *            row or -1
     * @return if both m and n are -1, returns {@link MLArray}, if n is -1, returns
     *         content under index m, if both m and n are not-negative, returns
     *         content of (m,n)
     */
    public Object getContent( MLArray array, int m, int n )
    {
        int type = array.getType();
        
        Object result = null;
        
        switch ( type )
        {
            case MLArray.mxINT8_CLASS:
            case MLArray.mxINT16_CLASS:
            case MLArray.mxINT32_CLASS:
            case MLArray.mxINT64_CLASS:
            case MLArray.mxUINT8_CLASS:
            case MLArray.mxUINT16_CLASS:
            case MLArray.mxUINT32_CLASS:
            case MLArray.mxUINT64_CLASS:
            case MLArray.mxSINGLE_CLASS:
            case MLArray.mxDOUBLE_CLASS:
                MLNumericArray<?> numeric = (MLNumericArray<?>) array;
                if ( m > -1 && n > -1 )
                {
                    result = numeric.get( m, n );
                }
                else if ( m > -1 )
                {
                    result = numeric.get( m );
                }
                else
                {
                    result = array;
                }
                break;
            case MLArray.mxCHAR_CLASS:
                MLChar mlchar = (MLChar) array;
                if ( m > -1 && n > -1 )
                {
                    result = mlchar.getChar( m, n );
                }
                else if ( m > -1 )
                {
                    result = mlchar.getString( m );
                }
                else
                {
                    result = mlchar;
                }
                break;
            case MLArray.mxCELL_CLASS:
                MLCell mlcell = (MLCell) array;
                if ( m > -1 && n > -1 )
                {
                    result = getContent( mlcell.get( m, n ), 0, -1);
                }
                else if ( m > -1 )
                {
                    result = getContent( mlcell.get( m ), 0, -1 );
                }
                else
                {
                    result = getContent( mlcell.get( 0 ), -1, -1 );
                }
                break;
            default:
                result = array;
        }
        
        return result;
    }
    

    
}
