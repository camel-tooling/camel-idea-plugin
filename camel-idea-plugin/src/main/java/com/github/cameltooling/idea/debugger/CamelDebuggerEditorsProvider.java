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
package com.github.cameltooling.idea.debugger;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.LocalTimeCounter;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.EvaluationMode;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class CamelDebuggerEditorsProvider extends XDebuggerEditorsProvider {
    @NotNull
    @Override
    public FileType getFileType() {
        //return XmlFileType.INSTANCE;
        return PlainTextFileType.INSTANCE;
    }

    @NotNull
    @Override
    public Document createDocument(@NotNull Project project, @NotNull XExpression expression, @Nullable XSourcePosition xSourcePosition, @NotNull EvaluationMode evaluationMode) {
        final PsiFile psiFile = PsiFileFactory.getInstance(project)
                .createFileFromText("camelExpr." + getFileType().getDefaultExtension(), getFileType(), expression.getExpression(), LocalTimeCounter.currentTime(), true);
        return PsiDocumentManager.getInstance(project).getDocument(psiFile);
    }
}
