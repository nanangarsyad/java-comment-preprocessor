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
package com.igormaznitsa.jcp.expression;

import java.util.Arrays;
import com.igormaznitsa.jcp.exceptions.FilePositionInfo;
import com.igormaznitsa.jcp.exceptions.PreprocessorException;
import com.igormaznitsa.jcp.expression.functions.AbstractFunction;
import com.igormaznitsa.jcp.expression.operators.AbstractOperator;
import com.igormaznitsa.jcp.expression.operators.OperatorSUB;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.igormaznitsa.meta.annotation.MustNotContainNull;
import static com.igormaznitsa.meta.common.utils.Assertions.assertNotNull;

/**
 * The class describes a wrapper around an expression item to be saved into an expression tree
 *
 * @author Igor Maznitsa (igor.maznitsa@igormaznitsa.com)
 */
public class ExpressionTreeElement {

  /**
   * Inside constant to be used for speed up some operations
   */
  private static final OperatorSUB OPERATOR_SUB = AbstractOperator.findForClass(OperatorSUB.class);

  public static final ExpressionTreeElement EMPTY_SLOT = new ExpressionTreeElement();

  /**
   * Empty array to avoid unnecessary operations
   */
  private static final ExpressionTreeElement[] EMPTY = new ExpressionTreeElement[0];

  /**
   * The variable contains the wrapped expression item
   */
  private ExpressionItem savedItem;

  /**
   * The array contains links to the tree element children
   */
  private ExpressionTreeElement[] childElements;

  /**
   * The link to the parent element, if it is the tree root then it contains null
   */
  private ExpressionTreeElement parentTreeElement;

  /**
   * The priority of the tree element, it is very strongly used during tree sorting
   */
  private int priority;

  /**
   * Because I fill children sequentially, the variable contains the index of the first empty child slot
   */
  private int nextChildSlot = 0;

  /**
   * Contains the source string for the expression.
   */
  private final String sourceString;

  /**
   * Current include stack of the preprocessor to the source string.
   */
  private final FilePositionInfo[] includeStack;

  private ExpressionTreeElement() {
    this.sourceString = "";
    this.includeStack = new FilePositionInfo[0];
  }

  /**
   * Allows to check that the element is EMPTY_SLOT
   * @return true if the element is empty slot, false otherwise
   */
  public boolean isEmptySlot(){
    return EMPTY_SLOT == this;
  }
  
  /**
   * The constructor
   *
   * @param item an expression item to be wrapped
   * @param callStack current call stack
   * @param sourceString source string for the expression
   */
  ExpressionTreeElement(@Nonnull final ExpressionItem item, @Nonnull @MustNotContainNull final FilePositionInfo[] callStack, @Nullable final String sourceString) {
    this.sourceString = sourceString;
    this.includeStack = callStack;

    if (item == null) {
      throw new PreprocessorException("[Expression]The item is null", this.sourceString, this.includeStack, null);
    }

    int arity = 0;
    if (item.getExpressionItemType() == ExpressionItemType.OPERATOR) {
      arity = ((AbstractOperator) item).getArity();
    } else if (item.getExpressionItemType() == ExpressionItemType.FUNCTION) {
      arity = ((AbstractFunction) item).getArity();
    }
    priority = item.getExpressionItemPriority().getPriority();
    this.savedItem = item;
    childElements = arity == 0 ? EMPTY : new ExpressionTreeElement[arity];
    Arrays.fill(this.childElements, EMPTY_SLOT);
  }

  private void assertNotEmptySlot(){
    if (isEmptySlot()) {
      throw new UnsupportedOperationException("Unsupported operation for empty slot");
    }
  }
  
  /**
   * Inside auxiliary function to set the maximum priority the the element
   */
  void makeMaxPriority() {
    priority = ExpressionItemPriority.VALUE.getPriority();
  }

  /**
   * Get the wrapped item
   *
   * @return the item to be wrapped by the object
   */
  @Nonnull
  public ExpressionItem getItem() {
    return this.savedItem;
  }

  /**
   * Get arity for the element (I mean possible children number)
   *
   * @return the arity, zero for elements without children
   */
  public int getArity() {
    return childElements.length;
  }

  /**
   * Get the parent for the element
   *
   * @return the parent for the element or null if the element is the tree root
   */
  @Nullable
  public ExpressionTreeElement getParent() {
    return parentTreeElement;
  }

  /**
   * Get the current priority of the element
   *
   * @return the priority
   */
  public int getPriority() {
    return priority;
  }

  /**
   * Add a tree as new child and make the maximum priority for it
   *
   * @param tree a tree to be added as a child, must not be null
   * @return it returns this
   */
  @Nonnull
  public ExpressionTreeElement addSubTree(@Nonnull final ExpressionTree tree) {
    assertNotEmptySlot();
    
    final ExpressionTreeElement root = tree.getRoot();
    if (!root.isEmptySlot()) {
      root.makeMaxPriority();
      addElementToNextFreeSlot(root);
    }
    return this;
  }

  /**
   * It replaces a child element
   *
   * @param oldOne the old expression element to be replaced (must not be null)
   * @param newOne the new expression element to be used instead the old one (must not be null)
   * @return true if the element was found and replaced, else false
   */
  public boolean replaceElement(@Nonnull final ExpressionTreeElement oldOne, @Nonnull final ExpressionTreeElement newOne) {
    assertNotEmptySlot();
    
    if (oldOne == null) {
      throw new PreprocessorException("[Expression]The old element is null", this.sourceString, this.includeStack, null);
    }

    if (newOne == null) {
      throw new PreprocessorException("[Expression]The new element is null", this.sourceString, this.includeStack, null);
    }

    boolean result = false;

    final ExpressionTreeElement[] children = childElements;
    final int len = children.length;

    for (int i = 0; i < len; i++) {
      if (children[i] == oldOne) {
        children[i] = newOne;
        newOne.parentTreeElement = this;
        result = true;
        break;
      }
    }
    return result;
  }

  /**
   * Get the child element for its index (the first is 0)
   *
   * @param index the index of the needed child
   * @return the child or EMPTY_SLOT
   * @throws ArrayIndexOutOfBoundsException it will be thrown if an impossible index is being used
   * @see #EMPTY_SLOT
   */
  @Nonnull
  public ExpressionTreeElement getChildForIndex(final int index) {
    assertNotEmptySlot();
    return this.childElements[index];
  }

  /**
   * Add tree element with sorting operation depends on priority of the elements
   *
   * @param element the element to be added, must not be null
   * @return the element which should be used as the last for the current tree
   */
  @Nullable
  public ExpressionTreeElement addTreeElement(@Nonnull final ExpressionTreeElement element) {
    assertNotEmptySlot();
    assertNotNull("The element is null", element);

    final int newElementPriority = element.getPriority();

    ExpressionTreeElement result = this;

    final ExpressionTreeElement parentTreeElement = this.parentTreeElement;

    final int currentPriority = getPriority();

    if (newElementPriority < currentPriority) {
      if (parentTreeElement == null) {
        element.addTreeElement(this);
        result = element;
      } else {
        result = parentTreeElement.addTreeElement(element);
      }
    } else if (newElementPriority == currentPriority) {
      if (parentTreeElement != null) {
        parentTreeElement.replaceElement(this, element);
      }
      if (element.nextChildSlot >= element.childElements.length) {
        throw new PreprocessorException("[Expression]Can't process expression item, may be wrong number of arguments", this.sourceString, this.includeStack, null);
      }
      element.childElements[element.nextChildSlot] = this;
      element.nextChildSlot++;
      this.parentTreeElement = element;
      result = element;
    } else if (isFull()) {
      final int lastElementIndex = getArity() - 1;

      final ExpressionTreeElement lastElement = childElements[lastElementIndex];
      if (lastElement.getPriority() > newElementPriority) {
        element.addElementToNextFreeSlot(lastElement);
        childElements[lastElementIndex] = element;
        element.parentTreeElement = this;
        result = element;
      }

    } else {
      addElementToNextFreeSlot(element);
      result = element;
    }
    return result;
  }

  /**
   * It allows to check that all children slots have been filled
   *
   * @return true if there is not any free child slot else false
   */
  public boolean isFull() {
    return nextChildSlot >= childElements.length;
  }

  /**
   * It fills children slots from a list containing expression trees
   *
   * @param arguments the list containing trees to be used as children
   */
  public void fillArguments(@Nonnull @MustNotContainNull final List<ExpressionTree> arguments) {
    assertNotEmptySlot();
    
    if (arguments == null) {
      throw new PreprocessorException("[Expression]Argument list is null", this.sourceString, this.includeStack, null);
    }

    if (childElements.length != arguments.size()) {
      throw new PreprocessorException("Wrong argument list size", this.sourceString, this.includeStack, null);
    }

    int i = 0;
    for (ExpressionTree arg : arguments) {
      if (arg == null) {
        throw new PreprocessorException("[Expression]Argument [" + (i + 1) + "] is null", this.sourceString, this.includeStack, null);
      }

      if (!childElements[i].isEmptySlot()) {
        throw new PreprocessorException("[Expression]Non-empty slot detected, it is possible that there is a program error, contact a developer please", this.sourceString, this.includeStack, null);
      }

      final ExpressionTreeElement root = arg.getRoot();
      if (root.isEmptySlot()) {
        throw new PreprocessorException("[Expression]Empty argument [" + (i + 1) + "] detected", this.sourceString, this.includeStack, null);
      }
      childElements[i] = root;
      root.parentTreeElement = this;

      i++;
    }
  }

  /**
   * Add an expression element into the next free child slot
   *
   * @param element an element to be added, must not be null
   */
  private void addElementToNextFreeSlot(@Nonnull final ExpressionTreeElement element) {
    if (element == null) {
      throw new PreprocessorException("[Expression]Element is null", this.sourceString, this.includeStack, null);
    }

    if (childElements.length == 0) {
      throw new PreprocessorException("[Expression]Unexpected element, may be unknown function [" + savedItem.toString() + ']', this.sourceString, this.includeStack, null);
    } else if (isFull()) {
      throw new PreprocessorException("[Expression]There is not any possibility to add new argument [" + savedItem.toString() + ']', this.sourceString, this.includeStack, null);
    } else {
      childElements[nextChildSlot++] = element;
    }
    element.parentTreeElement = this;
  }

  /**
   * Post-processing after the tree is formed, the unary minus operation will be optimized
   */
  public void postProcess() {
    if (!this.isEmptySlot()) {

      switch (savedItem.getExpressionItemType()) {
        case OPERATOR: {
          if (savedItem == OPERATOR_SUB) {
            if (!childElements[0].isEmptySlot() && childElements[1].isEmptySlot()) {
              final ExpressionTreeElement left = childElements[0];
              final ExpressionItem item = left.getItem();
              if (item.getExpressionItemType() == ExpressionItemType.VALUE) {
                final Value val = (Value) item;
                switch (val.getType()) {
                  case INT: {
                    childElements = EMPTY;
                    savedItem = Value.valueOf(0 - val.asLong());
                    makeMaxPriority();
                  }
                  break;
                  case FLOAT: {
                    childElements = EMPTY;
                    savedItem = Value.valueOf(0.0f - val.asFloat());
                    makeMaxPriority();
                  }
                  break;
                  default: {
                    if (!left.isEmptySlot()) left.postProcess();
                  }
                  break;
                }
              }
            } else {
              for (final ExpressionTreeElement element : childElements) {
                if (!element.isEmptySlot()) {
                  element.postProcess();
                }
              }
            }
          } else {
            for (final ExpressionTreeElement element : childElements) {
              if (!element.isEmptySlot()) {
                element.postProcess();
              }
            }
          }
        }
        break;
        case FUNCTION: {
          for (final ExpressionTreeElement element : childElements) {
            if (!element.isEmptySlot()) {
              element.postProcess();
            }
          }
        }
        break;
      }
    }
  }
}
