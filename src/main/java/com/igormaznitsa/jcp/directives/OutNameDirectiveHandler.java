/* 
 * Copyright 2014 Igor Maznitsa (http://www.igormaznitsa.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igormaznitsa.jcp.directives;

import com.igormaznitsa.jcp.context.JCPSpecialVariableProcessor;
import com.igormaznitsa.jcp.context.PreprocessorContext;
import com.igormaznitsa.jcp.expression.Expression;
import com.igormaznitsa.jcp.expression.Value;
import com.igormaznitsa.jcp.expression.ValueType;

/**
 * The class implements the //#outname directive handler
 *
 * @author Igor Maznitsa (igor.maznitsa@igormaznitsa.com)
 */
public class OutNameDirectiveHandler extends AbstractDirectiveHandler {

  @Override
  public String getName() {
    return "outname";
  }

  @Override
  public String getReference() {
    return "allows to change the result file name for the current file (it can be read through " + JCPSpecialVariableProcessor.VAR_DEST_FILE_NAME + ')';
  }

  @Override
  public DirectiveArgumentType getArgumentType() {
    return DirectiveArgumentType.STRING;
  }

  @Override
  public AfterDirectiveProcessingBehaviour execute(final String string, final PreprocessorContext context) {
    final Value dirName = Expression.evalExpression(string, context);

    if (dirName == null || dirName.getType() != ValueType.STRING) {
      final String text = DIRECTIVE_PREFIX + "outname needs a string expression";
      throw new IllegalArgumentException(text, context.makeException(text, null));
    }
    context.getPreprocessingState().getRootFileInfo().setDestinationName(dirName.asString());
    return AfterDirectiveProcessingBehaviour.PROCESSED;
  }
}
