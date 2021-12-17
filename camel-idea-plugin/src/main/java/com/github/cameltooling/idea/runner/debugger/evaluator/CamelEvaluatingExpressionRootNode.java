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
