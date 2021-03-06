package net.sf.saxon.style;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.UserFunctionCall;
import net.sf.saxon.functions.FunctionLibrary;
import net.sf.saxon.instruct.UserFunction;
import net.sf.saxon.instruct.UserFunctionParameter;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;

import java.util.Arrays;
import java.util.List;

/**
 * A StylesheetFunctionLibrary contains functions defined by the user in a stylesheet. This library is used at
 * compile time only, as it contains references to the actual XSLFunction objects. Binding to a function in this
 * library registers the function call on a fix-up list to be notified when the actual compiled function becomes
 * available.
 */

public class StylesheetFunctionLibrary implements FunctionLibrary {

    private XSLStylesheet stylesheet;
    private boolean overriding;

    /**
     * Create a FunctionLibrary that provides access to stylesheet functions
     * @param sheet The XSLStylesheet element of the principal stylesheet module
     * @param overriding set to true if this library is to contain functions specifying override="yes",
     * or to false if it is to contain functions specifying override="no". (XSLT uses two instances
     * of this class, one for overriding functions and one for non-overriding functions.)
     */
    public StylesheetFunctionLibrary(XSLStylesheet sheet, boolean overriding) {
        this.stylesheet = sheet;
        this.overriding = overriding;
    }

    /**
     * Test whether a function with a given name and arity is available; if so, return its signature.
     * This supports the function-available() function in XSLT; it is also used to support
     * higher-order functions introduced in XQuery 1.1.
     *
     * <p>This method may be called either at compile time
     * or at run time. If the function library is to be used only in an XQuery or free-standing XPath
     * environment, this method may throw an UnsupportedOperationException.</p>
     * @param functionName the qualified name of the function being called
     * @param arity        The number of arguments. This is set to -1 in the case of the single-argument
     *                     function-available() function; in this case the method should return true if there is some
     *                     function of this name available for calling.
     * @return if a function of this name and arity is available for calling, then the type signature of the
     * function, as an array of sequence types in which the zeroth entry represents the return type; or a zero-length
     * array if the function exists but the signature is not known; or null if the function does not exist
     */

    public SequenceType[] getFunctionSignature(StructuredQName functionName, int arity) {
        XSLFunction fn = stylesheet.getStylesheetFunction(functionName, arity);
        if (fn != null) {
            SequenceType[] sig = new SequenceType[arity+1];
            UserFunction uf = fn.getCompiledFunction();
            if (uf == null) {
                // XSLT: function not yet compiled
                Arrays.fill(sig, SequenceType.ANY_SEQUENCE);
            } else {
                UserFunctionParameter[] params = uf.getParameterDefinitions();
                sig[0] = uf.getResultType(stylesheet.getConfiguration().getTypeHierarchy());
                for (int i=0; i<params.length; i++) {
                    sig[i+1] = params[i].getRequiredType();
                }
            }
            return sig;            
        } else {
            return null;
        }
    }

    /**
     * Bind a function, given the URI and local parts of the function name,
     * and the list of expressions supplied as arguments. This method is called at compile
     * time.
     * @param functionName
     * @param staticArgs  The expressions supplied statically in the function call. The intention is
     * that the static type of the arguments (obtainable via getItemType() and getCardinality() may
     * be used as part of the binding algorithm.
     * @param env
     * @return An object representing the extension function to be called, if one is found;
     * null if no extension function was found matching the required name and arity.
     * @throws net.sf.saxon.trans.XPathException if a function is found with the required name and arity, but
     * the implementation of the function cannot be loaded or used; or if an error occurs
     * while searching for the function; or if this function library "owns" the namespace containing
     * the function call, but no function was found.
     */

    public Expression bind(StructuredQName functionName, Expression[] staticArgs, StaticContext env)
            throws XPathException {
        XSLFunction fn = stylesheet.getStylesheetFunction(functionName, staticArgs.length);
        if (fn==null) {
            return null;
        }
        if (fn.isOverriding() != overriding) {
            return null;
        }
        UserFunctionCall fc = new UserFunctionCall();
        fn.registerReference(fc);
        fc.setFunctionName(functionName);
        fc.setArguments(staticArgs);
        fc.setConfirmed(true);
        return fc;
    }

    /**
     * This method creates a copy of a FunctionLibrary: if the original FunctionLibrary allows
     * new functions to be added, then additions to this copy will not affect the original, or
     * vice versa.
     *
     * @return a copy of this function library. This must be an instance of the original class.
     */

    public FunctionLibrary copy() {
        return this;
    }

    /**
     * Get a list of all functions in this StylesheetFunctionLibrary
     */

    public List getAllFunctions() {
        return stylesheet.getAllStylesheetFunctions();
    }

}
//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//