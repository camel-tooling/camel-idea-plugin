/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cameltooling.idea.runner.debugger.evaluator;

import com.intellij.util.ui.UIUtil;
import com.intellij.xdebugger.XDebuggerBundle;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XValue;
import com.intellij.xdebugger.frame.XValueChildrenList;
import com.intellij.xdebugger.frame.XValueContainer;
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTree;
import com.intellij.xdebugger.impl.ui.tree.nodes.MessageTreeNode;
import com.intellij.xdebugger.impl.ui.tree.nodes.XEvaluationCallbackBase;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueContainerNode;
import org.jetbrains.annotations.NotNull;

public class CamelEvaluatingExpressionRootNode extends XValueContainerNode<CamelEvaluatingExpressionRootNode.EvaluatingResultContainer> {
    public CamelEvaluatingExpressionRootNode(CamelDebuggerEvaluationDialog evaluationDialog, final XDebuggerTree tree) {
        super(tree, null, false, new CamelEvaluatingExpressionRootNode.EvaluatingResultContainer(evaluationDialog));
    }

    @Override
    protected MessageTreeNode createLoadingMessageNode() {
        return MessageTreeNode.createEvaluatingMessage(myTree, this);
    }

    public static class EvaluatingResultContainer extends XValueContainer {
        private final CamelDebuggerEvaluationDialog myDialog;

        public EvaluatingResultContainer(final CamelDebuggerEvaluationDialog dialog) {
            myDialog = dialog;
        }

        @Override
        public void computeChildren(@NotNull final XCompositeNode node) {
            myDialog.startEvaluation(new XEvaluationCallbackBase() {
                @Override
                public void evaluated(@NotNull final XValue result) {
                    String name = UIUtil.removeMnemonic(XDebuggerBundle.message("xdebugger.evaluate.result"));
                    node.addChildren(XValueChildrenList.singleton(name, result), true);
                    myDialog.evaluationDone();
                }

                @Override
                public void errorOccurred(@NotNull final String errorMessage) {
                    node.setErrorMessage(errorMessage);
                    myDialog.evaluationDone();
                }
            });
        }
    }
}
