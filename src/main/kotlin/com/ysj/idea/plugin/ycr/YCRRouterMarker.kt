package com.ysj.idea.plugin.ycr

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.pom.Navigatable
import com.intellij.psi.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.plainContent

/**
 * 用于定位 YCR 的 build("/aaa/bbb") 对应的 @Route(path = "/aaa/bbb") 的标记类
 *
 * @author Ysj
 * Create time: 2021/7/2
 */
class YCRRouterMarker : LineMarkerProviderDescriptor() {

    override fun getName() = "YCR"

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<out PsiElement>? {
        searchTarget(element) ?: return null
        return markerInfo(element) { _, elm ->
            // GutterIconNavigationHandler 这玩意会复用？
            searchTarget(elm)?.navigate(true)
        }
    }

    private fun searchTarget(element: PsiElement): Navigatable? {
        val routePath = matchRoutePath(element) ?: return null
        return element.project.findRouteAnnotations()?.find { mb ->
            routePath in mb.annotations.mapNotNull { ann ->
                val path = ann.findAttributeValue("path")
                val group = ann.findAttributeValue("group")
                if (path !is PsiLiteralValue || group !is PsiLiteralValue) return@mapNotNull null
                group.value.toString().run { if (isEmpty()) path.value else "$this/${path.value}" }
            }
        }
    }

    private fun matchRoutePath(element: PsiElement) =
        if (element !is KtElement) element.buildArgs()?.getOrNull(0)?.cast<PsiLiteralValue>()?.value?.toString()
        else (element as KtElement).buildArgs()?.getOrNull(0)
            ?.getArgumentExpression()
            ?.cast<KtStringTemplateExpression>()
            ?.plainContent

    private fun markerInfo(
        element: PsiElement,
        navHandler: GutterIconNavigationHandler<PsiElement>,
    ) = LineMarkerInfo(
        element,
        element.textRange,
        AllIcons.General.Locate,
        null,
        navHandler,
        GutterIconRenderer.Alignment.LEFT,
    )

}