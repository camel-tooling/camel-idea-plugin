package com.github.cameltooling.idea.runner.debugger;


import com.github.cameltooling.idea.runner.debugger.stack.CamelMessageInfo;
import com.github.cameltooling.idea.runner.debugger.stack.ObjectFieldDefinitionValue;
import com.intellij.icons.AllIcons;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CamelSimpleEvaluator extends XDebuggerEvaluator {

    private CamelDebuggerSession session;

    public CamelSimpleEvaluator(@NotNull CamelDebuggerSession session) {
        this.session = session;
    }

    @Override
    public void evaluate(@NotNull String script, @NotNull final XEvaluationCallback xEvaluationCallback, @Nullable XSourcePosition xSourcePosition) {
        Object result = session.evaluateExpression(script, "simple");
        if (result != null) {
            xEvaluationCallback.evaluated(new ObjectFieldDefinitionValue(session, new CamelMessageInfo.Value(result.getClass().getName(), result), result instanceof Throwable ? AllIcons.General.Error : AllIcons.Nodes.Function));
        } else {
            xEvaluationCallback.evaluated(new ObjectFieldDefinitionValue(session, new CamelMessageInfo.Value("null", "null"), AllIcons.General.Error));
        }
    }
}
