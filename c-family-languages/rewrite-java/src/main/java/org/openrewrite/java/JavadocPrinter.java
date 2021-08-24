/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java;

import org.openrewrite.PrintOutputCapture;
import org.openrewrite.family.c.CVisitor;
import org.openrewrite.family.c.tree.C;
import org.openrewrite.family.c.tree.CContainer;
import org.openrewrite.family.c.tree.CLeftPadded;
import org.openrewrite.family.c.tree.Space;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Javadoc;

import java.util.List;

public class JavadocPrinter<P> extends JavadocVisitor<PrintOutputCapture<P>> {
    @Override
    public Javadoc visitAttribute(Javadoc.Attribute attribute, PrintOutputCapture<P> p) {
        visitMarkers(attribute.getMarkers(), p);
        p.out.append(attribute.getPrefix()).append(attribute.getName());
        switch (attribute.getKind()) {
            case Empty:
                break;
            case Unquoted:
                p.out.append('=');
                visit(attribute.getValue(), p);
                break;
            case SingleQuoted:
                p.out.append('=').append("'");
                visit(attribute.getValue(), p);
                p.out.append("'");
                break;
            case DoubleQuoted:
                p.out.append('=').append('"');
                visit(attribute.getValue(), p);
                p.out.append('"');
                break;
        }
        return attribute;
    }

    @Override
    public Javadoc visitAuthor(Javadoc.Author author, PrintOutputCapture<P> p) {
        visitMarkers(author.getMarkers(), p);
        p.out.append(author.getPrefix()).append("@author");
        visit(author.getName(), p);
        return author;
    }

    @Override
    public Javadoc visitDeprecated(Javadoc.Deprecated deprecated, PrintOutputCapture<P> p) {
        visitMarkers(deprecated.getMarkers(), p);
        p.out.append(deprecated.getPrefix()).append("@deprecated");
        visit(deprecated.getDescription(), p);
        return deprecated;
    }

    @Override
    public Javadoc visitDocComment(Javadoc.DocComment javadoc, PrintOutputCapture<P> p) {
        visitMarkers(javadoc.getMarkers(), p);
        p.out.append("/**");
        visit(javadoc.getBody(), p);
        p.out.append("*/");
        return javadoc;
    }

    @Override
    public Javadoc visitDocRoot(Javadoc.DocRoot docRoot, PrintOutputCapture<P> p) {
        visitMarkers(docRoot.getMarkers(), p);
        p.out.append(docRoot.getPrefix()).append("{@docRoot")
                .append(docRoot.getBeforeEndBrace()).append('}');
        return docRoot;
    }

    @Override
    public Javadoc visitDocType(Javadoc.DocType docType, PrintOutputCapture<P> p) {
        visitMarkers(docType.getMarkers(), p);
        p.out.append(docType.getPrefix()).append("<!doctype")
                .append(docType.getText()).append('>');
        return docType;
    }

    @Override
    public Javadoc visitEndElement(Javadoc.EndElement endElement, PrintOutputCapture<P> p) {
        visitMarkers(endElement.getMarkers(), p);
        p.out.append(endElement.getPrefix()).append("</").append(endElement.getName())
                .append(endElement.getBeforeEndBracket()).append('>');
        return endElement;
    }

    @Override
    public Javadoc visitHidden(Javadoc.Hidden hidden, PrintOutputCapture<P> p) {
        visitMarkers(hidden.getMarkers(), p);
        p.out.append(hidden.getPrefix()).append("@hidden");
        visit(hidden.getBody(), p);
        return hidden;
    }

    @Override
    public Javadoc visitIndex(Javadoc.Index index, PrintOutputCapture<P> p) {
        visitMarkers(index.getMarkers(), p);
        p.out.append(index.getPrefix()).append("{@index");
        visit(index.getSearchTerm(), p);
        visit(index.getDescription(), p);
        p.out.append('}');
        return index;
    }

    @Override
    public Javadoc visitInheritDoc(Javadoc.InheritDoc inheritDoc, PrintOutputCapture<P> p) {
        visitMarkers(inheritDoc.getMarkers(), p);
        p.out.append(inheritDoc.getPrefix()).append("{@inheritDoc")
                .append(inheritDoc.getBeforeEndBrace()).append('}');
        return inheritDoc;
    }

    @Override
    public Javadoc visitInlinedValue(Javadoc.InlinedValue value, PrintOutputCapture<P> p) {
        visitMarkers(value.getMarkers(), p);
        p.out.append(value.getPrefix()).append("{@value");
        javaVisitor.visit(value.getTree(), p);
        p.out.append(value.getBeforeEndBrace()).append('}');
        return value;
    }

    @Override
    public Javadoc visitLineBreak(Javadoc.LineBreak lineBreak, PrintOutputCapture<P> p) {
        visitMarkers(lineBreak.getMarkers(), p);
        p.out.append('\n').append(lineBreak.getMargin());
        return lineBreak;
    }

    @Override
    public Javadoc visitLink(Javadoc.Link link, PrintOutputCapture<P> p) {
        visitMarkers(link.getMarkers(), p);
        p.out.append(link.getPrefix());
        p.out.append(link.isPlain() ? "{@linkplain" : "{@link");
        javaVisitor.visit(link.getTree(), p);
        p.out.append(link.getBeforeEndBrace()).append('}');
        return link;
    }

    @Override
    public Javadoc visitLiteral(Javadoc.Literal literal, PrintOutputCapture<P> p) {
        visitMarkers(literal.getMarkers(), p);
        p.out.append(literal.getPrefix()).append(literal.isCode() ? "{@code" : "{@link");
        visit(literal.getBody(), p);
        p.out.append("}");
        return literal;
    }

    @Override
    public Javadoc visitParameter(Javadoc.Parameter parameter, PrintOutputCapture<P> p) {
        visitMarkers(parameter.getMarkers(), p);
        p.out.append(parameter.getPrefix()).append("@param");
        javaVisitor.visit(parameter.getName(), p);
        visit(parameter.getDescription(), p);
        return parameter;
    }

    @Override
    public Javadoc visitProvides(Javadoc.Provides provides, PrintOutputCapture<P> p) {
        visitMarkers(provides.getMarkers(), p);
        p.out.append(provides.getPrefix()).append("@provides");
        javaVisitor.visit(provides.getServiceType(), p);
        visit(provides.getDescription(), p);
        return provides;
    }

    @Override
    public Javadoc visitReturn(Javadoc.Return aReturn, PrintOutputCapture<P> p) {
        visitMarkers(aReturn.getMarkers(), p);
        p.out.append(aReturn.getPrefix()).append("@return");
        visit(aReturn.getDescription(), p);
        return aReturn;
    }

    @Override
    public Javadoc visitSerial(Javadoc.Serial serial, PrintOutputCapture<P> p) {
        visitMarkers(serial.getMarkers(), p);
        p.out.append(serial.getPrefix()).append("@serial");
        visit(serial.getDescription(), p);
        return serial;
    }

    @Override
    public Javadoc visitSerialData(Javadoc.SerialData serialData, PrintOutputCapture<P> p) {
        visitMarkers(serialData.getMarkers(), p);
        p.out.append(serialData.getPrefix()).append("@serialData");
        visit(serialData.getDescription(), p);
        return serialData;
    }

    @Override
    public Javadoc visitSerialField(Javadoc.SerialField serialField, PrintOutputCapture<P> p) {
        visitMarkers(serialField.getMarkers(), p);
        p.out.append(serialField.getPrefix()).append("@serialField");
        javaVisitor.visit(serialField.getName(), p);
        javaVisitor.visit(serialField.getType(), p);
        visit(serialField.getDescription(), p);
        return serialField;
    }

    @Override
    public Javadoc visitSince(Javadoc.Since since, PrintOutputCapture<P> p) {
        visitMarkers(since.getMarkers(), p);
        p.out.append(since.getPrefix()).append("@since");
        visit(since.getDescription(), p);
        return since;
    }

    @Override
    public Javadoc visitStartElement(Javadoc.StartElement startElement, PrintOutputCapture<P> p) {
        visitMarkers(startElement.getMarkers(), p);
        p.out.append('<').append(startElement.getName());
        visit(startElement.getAttributes(), p);
        p.out.append(startElement.getBeforeEndBracket());
        if (startElement.isSelfClosing()) {
            p.out.append('/');
        }
        p.out.append('>');
        return startElement;
    }

    @Override
    public Javadoc visitSummary(Javadoc.Summary summary, PrintOutputCapture<P> p) {
        visitMarkers(summary.getMarkers(), p);
        p.out.append(summary.getPrefix()).append("{@summary");
        visit(summary.getSummary(), p);
        p.out.append('}');
        return summary;
    }

    @Override
    public Javadoc visitText(Javadoc.Text text, PrintOutputCapture<P> p) {
        visitMarkers(text.getMarkers(), p);
        p.out.append(text.getText());
        visit(text.getLineBreak(), p);
        visit(text.getNext(), p);
        return text;
    }

    @Override
    public Javadoc visitThrows(Javadoc.Throws aThrows, PrintOutputCapture<P> p) {
        visitMarkers(aThrows.getMarkers(), p);
        p.out.append(aThrows.getPrefix()).append(aThrows.isThrowsKeyword() ? "@throws" : "@exception");
        javaVisitor.visit(aThrows.getExceptionName(), p);
        visit(aThrows.getDescription(), p);
        return aThrows;
    }

    @Override
    public Javadoc visitUnknownBlock(Javadoc.UnknownBlock unknownBlock, PrintOutputCapture<P> p) {
        visitMarkers(unknownBlock.getMarkers(), p);
        p.out.append(unknownBlock.getPrefix()).append("@").append(unknownBlock.getName());
        visit(unknownBlock.getContent(), p);
        return unknownBlock;
    }

    @Override
    public Javadoc visitUnknownInline(Javadoc.UnknownInline unknownInline, PrintOutputCapture<P> p) {
        visitMarkers(unknownInline.getMarkers(), p);
        p.out.append(unknownInline.getPrefix()).append("{@").append(unknownInline.getName())
                .append(unknownInline.getBeforeEndBrace()).append('}');
        return unknownInline;
    }

    @Override
    public Javadoc visitUses(Javadoc.Uses uses, PrintOutputCapture<P> p) {
        visitMarkers(uses.getMarkers(), p);
        p.out.append(uses.getPrefix()).append("@uses");
        javaVisitor.visit(uses.getServiceType(), p);
        visit(uses.getDescription(), p);
        return uses;
    }

    @Override
    public Javadoc visitVersion(Javadoc.Version since, PrintOutputCapture<P> p) {
        visitMarkers(since.getMarkers(), p);
        p.out.append(since.getPrefix()).append("@version");
        visit(since.getBody(), p);
        return since;
    }

    protected void visit(@Nullable List<? extends Javadoc> nodes, PrintOutputCapture<P> p) {
        if (nodes != null) {
            for (Javadoc node : nodes) {
                visit(node, p);
            }
        }
    }

    class JavadocJavaPrinter extends CVisitor<PrintOutputCapture<P>> {
        @Override
        public C visitMethodInvocation(J.MethodInvocation method, PrintOutputCapture<P> p) {
            visitMarkers(method.getMarkers(), p);
            visitSpace(method.getPrefix(), Space.Location.IDENTIFIER_PREFIX, p);
            visit(method.getSelect(), p);
            p.out.append('#').append(method.getSimpleName()).append('(');
            visitContainer(method.getPadding().getArguments(), CContainer.Location.METHOD_INVOCATION_ARGUMENTS, p);
            p.out.append(')');
            return method;
        }

        @Override
        public C visitIdentifier(J.Identifier ident, PrintOutputCapture<P> p) {
            visitMarkers(ident.getMarkers(), p);
            visitSpace(ident.getPrefix(), Space.Location.IDENTIFIER_PREFIX, p);
            p.out.append(ident.getSimpleName());
            return ident;
        }

        @Override
        public C visitFieldAccess(J.FieldAccess fieldAccess, PrintOutputCapture<P> p) {
            visitSpace(fieldAccess.getPrefix(), Space.Location.FIELD_ACCESS_PREFIX, p);
            visitMarkers(fieldAccess.getMarkers(), p);
            visit(fieldAccess.getTarget(), p);
            visitLeftPadded(".", fieldAccess.getPadding().getName(), CLeftPadded.Location.FIELD_ACCESS_NAME, p);
            return fieldAccess;
        }

        @Override
        public C visitMemberReference(J.MemberReference memberRef, PrintOutputCapture<P> p) {
            visitSpace(memberRef.getPrefix(), Space.Location.MEMBER_REFERENCE_PREFIX, p);
            visitMarkers(memberRef.getMarkers(), p);
            visit(memberRef.getContaining(), p);
            visitLeftPadded("#", memberRef.getPadding().getReference(), CLeftPadded.Location.MEMBER_REFERENCE_NAME, p);
            return memberRef;
        }

        @Override
        public C visitTypeParameter(J.TypeParameter typeParam, PrintOutputCapture<P> p) {
            visitSpace(typeParam.getPrefix(), Space.Location.TYPE_PARAMETERS_PREFIX, p);
            visitMarkers(typeParam.getMarkers(), p);
            p.out.append("<");
            visit(typeParam.getName(), p);
            p.out.append(">");
            return typeParam;
        }

        @Override
        public Space visitSpace(Space space, Space.Location loc, PrintOutputCapture<P> p) {
            p.out.append(space.getWhitespace());
            return space;
        }

        private void visitLeftPadded(@Nullable String prefix, @Nullable CLeftPadded<? extends C> leftPadded, CLeftPadded.Location location, PrintOutputCapture<P> p) {
            if (leftPadded != null) {
                visitSpace(leftPadded.getBefore(), location.getBeforeLocation(), p);
                if (prefix != null) {
                    p.out.append(prefix);
                }
                visit(leftPadded.getElement(), p);
            }
        }
    }
}
