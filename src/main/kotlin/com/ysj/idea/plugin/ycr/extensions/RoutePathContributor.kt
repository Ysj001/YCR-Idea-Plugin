package com.ysj.idea.plugin.ycr.extensions

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.Language
import com.intellij.psi.*
import com.intellij.psi.util.parents
import com.ysj.idea.plugin.ycr.*
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.plainContent

/**
 * build 方法 path 内容提示和自动补全功能
 *
 * - Windows: Ctrl+Shift+Space
 * - MacOS: ⌃⇧Space
 *
 * [CompletionContributor 文档](https://plugins.jetbrains.com/docs/intellij/completion-contributor.html#register-the-completion-contributor)
 *
 * @author Ysj
 * Create time: 2021/7/9
 */
class RoutePathContributor : CompletionContributor() {

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val position = parameters.originalPosition ?: return
        result.addAllElements(
            when (position.language) {
                Language.findLanguageByID("JAVA") -> {
                    val element = position.parents.find { it is PsiMethodCallExpression }
                    val args = element?.buildArgs()
                    if (args.isNullOrEmpty()) return
                    val inputValue = args[0].cast<PsiLiteralValue>()?.value?.toString()
                    element.results(inputValue) { context, item ->
                        if (inputValue.isNullOrEmpty()) return@results
                        element.buildArgs()?.get(0)?.run {
                            val startOffset = textRange.startOffset
                            context.document.replaceString(startOffset, textRange.endOffset, "\"${item.lookupString}\"")
                        }
                    } ?: return
                }
                Language.findLanguageByID("kotlin") -> {
                    val element = position.parents.find { it is KtCallExpression } ?: return
                    val args = element.cast<KtElement>()?.buildArgs()
                    if (args.isNullOrEmpty()) return
                    val inputValue = args[0].getArgumentExpression().cast<KtStringTemplateExpression>()?.plainContent
                    element.results(inputValue) { context, item ->
                        if (inputValue.isNullOrEmpty()) return@results
                        element.cast<KtElement>()?.buildArgs()?.get(0)?.cast<PsiElement>()?.run {
                            val startOffset = textRange.startOffset
                            context.document.replaceString(startOffset, textRange.endOffset, "\"${item.lookupString}\"")
                        }
                    } ?: return
                }
                else -> return
            }
        )
    }

    private fun PsiElement.results(
        inputValue: String?,
        insertHandler: InsertHandler<LookupElement>,
    ) = project.findRouteAnnotations()?.mapNotNull {
        val annotation = it.getAnnotation(ROUTE_ANNOTATION_NAME) ?: return@mapNotNull null
        val targetPath = it.routePath() ?: return@mapNotNull null
        LookupElementBuilder.create(if (inputValue == null) "\"$targetPath\"" else targetPath)
            .withTailText(" " + (annotation.parent.parent.cast<PsiClass>())?.qualifiedName)
            .withTypeText("YCR")
            .withInsertHandler(insertHandler)
    }

}