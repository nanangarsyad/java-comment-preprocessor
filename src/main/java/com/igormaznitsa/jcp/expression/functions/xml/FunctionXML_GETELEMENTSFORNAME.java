/*
 * Copyright 2011 Igor Maznitsa (http://www.igormaznitsa.com)
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of version 3 of the GNU Lesser General Public
 * License as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307  USA
 */
package com.igormaznitsa.jcp.expression.functions.xml;

import com.igormaznitsa.jcp.context.PreprocessorContext;
import com.igormaznitsa.jcp.expression.Value;
import com.igormaznitsa.jcp.expression.ValueType;
import com.igormaznitsa.jcp.expression.functions.AbstractFunction;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The class implements the xml_getelementsforname function handler
 * 
 * @author Igor Maznitsa (igor.maznitsa@igormaznitsa.com)
 */
public final class FunctionXML_GETELEMENTSFORNAME extends AbstractFunction {

    private static final ValueType[][] ARG_TYPES = new ValueType[][]{{ValueType.STRING,ValueType.STRING}};
    
    @Override
    public String getName() {
        return "xml_getelementsforname";
    }

    public Value executeStrStr(final PreprocessorContext context, final Value elementId, final Value name) {
        final String elementIdStr = elementId.asString();
        final String nameStr = name.asString();

        final String nodeListId = elementIdStr + "_nodelist_" + name;
        final Value result = Value.valueOf(nodeListId);

        NodeContainer container = (NodeContainer) context.getSharedResource(nodeListId);
        if (container == null) {
            container = (NodeContainer) context.getSharedResource(elementIdStr);
            if (container == null) {
                throw new IllegalArgumentException("Can't find any element for the \'" + elementIdStr + "\' id");
            }
            final Element element = (Element) container.getNode();
            final NodeList list = element.getElementsByTagName(nameStr);
            container = new NodeContainer(UID_COUNTER.getAndIncrement(), list);
            context.setSharedResource(nodeListId, container);
        }

        return result;
    }

    @Override
    public int getArity() {
        return 2;
    }

    @Override
    public ValueType[][] getAllowedArgumentTypes() {
        return ARG_TYPES;
    }

    @Override
    public String getReference() {
        return "it makes a node list from element children with the name and return the list id";
    }

    @Override
    public ValueType getResultType() {
        return ValueType.STRING;
    }
}