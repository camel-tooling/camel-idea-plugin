package com.github.cameltooling.idea.runner.debugger;


import com.github.cameltooling.idea.runner.debugger.stack.CamelMessageInfo;
import com.github.cameltooling.idea.runner.debugger.stack.ObjectFieldDefinitionValue;
import com.intellij.icons.AllIcons;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CamelExpressionEvaluator extends XDebuggerEvaluator {

    private CamelDebuggerSession session;

    public CamelExpressionEvaluator(@NotNull CamelDebuggerSession session) {
        this.session = session;
    }

    //This method should never be called because we override another `evaluate` method - but it has to be implemented because it's abstract in the parent class
    @Override
    public void evaluate(@NotNull String expression, @NotNull XEvaluationCallback xEvaluationCallback, @Nullable XSourcePosition expressionPosition) {
        Object result = session.evaluateExpression(expression, "simple");
        if (result != null) {
            xEvaluationCallback.evaluated(new ObjectFieldDefinitionValue(session, new CamelMessageInfo.Value(result.getClass().getName(), result), result instanceof Throwable ? AllIcons.General.Error : AllIcons.Nodes.Function));
        } else {
            xEvaluationCallback.evaluated(new ObjectFieldDefinitionValue(session, new CamelMessageInfo.Value("null", "null"), AllIcons.General.Error));
        }
    }

    @Override
    public void evaluate(@NotNull XExpression expression, @NotNull XEvaluationCallback xEvaluationCallback, @Nullable XSourcePosition expressionPosition) {
        Object result = session.evaluateExpression(expression.getExpression(), expression.getLanguage().getID());
        if (result != null) {
            xEvaluationCallback.evaluated(new ObjectFieldDefinitionValue(session, new CamelMessageInfo.Value(result.getClass().getName(), result), result instanceof Throwable ? AllIcons.General.Error : AllIcons.Nodes.Function));
        } else {
            xEvaluationCallback.evaluated(new ObjectFieldDefinitionValue(session, new CamelMessageInfo.Value("null", "null"), AllIcons.General.Error));
        }
    }

}
